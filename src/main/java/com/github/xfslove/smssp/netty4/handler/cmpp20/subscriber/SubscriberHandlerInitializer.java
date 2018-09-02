package com.github.xfslove.smssp.netty4.handler.cmpp20.subscriber;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.cmpp20.MessageCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class SubscriberHandlerInitializer extends ChannelInitializer<Channel> {

  private final LogLevel logLevel = LogLevel.DEBUG;
  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private Consumer<Message> consumer;

  private int idleCheckInterval = 5 * 60;

  private String loginName;

  private String loginPassword;

  public SubscriberHandlerInitializer(Consumer<Message> consumer, String loginName, String loginPassword) {
    this.consumer = consumer;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = logLevel.toInternalLevel();
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {
    channel.pipeline().addLast("cmppSocketLogging", new LoggingHandler(LogLevel.DEBUG));
    channel.pipeline().addLast("cmppIdleState", new IdleStateHandler(0, 0, idleCheckInterval, TimeUnit.SECONDS));
    channel.pipeline().addLast("cmppMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("cmppMessageCodec", new MessageCodec());
    channel.pipeline().addLast("cmppMessageLogging", new LoggingHandler(logLevel));
    channel.pipeline().addLast("cmppSessionHandler", new SubscriberSessionHandler(loginName, loginPassword, logLevel));
    channel.pipeline().addLast("cmppMessageHandler", new SubscriberMessageHandler(consumer, logLevel));

    logger.log(internalLevel, "initialized sender pipeline[cmppSocketLogging, cmppIdleState, cmppMessageLengthCodec, cmppMessageCodec, cmppMessageLogging, cmppSessionHandler, cmppMessageHandler]");
  }

  public void setIdleCheckInterval(int idleCheckInterval) {
    this.idleCheckInterval = idleCheckInterval;
  }
}