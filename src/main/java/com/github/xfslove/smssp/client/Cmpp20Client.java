package com.github.xfslove.smssp.client;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smssp.exchange.DefaultFuture;
import com.github.xfslove.smssp.exchange.ResponseListener;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.cmpp20.DefaultSequence;
import com.github.xfslove.smssp.message.cmpp20.MsgId;
import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import com.github.xfslove.smssp.notification.DefaultProxyListener;
import com.github.xfslove.smssp.notification.Notification;
import com.github.xfslove.smssp.notification.NotificationListener;
import com.github.xfslove.smssp.transport.netty4.handler.cmpp20.mix.HandlerInitializer;
import com.github.xfslove.smssp.transport.netty4.handler.cmpp20.mix.PoolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public class Cmpp20Client {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(Cmpp20Client.class);

  private EventLoopGroup workGroup;
  private Bootstrap bootstrap;
  private ChannelPool channelPool;

  private String username;
  private String password;
  private String host;
  private int port;

  private int[] localPorts;
  private int connections = 1;
  private int idleCheckTime = 300;

  private int nodeId;
  private Sequence<Integer> sequence;
  private ResponseListener consumer;
  private NotificationListener consumer2;

  private Cmpp20Client(int nodeId, final String username, String password, String host, int port) {
    this.nodeId = nodeId;
    this.username = username;
    this.password = password;
    this.host = host;
    this.port = port;
    this.sequence = new DefaultSequence();
    this.consumer = new DefaultFuture.DefaultListener(username);
    this.consumer2 = new DefaultProxyListener(username, new NotificationListener() {
      @Override
      public void done(Notification notification) {
        LOGGER.info("{} received notification: {}", username, notification);
      }
    });

    this.workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("cmppWorker-" + username, true));
    this.bootstrap = new Bootstrap().group(workGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
  }

  public static Cmpp20Client newConnection(int nodeId, String username, String password, String host, int port) {
    return new Cmpp20Client(nodeId, username, password, host, port);
  }


  public Cmpp20Client localPorts(int... localPorts) {
    this.localPorts = localPorts;
    return this;
  }

  public Cmpp20Client connections(int connections) {
    this.connections = connections;
    return this;
  }

  public Cmpp20Client sequence(Sequence<Integer> sequence) {
    this.sequence = sequence;
    return this;
  }

  public Cmpp20Client idleCheckTime(int idleCheckTime) {
    this.idleCheckTime = idleCheckTime;
    return this;
  }

  public Cmpp20Client responseListener(ResponseListener consumer) {
    this.consumer = consumer;
    return this;
  }

  public Cmpp20Client notificationListener(NotificationListener consumer2) {
    this.consumer2 = consumer2;
    return this;
  }

  /**
   * 建立链接（长链接），会初始化链接，并保持
   *
   * @return this
   */
  public Cmpp20Client connect() {

    HandlerInitializer mix = new HandlerInitializer(username, password, consumer2, consumer, sequence, idleCheckTime);

    bootstrap.remoteAddress(host, port);

    if (localPorts == null) {
      channelPool = new FixedChannelPool(bootstrap, new PoolHandler(mix), connections);
    } else {
      connections = localPorts.length;
      channelPool = new CmppChannelPool(bootstrap, new PoolHandler(mix));
    }

    LOGGER.info("{} init connection pool to [{}:{}] success", username, host, port);
    return this;
  }

  public void close() {

    if (channelPool != null) {
      channelPool.close();
    }

    workGroup.shutdownGracefully().syncUninterruptibly();

    if (consumer instanceof DefaultFuture.DefaultListener) {
      DefaultFuture.cleanUp(username);
    }
    if (consumer2 instanceof DefaultProxyListener) {
      DefaultProxyListener.cleanUp(username);
    }

    LOGGER.info("{} shutdown gracefully, disconnect to [{}:{}] success", username, host, port);
  }

  public void submit(final SubmitMessage submit) {

    channelPool.acquire().addListener(new GenericFutureListener<Future<Channel>>() {
      @Override
      public void operationComplete(Future<Channel> future) throws Exception {
        if (future.isSuccess()) {
          final Channel channel = future.get();
          channel.writeAndFlush(submit).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
              channelPool.release(channel);
            }
          });
        } else {
          LOGGER.warn("{} acquired channel failure, exception message: {}", username, future.cause().getMessage());
        }
      }
    });
  }

  public SubmitRespMessage submit(final SubmitMessage submit, int timeout) {

    DefaultFuture future = new DefaultFuture(username, submit);

    final Channel channel;
    DefaultPromise<Channel> promise = (DefaultPromise<Channel>) channelPool.acquire();

    long start = System.currentTimeMillis();
    try {
      channel = promise.get(timeout, TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      LOGGER.warn("{} acquired channel failure, exception message: {}", username, e.getMessage());
      return null;
    }
    long spend = System.currentTimeMillis() - start;

    channel.writeAndFlush(submit).addListener(new GenericFutureListener<Future<? super Void>>() {
      @Override
      public void operationComplete(Future<? super Void> future) throws Exception {
        channelPool.release(channel);
      }
    });

    try {
      return (SubmitRespMessage) future.getResponse((int) (timeout - spend));

    } catch (InterruptedException e) {
      LOGGER.warn("{} get response failure, exception message: {}", username, e.getMessage());
      return null;
    }
  }

  public SubmitMessage[] convert(Message message) {

    Message.Cmpp20 cmpp20 = (Message.Cmpp20) message;

    SmsPdu[] pdus = cmpp20.getPdu().convert();
    SubmitMessage[] req = new SubmitMessage[pdus.length];
    Calendar calendar = Calendar.getInstance();
    MsgId msgId = new MsgId(nodeId, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), sequence.next());
    for (int i = 0; i < pdus.length; i++) {
      final SubmitMessage submit = new SubmitMessage(sequence);

      submit.setMsgId(msgId);

      for (String phone : cmpp20.getPhones()) {
        submit.getDestTerminalIds().add(phone);
      }
      submit.setSrcId(cmpp20.getSrcId());
      submit.setServiceId(cmpp20.getServiceId());
      submit.setPkTotal(pdus.length);
      submit.setPkNumber(i + 1);
      submit.setMsgSrc(cmpp20.getMsgSrc());

      submit.setUserDataHeaders(pdus[i].getUserDateHeaders());
      submit.setUserData(pdus[i].getUserData());
      req[i] = submit;
    }

    return req;
  }

  private class CmppChannelPool extends FixedChannelPool {

    CmppChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler) {
      super(bootstrap, handler, new CmppHealthChecker(), null, -1, connections, Integer.MAX_VALUE);
    }

    @Override
    protected ChannelFuture connectChannel(Bootstrap bs) {
      CmppHealthChecker checker = (CmppHealthChecker) healthChecker();
      return bs.connect(SocketUtils.socketAddress(Cmpp20Client.this.host, Cmpp20Client.this.port), checker.availableAddress());
    }
  }

  private class CmppHealthChecker implements ChannelHealthChecker {

    private Deque<SocketAddress> availableAddress = PlatformDependent.newConcurrentDeque();

    CmppHealthChecker() {
      for (int localPort : Cmpp20Client.this.localPorts) {
        availableAddress.offer(new InetSocketAddress(localPort));
      }
    }

    @Override
    public Future<Boolean> isHealthy(final Channel channel) {
      EventLoop loop = channel.eventLoop();
      Future<Boolean> health = loop.newSucceededFuture(Boolean.TRUE);
      if (!channel.isActive()) {
        health = loop.newSucceededFuture(Boolean.FALSE);
        availableAddress.offer(channel.localAddress());
      }
      return health;
    }

    SocketAddress availableAddress() {
      return availableAddress.poll();
    }
  }


}
