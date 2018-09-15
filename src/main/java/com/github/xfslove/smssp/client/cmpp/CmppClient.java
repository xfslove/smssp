package com.github.xfslove.smssp.client.cmpp;

import com.github.xfslove.smsj.sms.SmsMessage;
import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.SmsTextMessage;
import com.github.xfslove.smsj.sms.dcs.DcsGroup;
import com.github.xfslove.smsj.sms.dcs.SmsAlphabet;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.dcs.SmsMsgClass;
import com.github.xfslove.smsj.wap.mms.SmsMmsNotificationMessage;
import com.github.xfslove.smssp.client.DefaultFuture;
import com.github.xfslove.smssp.client.ResponseListener;
import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import com.github.xfslove.smssp.message.sequence.DefaultCmppSequence;
import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.netty4.handler.cmpp20.mix.HandlerInitializer;
import com.github.xfslove.smssp.netty4.handler.cmpp20.mix.PoolHandler;
import com.github.xfslove.smssp.server.DefaultProxyListener;
import com.github.xfslove.smssp.server.Notification;
import com.github.xfslove.smssp.server.NotificationListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.*;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public class CmppClient {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(CmppClient.class);

  private EventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("cmppWorker", true));

  private Bootstrap bootstrap = new Bootstrap().group(workGroup)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.SO_KEEPALIVE, true)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000);

  private EventExecutorGroup bizGroup = new DefaultEventExecutorGroup(32);

  private ChannelPool channelPool;

  private String loginName;
  private String loginPassword;
  private String host;
  private int port;
  private int[] localPorts;
  private int connections = 1;
  private int idleCheckTime = 300;

  private Sequence sequence = new DefaultCmppSequence();
  private ResponseListener consumer = new DefaultFuture.DefaultListener();
  private NotificationListener consumer2 = new NotificationListener() {
    @Override
    public void done(Notification notification) {
      LOGGER.info("received notification: {}", notification);
    }
  };

  private CmppClient() {
  }

  public static CmppClient newConnection() {
    return new CmppClient();
  }

  public CmppClient loginName(String loginName) {
    this.loginName = loginName;
    return this;
  }

  public CmppClient loginPassword(String loginPassword) {
    this.loginPassword = loginPassword;
    return this;
  }

  public CmppClient host(String host) {
    this.host = host;
    return this;
  }

  public CmppClient port(int port) {
    this.port = port;
    return this;
  }

  public CmppClient localPorts(int... localPorts) {
    this.localPorts = localPorts;
    return this;
  }

  public CmppClient connections(int connections) {
    this.connections = connections;
    return this;
  }

  public CmppClient sequence(Sequence sequence) {
    this.sequence = sequence;
    return this;
  }

  public CmppClient responseListener(ResponseListener consumer) {
    this.consumer = consumer;
    return this;
  }

  public CmppClient notificationListener(NotificationListener consumer2) {
    this.consumer2 = consumer2;
    return this;
  }

  public CmppClient idleCheckTime(int idleCheckTime) {
    this.idleCheckTime = idleCheckTime;
    return this;
  }

  /**
   * 建立链接（长链接），会初始化链接，并保持
   *
   * @return this
   */
  public CmppClient connect() {

    HandlerInitializer mix = new HandlerInitializer(loginName, loginPassword, new DefaultProxyListener(consumer2), consumer, sequence, bizGroup, idleCheckTime);

    bootstrap.remoteAddress(host, port);

    if (localPorts == null) {
      channelPool = new FixedChannelPool(bootstrap, new PoolHandler(mix), connections);
    } else {
      connections = localPorts.length;
      channelPool = new CmppChannelPool(bootstrap, new PoolHandler(mix));
    }

    Channel[] channels = new Channel[connections];
    try {
      for (int i = 0; i < connections; i++) {
        Future<Channel> future = channelPool.acquire().sync();
        if (future.isSuccess()) {
          channels[i] = future.getNow();
        } else {
          LOGGER.info("init channel failure when connect to [{}:{}]", host, port);
        }
      }
    } catch (InterruptedException e) {
      LOGGER.info("init channel failure when connect to [{}:{}], be interrupted", host, port);
    } finally {
      for (Channel channel : channels) {
        if (channel != null) {
          channelPool.release(channel);
        }
      }
    }

    LOGGER.info("connect to [{}:{}] success, listen localPorts:{}", host, port, Arrays.toString(localPorts));

    return this;
  }

  public void close() {

    channelPool.close();

    workGroup.shutdownGracefully().syncUninterruptibly();
    bizGroup.shutdownGracefully().syncUninterruptibly();

    LOGGER.info("shutdown gracefully, disconnect to [{}:{}] success", host, port);
  }

  public SubmitRespMessage[] submit(MessageBuilder message, int timeout) {
    if (message.msgSrc == null) {
      message.msgSrc(loginName);
    }
    SubmitMessage[] req = message.split(sequence);

    Channel channel = null;
    try {
      DefaultPromise<Channel> future = (DefaultPromise<Channel>) channelPool.acquire();
      channel = future.get(timeout, TimeUnit.MILLISECONDS);

      for (SubmitMessage submit : req) {
        channel.writeAndFlush(submit);
      }
    } catch (Exception e) {
      LOGGER.info("acquired channel failure, exception message: {}", e.getMessage());
      return null;
    } finally {
      if (channel != null) {
        channelPool.release(channel);
      }
    }

    SubmitRespMessage[] resp = new SubmitRespMessage[req.length];
    for (int i = 0; i < req.length; i++) {

      SubmitRespMessage response = null;
      try {
        response = (SubmitRespMessage) new DefaultFuture(req[i]).getResponse(timeout);
      } catch (InterruptedException e) {
        LOGGER.info("get response failure, exception message: {}", e.getMessage());
      }
      resp[i] = response;
    }

    return resp;
  }

  public static class MessageBuilder {

    private String[] phones;

    private String text;
    private SmsAlphabet alphabet = SmsAlphabet.UCS2;
    private SmsMsgClass msgClass;

    private String srcId;
    private String serviceId;
    private String msgSrc;

    private String transactionId;
    private String from;
    private int size;
    private String contentLocation;
    private int expiry = 7 * 24 * 60 * 60;

    public MessageBuilder text(String text) {
      this.text = text;
      return this;
    }

    public MessageBuilder phones(String... phones) {
      this.phones = phones;
      return this;
    }

    public MessageBuilder charset(SmsAlphabet alphabet) {
      this.alphabet = alphabet;
      return this;
    }

    public MessageBuilder messageClass(SmsMsgClass msgClass) {
      this.msgClass = msgClass;
      return this;
    }

    public MessageBuilder srcId(String srcId) {
      this.srcId = srcId;
      return this;
    }

    public MessageBuilder serviceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public MessageBuilder msgSrc(String msgSrc) {
      this.msgSrc = msgSrc;
      return this;
    }


    public MessageBuilder transactionId(String transactionId) {
      this.transactionId = transactionId;
      return this;
    }

    public MessageBuilder from(String from) {
      this.from = from;
      return this;
    }

    public MessageBuilder size(int size) {
      this.size = size;
      return this;
    }

    public MessageBuilder contentLocation(String contentLocation) {
      this.contentLocation = contentLocation;
      return this;
    }

    public MessageBuilder expiry(int expiry) {
      this.expiry = expiry;
      return this;
    }

    public SubmitMessage[] split(Sequence sequence) {

      SmsMessage message;
      if (StringUtils.isNoneBlank(text)) {
        message = new SmsTextMessage(this.text, SmsDcs.general(DcsGroup.GENERAL_DATA_CODING, alphabet, msgClass));
      } else {
        message = new SmsMmsNotificationMessage(contentLocation, size);
        ((SmsMmsNotificationMessage) message).setFrom(from + "/TYPE=PLMN");
        ((SmsMmsNotificationMessage) message).setTransactionId(transactionId);
        ((SmsMmsNotificationMessage) message).setExpiry(expiry);
      }

      SmsPdu[] pdus = message.getPdus();
      SubmitMessage[] split = new SubmitMessage[pdus.length];
      for (int i = 0; i < pdus.length; i++) {
        final SubmitMessage submit = new SubmitMessage(sequence);

        for (String phone : phones) {
          submit.getDestTerminalIds().add(phone);
        }
        submit.setSrcId(srcId);
        submit.setServiceId(serviceId);
        submit.setPkTotal(pdus.length);
        submit.setPkNumber(i + 1);
        submit.setMsgSrc(msgSrc);

        submit.setUserData(pdus[i].getUserData());
        split[i] = submit;
      }

      return split;
    }
  }

  private class CmppChannelPool extends FixedChannelPool {

    CmppChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler) {
      super(bootstrap, handler, new CmppHealthChecker(), null, -1, connections, Integer.MAX_VALUE);
    }

    @Override
    protected ChannelFuture connectChannel(Bootstrap bs) {
      CmppHealthChecker checker = (CmppHealthChecker) healthChecker();
      return bs.connect(SocketUtils.socketAddress(CmppClient.this.host, CmppClient.this.port), checker.availableAddress());
    }
  }

  private class CmppHealthChecker implements ChannelHealthChecker {

    private Deque<SocketAddress> availableAddress = PlatformDependent.newConcurrentDeque();

    CmppHealthChecker() {
      for (int localPort : CmppClient.this.localPorts) {
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
