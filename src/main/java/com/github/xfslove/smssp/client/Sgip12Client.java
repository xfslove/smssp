package com.github.xfslove.smssp.client;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smssp.exchange.DefaultFuture;
import com.github.xfslove.smssp.exchange.ResponseListener;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.sgip12.DefaultSequence;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import com.github.xfslove.smssp.message.sgip12.SubmitMessage;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import com.github.xfslove.smssp.transport.netty4.handler.sgip12.client.HandlerInitializer;
import com.github.xfslove.smssp.transport.netty4.handler.sgip12.client.PoolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/13
 */
public class Sgip12Client {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(Sgip12Client.class);

  private EventLoopGroup workGroup;
  private Bootstrap bootstrap;
  private ChannelPool channelPool;

  private String username;
  private String password;
  private String host;
  private int port;

  private int idleCheckTime = 30;
  private int connections = 1;

  private Sequence<SequenceNumber> sequence;
  private ResponseListener consumer;

  private Sgip12Client(int nodeId, String username, String password, String host, int port) {
    this.username = username;
    this.password = password;
    this.host = host;
    this.port = port;
    this.sequence = new DefaultSequence(nodeId);
    this.consumer = new DefaultFuture.DefaultListener(username);

    this.workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("sgipWorker-" + username, true));
    this.bootstrap = new Bootstrap().group(workGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .option(ChannelOption.TCP_NODELAY, true)
        .option(ChannelOption.SO_REUSEADDR, true)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);
  }

  public static Sgip12Client newConnection(int nodeId, String username, String password, String host, int port) {
    return new Sgip12Client(nodeId, username, password, host, port);
  }

  public Sgip12Client idleCheckTime(int idleCheckTime) {
    this.idleCheckTime = idleCheckTime;
    return this;
  }

  public Sgip12Client connections(int connections) {
    this.connections = connections;
    return this;
  }

  public Sgip12Client sequence(Sequence<SequenceNumber> sequence) {
    this.sequence = sequence;
    return this;
  }

  public Sgip12Client responseListener(ResponseListener consumer) {
    this.consumer = consumer;
    return this;
  }

  public Sgip12Client connect() {

    HandlerInitializer handler = new HandlerInitializer(username, password, consumer, sequence, idleCheckTime);

    bootstrap.remoteAddress(host, port);

    channelPool = new FixedChannelPool(bootstrap, new PoolHandler(handler), connections);

    LOGGER.info("{} init connection pool to [{}:{}] success", username, host, port);
    return this;
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

  public SubmitRespMessage submit(SubmitMessage submit, int timeout) {

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

    Message.Sgip12 sgip12 = (Message.Sgip12) message;

    SmsPdu[] pdus = sgip12.getPdu().convert();
    SubmitMessage[] req = new SubmitMessage[pdus.length];
    for (int i = 0; i < pdus.length; i++) {
      final SubmitMessage submit = new SubmitMessage(sequence);

      for (String phone : sgip12.getPhones()) {
        submit.getUserNumbers().add(phone);
      }

      submit.setSpNumber(sgip12.getSpNumber());
      submit.setCorpId(sgip12.getCorpId());
      submit.setServiceType(sgip12.getServiceType());
      submit.setMorelatetoMTFlag(sgip12.getMorelatetoMTFlag());

      submit.setUserDataHeaders(pdus[i].getUserDateHeaders());
      submit.setUserData(pdus[i].getUserData());
      req[i] = submit;
    }

    return req;

  }

  public void close() {

    if (channelPool != null) {
      channelPool.close();
    }

    workGroup.shutdownGracefully().syncUninterruptibly();

    if (consumer instanceof DefaultFuture.DefaultListener) {
      DefaultFuture.cleanUp(username);
    }

    LOGGER.info("{} shutdown gracefully, disconnect to [{}:{}] success", username, host, port);
  }

}
