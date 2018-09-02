package com.github.xfslove.smssp.sender.cmpp20.netty4;

import com.github.xfslove.smssp.handler.cmpp20.sender.SenderPoolMessageHandler;
import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import com.github.xfslove.smssp.sender.Sender;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.xfslove.smssp.handler.AttributeKeyConstants.RESP_QUEUE;


/**
 * @author hanwen
 * created at 2018/9/1
 */
public class Netty4Sender implements Sender<SubmitMessage, SubmitRespMessage> {

  private EventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("sgipSendWorker", true));

  private Bootstrap bootstrap = new Bootstrap().group(workGroup)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.SO_KEEPALIVE, true)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

  private ChannelPool channelPool;

  private String loginName;

  private String loginPassword;

  public Netty4Sender(String loginName, String loginPassword) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
  }

  @Override
  public void connect(String host, int port) {
    bootstrap.remoteAddress(host, port);

    channelPool = new FixedChannelPool(
        bootstrap, new SenderPoolMessageHandler(loginName, loginPassword), 2);
  }

  @Override
  public SubmitRespMessage send(SubmitMessage message) {

    Channel channel = null;
    try {
      Future<Channel> future = channelPool.acquire().sync();
      channel = future.getNow();
      channel.writeAndFlush(message);

      BlockingQueue<SubmitRespMessage> queue = channel.attr(RESP_QUEUE).get();
      return queue.poll(3000, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return null;
    } finally {

      channelPool.release(channel);
    }
  }

  @Override
  public void close() {

    channelPool.close();
  }
}
