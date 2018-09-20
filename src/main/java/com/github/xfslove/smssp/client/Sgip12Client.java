package com.github.xfslove.smssp.client;

import com.github.xfslove.smsj.sms.SmsMessage;
import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.SmsTextMessage;
import com.github.xfslove.smsj.sms.dcs.DcsGroup;
import com.github.xfslove.smsj.sms.dcs.SmsAlphabet;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.dcs.SmsMsgClass;
import com.github.xfslove.smsj.wap.mms.SmsMmsNotificationMessage;
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
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/13
 */
public class Sgip12Client {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(Sgip12Client.class);

  private EventLoopGroup workGroup = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32), new DefaultThreadFactory("sgipWorker", true));

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

  private int idleCheckTime = 30;
  private int connections = 1;

  private int nodeId;
  private Sequence<SequenceNumber> sequence;
  private ResponseListener consumer;

  private Sgip12Client(int nodeId, String loginName, String loginPassword, String host, int port) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.host = host;
    this.port = port;
    this.sequence = new DefaultSequence(nodeId);
    this.consumer = new DefaultFuture.DefaultListener(loginName);
  }

  public static Sgip12Client newConnection(int nodeId, String loginName, String loginPassword, String host, int port) {
    return new Sgip12Client(nodeId, loginName, loginPassword, host, port);
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

    HandlerInitializer handler = new HandlerInitializer(loginName, loginPassword, consumer, sequence, bizGroup, idleCheckTime);

    bootstrap.remoteAddress(host, port);

    channelPool = new FixedChannelPool(bootstrap, new PoolHandler(handler), connections);

    LOGGER.info("init connection pool to [{}:{}] success", host, port);
    return this;
  }

  public SubmitRespMessage[] submit(MessageBuilder message, int timeout) {
    SubmitMessage[] req = message.split(sequence);
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

    SubmitRespMessage[] resp = new SubmitRespMessage[futures.length];
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

  public void close() {

    channelPool.close();

    workGroup.shutdownGracefully().syncUninterruptibly();
    bizGroup.shutdownGracefully().syncUninterruptibly();

    if (consumer instanceof DefaultFuture.DefaultListener) {
      DefaultFuture.cleanUp(loginName);
    }

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

    private String transactionId;
    private String from;
    private int size;
    private String contentLocation;
    private int expiry = 7 * 24 * 60 * 60;

    public MessageBuilder phones(String... phones) {
      this.phones = phones;
      return this;
    }

    public MessageBuilder text(String text) {
      this.text = text;
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

    public SubmitMessage[] split(Sequence<SequenceNumber> sequence) {

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
          submit.getUserNumbers().add(phone);
        }
        submit.setSpNumber(spNumber);
        submit.setCorpId(corpId);
        submit.setServiceType(serviceType);
        submit.setMorelatetoMTFlag(morelatetoMTFlag);

        submit.setUserDataHeaders(pdus[i].getUserDateHeaders());
        submit.setUserData(pdus[i].getUserData());
        split[i] = submit;
      }

      return split;
    }

    @Override
    public String toString() {
      return "SubmitMessage{" +
          "phones=" + Arrays.toString(phones) +
          ", text='" + text + '\'' +
          ", alphabet=" + alphabet +
          ", msgClass=" + msgClass +
          ", spNumber='" + spNumber + '\'' +
          ", corpId='" + corpId + '\'' +
          ", serviceType='" + serviceType + '\'' +
          ", morelatetoMTFlag=" + morelatetoMTFlag +
          ", transactionId='" + transactionId + '\'' +
          ", from='" + from + '\'' +
          ", size=" + size +
          ", contentLocation='" + contentLocation + '\'' +
          ", expiry=" + expiry +
          '}';
    }
  }
}
