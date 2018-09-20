package com.github.xfslove.smssp.server;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.sgip12.DefaultSequence;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import com.github.xfslove.smssp.notification.DefaultProxyListener;
import com.github.xfslove.smssp.notification.Notification;
import com.github.xfslove.smssp.notification.NotificationListener;
import com.github.xfslove.smssp.transport.netty4.handler.sgip12.server.HandlerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author hanwen
 * created at 2018/9/13
 */
public class Sgip12Server {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(Sgip12Server.class);

  private EventLoopGroup bossGroup;
  private EventLoopGroup workGroup;
  private EventExecutorGroup bizGroup;
  private ServerBootstrap bootstrap;
  private Channel channel;

  private String loginName;
  private String loginPassword;
  private int port;

  private int idleCheckTime = 30;

  private int nodeId;
  private Sequence<SequenceNumber> sequence;
  private NotificationListener consumer;

  private Sgip12Server(int nodeId, final String loginName, String loginPassword, int port) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.port = port;
    this.consumer = new DefaultProxyListener(loginName, new NotificationListener() {
      @Override
      public void done(Notification notification) {
        LOGGER.info("{} received notification: {}", loginName, notification);
      }
    });
    this.sequence = new DefaultSequence(nodeId);

    this.bossGroup =  new NioEventLoopGroup(1, new DefaultThreadFactory("sgipServerBoss-"+loginName, true));
    this.workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("sgipServerWorker-" + loginName, true));
    this.bizGroup = new DefaultEventExecutorGroup(32, new DefaultThreadFactory("sgipServerBiz-" + loginName, true));
    this.bootstrap  = new ServerBootstrap()
        .group(bossGroup, workGroup)
        .channel(NioServerSocketChannel.class)
        .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
  }

  public static Sgip12Server newBind(int nodeId, String loginName, String loginPassword, int port) {
    return new Sgip12Server(nodeId, loginName, loginPassword, port);
  }

  public Sgip12Server idleCheckTime(int idleCheckTime) {
    this.idleCheckTime = idleCheckTime;
    return this;
  }

  public Sgip12Server notificationListener(NotificationListener consumer) {
    this.consumer = consumer;
    return this;
  }

  public Sgip12Server sequence(Sequence<SequenceNumber> sequence) {
    this.sequence = sequence;
    return this;
  }

  public Sgip12Server bind() {

    HandlerInitializer handler = new HandlerInitializer(loginName, loginPassword, consumer, sequence, bizGroup, idleCheckTime);

    ChannelFuture channelFuture = bootstrap.childHandler(handler).bind(port);
    channelFuture.syncUninterruptibly();

    channel = channelFuture.channel();

    LOGGER.info("{} bind server success, listen port[{}]", loginName, port);

    return this;
  }

  public void close() {
    if (channel != null) {
      channel.close();
    }

    bossGroup.shutdownGracefully().syncUninterruptibly();
    workGroup.shutdownGracefully().syncUninterruptibly();
    bizGroup.shutdownGracefully().syncUninterruptibly();

    if (consumer instanceof DefaultProxyListener) {
      DefaultProxyListener.cleanUp(loginName);
    }

    LOGGER.info("shutdown server gracefully success");
  }
}
