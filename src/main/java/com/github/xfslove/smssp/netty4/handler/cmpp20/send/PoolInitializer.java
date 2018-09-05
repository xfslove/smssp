package com.github.xfslove.smssp.netty4.handler.cmpp20.send;

import com.github.xfslove.smssp.message.sequence.SequenceGenerator;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.cmpp20.MessageCodec;
import com.github.xfslove.smssp.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.ActiveTestHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.TerminateHandler;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class PoolInitializer implements ChannelPoolHandler {

  private final LogLevel logLevel = LogLevel.INFO;

  private int idleCheckInterval = 5 * 60;

  private String loginName;

  private String loginPassword;

  private SubmitBiConsumer submitBiConsumer;

  private SequenceGenerator sequenceGenerator;

  public PoolInitializer(String loginName, String loginPassword, SubmitBiConsumer submitBiConsumer, SequenceGenerator sequenceGenerator) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.submitBiConsumer = submitBiConsumer;
    this.sequenceGenerator = sequenceGenerator;
  }

  @Override
  public void channelReleased(Channel channel) throws Exception {
    // nothing
  }

  @Override
  public void channelAcquired(Channel channel) throws Exception {
    // nothing
  }

  @Override
  public void channelCreated(Channel channel) throws Exception {

    channel.pipeline().addLast("cmppSocketLogging", new LoggingHandler(logLevel));
    channel.pipeline().addLast("cmppIdleState", new IdleStateHandler(0, 0, idleCheckInterval, TimeUnit.SECONDS));
    channel.pipeline().addLast("cmppMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("cmppMessageCodec", new MessageCodec());
    channel.pipeline().addLast("cmppMessageLogging", new LoggingHandler(logLevel));

    channel.pipeline().addLast("cmppConnectHandler", new ConnectHandler(loginName, loginPassword, sequenceGenerator, logLevel));
    channel.pipeline().addLast("cmppActiveTestHandler", new ActiveTestHandler(loginName, sequenceGenerator, true, logLevel));
    channel.pipeline().addLast("cmppTerminateHandler", new TerminateHandler(loginName, sequenceGenerator, logLevel));
    channel.pipeline().addLast("cmppSubmitHandler", new SubmitHandler(loginName, sequenceGenerator, submitBiConsumer, logLevel));
    channel.pipeline().addLast("cmppException", new ExceptionHandler(loginName, logLevel));
  }
}
