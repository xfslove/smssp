package com.github.xfslove.smssp.subscriber;

import com.github.xfslove.smssp.message.MessageProtocol;
import com.github.xfslove.smssp.message.cmpp20.CmppMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.function.Consumer;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class Netty4Subscriber implements Subscriber<CmppMessage> {

  private EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("SubscribeBoss", true));
  private EventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("SubscribeWorker", true));

  private ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workGroup)
      .channel(NioServerSocketChannel.class)
      .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childOption(ChannelOption.SO_RCVBUF, 2048)
      .childOption(ChannelOption.SO_SNDBUF, 2048);

  private String loginName;

  private String loginPassword;

  /**
   * 默认什么都不做
   */
  private Consumer consumer = m -> {
  };

  public Netty4Subscriber(String loginName, String loginPassword) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
  }

  @Override
  public void bind(MessageProtocol protocol, String host, int port) {
    ChannelInitializer initializer;

    switch (protocol) {
      case CMPP_20:
        initializer = new com.github.xfslove.smssp.netty4.handler.cmpp20.subscriber.SubscriberHandlerInitializer(consumer, loginName, loginPassword);
        break;
      case SGIP_12:
        initializer = new com.github.xfslove.smssp.netty4.handler.sgip12.subscriber.SubscriberHandlerInitializer(consumer, loginName, loginPassword);
        break;
      default:
        throw new IllegalArgumentException("unsupported");
    }


    bootstrap.childHandler(initializer);
    bootstrap.bind(host, port).addListener(listener -> {

    }).syncUninterruptibly();
  }

  @Override
  public void close() {
    bossGroup.shutdownGracefully().addListener(listener -> {

    });
    workGroup.shutdownGracefully().addListener(listener -> {

    });
  }

  @Override
  public void handleMessage(Consumer consumer) {
    this.consumer = consumer;
  }
}
