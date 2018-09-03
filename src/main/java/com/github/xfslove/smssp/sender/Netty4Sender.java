package com.github.xfslove.smssp.sender;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.MessageProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

import static com.github.xfslove.smssp.netty4.handler.AttributeKeyConstants.RESP_QUEUE;


/**
 * @author hanwen
 * created at 2018/9/1
 */
public class Netty4Sender implements Sender {

  private static final Logger LOGGER = LoggerFactory.getLogger(Netty4Sender.class);

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
  public void connect(MessageProtocol protocol, String host, int port) {
    bootstrap.remoteAddress(host, port);

    ChannelPoolHandler handler;

    switch (protocol) {
      case CMPP_20:
        handler = new com.github.xfslove.smssp.netty4.handler.cmpp20.sender.SenderPoolMessageHandler(loginName, loginPassword);
        break;
      case SGIP_12:
        handler = new com.github.xfslove.smssp.netty4.handler.sgip12.sender.SenderPoolMessageHandler(loginName, loginPassword);
        break;
      default:
        throw new IllegalArgumentException("unsupported");
    }

    channelPool = new FixedChannelPool(bootstrap, handler, 2);
  }

  @Override
  public Message send(Message message) {

    Channel channel = null;
    try {
      Future<Channel> future = channelPool.acquire().sync();
      channel = future.getNow();
      channel.writeAndFlush(message);

      BlockingQueue<Message> queue = channel.attr(RESP_QUEUE).get();
      return queue.take();
    } catch (InterruptedException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
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
