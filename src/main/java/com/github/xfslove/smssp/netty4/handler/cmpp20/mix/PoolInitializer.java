package com.github.xfslove.smssp.netty4.handler.cmpp20.mix;

import com.github.xfslove.smssp.message.seq.SequenceGenerator;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.cmpp20.MessageCodec;
import com.github.xfslove.smssp.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.ActiveTestHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.TerminateHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.send.ConnectHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.send.SubmitBiConsumer;
import com.github.xfslove.smssp.netty4.handler.cmpp20.send.SubmitHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.subscribe.DeliverConsumer;
import com.github.xfslove.smssp.netty4.handler.cmpp20.subscribe.DeliverHandler;
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

  private int windowSize = 32;

  private String loginName;

  private String loginPassword;

  private DeliverConsumer deliverConsumer;

  private SubmitBiConsumer submitBiConsumer;

  private SequenceGenerator sequenceGenerator;

  public PoolInitializer(String loginName, String loginPassword, DeliverConsumer deliverConsumer, SubmitBiConsumer submitBiConsumer, SequenceGenerator sequenceGenerator) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.deliverConsumer = deliverConsumer;
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
    channel.pipeline().addLast("cmppSubmitHandler", new SubmitHandler(loginName, sequenceGenerator, submitBiConsumer, windowSize, logLevel));
    channel.pipeline().addLast("cmppDeliverHandler", new DeliverHandler(loginName, deliverConsumer, logLevel));
    channel.pipeline().addLast("cmppException", new ExceptionHandler(loginName, logLevel));

  }

  public void setIdleCheckInterval(int idleCheckInterval) {
    this.idleCheckInterval = idleCheckInterval;
  }

  public void setWindowSize(int windowSize) {
    this.windowSize = windowSize;
  }
}
