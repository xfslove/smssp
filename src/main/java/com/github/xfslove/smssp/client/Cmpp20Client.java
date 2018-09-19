package com.github.xfslove.smssp.client;

import com.github.xfslove.smsj.sms.SmsMessage;
import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.SmsTextMessage;
import com.github.xfslove.smsj.sms.dcs.DcsGroup;
import com.github.xfslove.smsj.sms.dcs.SmsAlphabet;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.dcs.SmsMsgClass;
import com.github.xfslove.smsj.wap.mms.SmsMmsNotificationMessage;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.cmpp20.DefaultSequence;
import com.github.xfslove.smssp.message.cmpp20.MsgId;
import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
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
import java.util.Calendar;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public class Cmpp20Client {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(Cmpp20Client.class);

  private EventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("cmppWorker", true));

  private Bootstrap bootstrap = new Bootstrap().group(workGroup)
      .channel(NioSocketChannel.class)
      .option(ChannelOption.SO_KEEPALIVE, true)
      .option(ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.SO_REUSEADDR, true)
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

  private int nodeId;
  private Sequence<Integer> sequence;
  private ResponseListener consumer;
  private NotificationListener consumer2;

  private Cmpp20Client(int nodeId, String loginName, String loginPassword, String host, int port) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.host = host;
    this.port = port;
    this.sequence = new DefaultSequence();
    this.consumer = new DefaultFuture.DefaultListener(loginName);
    this.consumer2 = new DefaultProxyListener(loginName, new NotificationListener() {
      @Override
      public void done(Notification notification) {
        LOGGER.info("received notification: {}", notification);
      }
    });
  }

  public static Cmpp20Client newConnection(int nodeId, String loginName, String loginPassword, String host, int port) {
    return new Cmpp20Client(nodeId, loginName, loginPassword, host, port);
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

  public Cmpp20Client responseListener(ResponseListener consumer) {
    this.consumer = consumer;
    return this;
  }

  public Cmpp20Client notificationListener(NotificationListener consumer2) {
    this.consumer2 = consumer2;
    return this;
  }

  public Cmpp20Client idleCheckTime(int idleCheckTime) {
    this.idleCheckTime = idleCheckTime;
    return this;
  }

  /**
   * 建立链接（长链接），会初始化链接，并保持
   *
   * @return this
   */
  public Cmpp20Client connect() {

    HandlerInitializer mix = new HandlerInitializer(loginName, loginPassword, consumer2, consumer, sequence, bizGroup, idleCheckTime);

    bootstrap.remoteAddress(host, port);

    if (localPorts == null) {
      channelPool = new FixedChannelPool(bootstrap, new PoolHandler(mix), connections);
    } else {
      connections = localPorts.length;
      channelPool = new CmppChannelPool(bootstrap, new PoolHandler(mix));
    }

    LOGGER.info("init connection pool to [{}:{}] success", host, port);
    return this;
  }

  public void close() {

    channelPool.close();

    workGroup.shutdownGracefully().syncUninterruptibly();
    bizGroup.shutdownGracefully().syncUninterruptibly();

    if (consumer instanceof DefaultFuture.DefaultListener) {
      DefaultFuture.cleanUp(loginName);
    }
    if (consumer2 instanceof DefaultProxyListener) {
      DefaultProxyListener.cleanUp(loginName);
    }

    LOGGER.info("shutdown gracefully, disconnect to [{}:{}] success", host, port);
  }

  public SubmitRespMessage[] submit(MessageBuilder message, int timeout) {
    if (message.msgSrc == null) {
      message.msgSrc(loginName);
    }
    SubmitMessage[] req = message.split(nodeId, sequence);
    DefaultFuture[] futures = new DefaultFuture[req.length];
    for (int i = 0; i < req.length; i++) {
      futures[i] = new DefaultFuture(loginName, req[i]);
    }

    Channel channel = null;
    try {
      DefaultPromise<Channel> future = (DefaultPromise<Channel>) channelPool.acquire();
      channel = future.get(timeout, TimeUnit.MILLISECONDS);

      for (SubmitMessage submit : req) {
        channel.writeAndFlush(submit);
      }
    } catch (Exception e) {
      LOGGER.warn("acquired channel failure, exception message: {}", e.getMessage());
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
        response = (SubmitRespMessage) futures[i].getResponse(timeout);
      } catch (InterruptedException e) {
        LOGGER.warn("get response failure, exception message: {}", e.getMessage());
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

    public SubmitMessage[] split(int nodeId, Sequence<Integer> sequence) {

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

      Calendar calendar = Calendar.getInstance();
      MsgId msgId = new MsgId(nodeId, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), sequence.next());

      for (int i = 0; i < pdus.length; i++) {
        final SubmitMessage submit = new SubmitMessage(sequence);

        submit.setMsgId(msgId);

        for (String phone : phones) {
          submit.getDestTerminalIds().add(phone);
        }
        submit.setSrcId(srcId);
        submit.setServiceId(serviceId);
        submit.setPkTotal(pdus.length);
        submit.setPkNumber(i + 1);
        submit.setMsgSrc(msgSrc);

        submit.setUserDataHeaders(pdus[i].getUserDateHeaders());
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
