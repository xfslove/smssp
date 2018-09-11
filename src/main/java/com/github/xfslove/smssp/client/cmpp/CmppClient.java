package com.github.xfslove.smssp.client.cmpp;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.SmsTextMessage;
import com.github.xfslove.smsj.sms.dcs.DcsGroup;
import com.github.xfslove.smsj.sms.dcs.SmsAlphabet;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.dcs.SmsMsgClass;
import com.github.xfslove.smssp.client.DefaultFuture;
import com.github.xfslove.smssp.client.ResponseListener;
import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import com.github.xfslove.smssp.message.sequence.DefaultCmppSequence;
import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.netty4.handler.cmpp20.mix.HandlerInitializer;
import com.github.xfslove.smssp.netty4.handler.cmpp20.mix.PoolHandler;
import com.github.xfslove.smssp.server.DefaultProxyListener;
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
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Deque;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public class CmppClient {

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
  private NotificationListener consumer2;

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
    this.consumer2 = new DefaultProxyListener(consumer2);
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
   * @throws InterruptedException ex
   */
  public CmppClient connect() throws InterruptedException {

    HandlerInitializer mix = new HandlerInitializer(loginName, loginPassword, consumer2, consumer, sequence, bizGroup, idleCheckTime);

    bootstrap.remoteAddress(host, port);

    if (localPorts == null) {
      channelPool = new FixedChannelPool(bootstrap, new PoolHandler(mix), connections);
    } else {
      connections = localPorts.length;
      channelPool = new CmppChannelPool(bootstrap, new PoolHandler(mix), new CmppHealthChecker());
    }

    Channel[] channels = new Channel[connections];
    try {
      for (int i = 0; i < connections; i++) {
        Future<Channel> future = channelPool.acquire().sync();
        if (future.isSuccess()) {
          channels[i] = future.getNow();
        }
      }
    } finally {
      for (Channel channel : channels) {
        if (channel != null) {
          channelPool.release(channel);
        }
      }
    }

    return this;
  }

  public void close() {

    channelPool.close();

    workGroup.shutdownGracefully().syncUninterruptibly();
    bizGroup.shutdownGracefully().syncUninterruptibly();
  }

  public SubmitRespMessage[] submit(MessageBuilder message, int timeout) throws InterruptedException {
    Future<Channel> future = channelPool.acquire().sync();
    if (!future.isSuccess()) {
      throw new InterruptedException("channel acquire failure");
    }

    SubmitMessage[] req = message.split(sequence);

    Channel channel = future.getNow();
    try {
      if (message.msgSrc == null) {
        message.msgSrc(loginName);
      }
      for (SubmitMessage submit : req) {
        channel.writeAndFlush(submit);
      }
    } finally {
      channelPool.release(channel);
    }

    SubmitRespMessage[] resp = new SubmitRespMessage[req.length];
    for (int i = 0; i < req.length; i++) {

      SubmitRespMessage response = (SubmitRespMessage) new DefaultFuture(req[i]).getResponse(timeout);
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
    private int userType = 2;
    private String feeType = "05";
    private String msgSrc;

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

    public MessageBuilder messsageClass(SmsMsgClass msgClass) {
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

    public MessageBuilder userType(int userType) {
      this.userType = userType;
      return this;
    }

    public MessageBuilder feeType(String feeType) {
      this.feeType = feeType;
      return this;
    }

    public MessageBuilder msgSrc(String msgSrc) {
      this.msgSrc = msgSrc;
      return this;
    }

    public SubmitMessage[] split(Sequence sequence) {
      SmsTextMessage text = new SmsTextMessage(this.text, SmsDcs.general(DcsGroup.GENERAL_DATA_CODING, alphabet, msgClass));

      SmsPdu[] pdus = text.getPdus();
      SubmitMessage[] split = new SubmitMessage[pdus.length];
      for (int i = 0; i < pdus.length; i++) {
        final SubmitMessage message = new SubmitMessage(sequence);

        for (String phone : phones) {
          message.getDestTerminalIds().add(phone);
        }
        message.setSrcId(srcId);
        message.setServiceId(serviceId);
        message.setFeeUserType(userType);
        message.setFeeType(feeType);
        message.setPkTotal(pdus.length);
        message.setPkNumber(i + 1);
        message.setMsgSrc(msgSrc);

        message.setUserData(pdus[i].getUserData());
        split[i] = message;
      }

      return split;
    }
  }

  private class CmppChannelPool extends FixedChannelPool {

    CmppChannelPool(Bootstrap bootstrap, ChannelPoolHandler handler, ChannelHealthChecker healthChecker) {
      super(bootstrap, handler, healthChecker, null, -1, connections, Integer.MAX_VALUE);
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
