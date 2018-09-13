package com.github.xfslove.smssp.server.sgip;

import com.github.xfslove.smssp.message.sequence.DefaultSgipSequence;
import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.netty4.handler.sgip12.server.HandlerInitializer;
import com.github.xfslove.smssp.server.DefaultProxyListener;
import com.github.xfslove.smssp.server.Notification;
import com.github.xfslove.smssp.server.NotificationListener;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
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
public class SgipServer {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(SgipServer.class);

  private EventLoopGroup bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("sgipServerBoss", true));

  private EventLoopGroup workerGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("sgipServerWorker", true));

  private ServerBootstrap bootstrap = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(NioServerSocketChannel.class)
      .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

  private EventExecutorGroup bizGroup = new DefaultEventExecutorGroup(32);

  private String loginName;
  private String loginPassword;
  private int port;
  private int idleCheckTime = 30;

  private Sequence sequence;
  private NotificationListener consumer = new NotificationListener() {
    @Override
    public void done(Notification notification) {
      LOGGER.info("received notification: {}", notification);
    }
  };

  private SgipServer(int port) {
    this.sequence = new DefaultSgipSequence(port);
  }

  public static SgipServer newBind(int port) {
    return new SgipServer(port);
  }

  public SgipServer loginName(String loginName) {
    this.loginName = loginName;
    return this;
  }

  public SgipServer loginPassword(String loginPassword) {
    this.loginPassword = loginPassword;
    return this;
  }

  public SgipServer port(int port) {
    this.port = port;
    return this;
  }

  public SgipServer idleCheckTime(int idleCheckTime) {
    this.idleCheckTime = idleCheckTime;
    return this;
  }

  public SgipServer notificationListener(NotificationListener consumer) {
    this.consumer = consumer;
    return this;
  }

  public SgipServer sequence(Sequence sequence) {
    this.sequence = sequence;
    return this;
  }

  public SgipServer bind() {

    HandlerInitializer handler = new HandlerInitializer(loginName, loginPassword, new DefaultProxyListener(consumer), sequence, bizGroup, idleCheckTime);

    bootstrap.childHandler(handler).bind(port);

    LOGGER.info("bind server success, listen port[{}]", port);

    return this;
  }

  public void close() {

    bossGroup.shutdownGracefully().syncUninterruptibly();
    workerGroup.shutdownGracefully().syncUninterruptibly();
    bizGroup.shutdownGracefully().syncUninterruptibly();

    LOGGER.info("shutdown server gracefully success");
  }
}
