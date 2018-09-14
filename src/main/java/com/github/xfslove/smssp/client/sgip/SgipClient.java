package com.github.xfslove.smssp.client.sgip;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.SmsTextMessage;
import com.github.xfslove.smsj.sms.dcs.DcsGroup;
import com.github.xfslove.smsj.sms.dcs.SmsAlphabet;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.dcs.SmsMsgClass;
import com.github.xfslove.smssp.client.DefaultFuture;
import com.github.xfslove.smssp.client.ResponseListener;
import com.github.xfslove.smssp.message.sequence.DefaultSgipSequence;
import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.message.sgip12.SubmitMessage;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import com.github.xfslove.smssp.netty4.handler.sgip12.client.HandlerInitializer;
import com.github.xfslove.smssp.netty4.handler.sgip12.client.PoolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author hanwen
 * created at 2018/9/13
 */
public class SgipClient {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(SgipClient.class);

  private EventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("sgipWorker", true));

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
  private int idleCheckTime = 30;
  private int connections = 1;

  private Sequence sequence;
  private ResponseListener consumer = new DefaultFuture.DefaultListener();

  private SgipClient(int nodeId) {
    this.sequence = new DefaultSgipSequence(nodeId);
  }

  public static SgipClient newConnection(int nodeId) {
    return new SgipClient(nodeId);
  }

  public SgipClient loginName(String loginName) {
    this.loginName = loginName;
    return this;
  }

  public SgipClient loginPassword(String loginPassword) {
    this.loginPassword = loginPassword;
    return this;
  }

  public SgipClient host(String host) {
    this.host = host;
    return this;
  }

  public SgipClient port(int port) {
    this.port = port;
    return this;
  }

  public SgipClient idleCheckTime(int idleCheckTime) {
    this.idleCheckTime = idleCheckTime;
    return this;
  }

  public SgipClient connections(int connections) {
    this.connections = connections;
    return this;
  }

  public SgipClient sequence(Sequence sequence) {
    this.sequence = sequence;
    return this;
  }

  public SgipClient responseListener(ResponseListener consumer) {
    this.consumer = consumer;
    return this;
  }

  public SgipClient connect() {

    HandlerInitializer handler = new HandlerInitializer(loginName, loginPassword, consumer, sequence, bizGroup, idleCheckTime);

    bootstrap.remoteAddress(host, port);

    channelPool = new FixedChannelPool(bootstrap, new PoolHandler(handler), connections);

    return this;
  }

  public SubmitRespMessage[] submit(MessageBuilder message, int timeout) {
    SubmitMessage[] req = message.split(sequence);

    Channel channel = null;
    try {
      Future<Channel> future = channelPool.acquire().sync();
      if (!future.isSuccess()) {
        LOGGER.info("acquired channel failure");
        return null;
      }
      channel = future.getNow();

      for (SubmitMessage submit : req) {
        channel.writeAndFlush(submit);
      }
    } catch (InterruptedException e) {
      LOGGER.info("acquired channel failure, be interrupted");
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
        LOGGER.info("get response failure, be interrupted");
      }
      resp[i] = response;
    }

    return resp;

  }

  public void close() {

    channelPool.close();

    workGroup.shutdownGracefully().syncUninterruptibly();
    bizGroup.shutdownGracefully().syncUninterruptibly();

    LOGGER.info("shutdown gracefully, disconnect to [{}:{}] success", host, port);
  }

  public static class MessageBuilder {

    private String[] phones;

    private String text;
    private SmsAlphabet alphabet = SmsAlphabet.UCS2;
    private SmsMsgClass msgClass;

    private String spNumber;
    private String corpId;
    private String serviceType;
    private int morelatetoMTFlag = 3;

    public MessageBuilder phones(String... phones) {
      this.phones = phones;
      return this;
    }

    public MessageBuilder text(String text) {
      this.text = text;
      return this;
    }

    public MessageBuilder alphabet(SmsAlphabet alphabet) {
      this.alphabet = alphabet;
      return this;
    }

    public MessageBuilder messageClass(SmsMsgClass msgClass) {
      this.msgClass = msgClass;
      return this;
    }

    public MessageBuilder spNumber(String spNumber) {
      this.spNumber = spNumber;
      return this;
    }

    public MessageBuilder corpId(String corpId) {
      this.corpId = corpId;
      return this;
    }

    public MessageBuilder serviceType(String serviceType) {
      this.serviceType = serviceType;
      return this;
    }

    public MessageBuilder morelatetoMTFlag(int morelatetoMTFlag) {
      this.morelatetoMTFlag = morelatetoMTFlag;
      return this;
    }

    public SubmitMessage[] split(Sequence sequence) {

      SmsTextMessage text = new SmsTextMessage(this.text, SmsDcs.general(DcsGroup.GENERAL_DATA_CODING, alphabet, msgClass));

      SmsPdu[] pdus = text.getPdus();
      SubmitMessage[] split = new SubmitMessage[pdus.length];
      for (int i = 0; i < pdus.length; i++) {
        final SubmitMessage message = new SubmitMessage(sequence);

        for (String phone : phones) {
          message.getUserNumbers().add(phone);
        }
        message.setSpNumber(spNumber);
        message.setCorpId(corpId);
        message.setServiceType(serviceType);
        message.setMorelatetoMTFlag(morelatetoMTFlag);

        message.setUserData(pdus[i].getUserData());
        split[i] = message;
      }

      return split;
    }
  }
}
