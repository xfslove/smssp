package com.github.xfslove.smssp.netty4.handler.sgip12.subscriber;

import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.sgip12.MessageCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class SubscriberHandlerInitializer extends ChannelInitializer<Channel> {

  private final LogLevel logLevel = LogLevel.INFO;
  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private DeliverConsumer consumer;

  private int idleCheckInterval = 5 * 60;

  private String loginName;

  private String loginPassword;

  public SubscriberHandlerInitializer(DeliverConsumer consumer, String loginName, String loginPassword) {
    this.consumer = consumer;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = logLevel.toInternalLevel();
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {
    channel.pipeline().addLast("sgipSocketLogging", new LoggingHandler(LogLevel.INFO));
    channel.pipeline().addLast("sgipIdleState", new IdleStateHandler(0, 0, idleCheckInterval, TimeUnit.SECONDS));
    channel.pipeline().addLast("sgipMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("sgipMessageCodec", new MessageCodec());
    channel.pipeline().addLast("sgipMessageLogging", new LoggingHandler(logLevel));
    channel.pipeline().addLast("sgipSessionHandler", new SubscriberSessionHandler(loginName, loginPassword, logLevel));
    channel.pipeline().addLast("sgipMessageHandler", new SubscriberMessageHandler(consumer, logLevel));

    logger.log(internalLevel, "initialized sender pipeline[sgipSocketLogging, sgipIdleState, sgipMessageLengthCodec, sgipMessageCodec, sgipMessageLogging, sgipSessionHandler, sgipMessageHandler]");
  }

  public void setIdleCheckInterval(int idleCheckInterval) {
    this.idleCheckInterval = idleCheckInterval;
  }
}