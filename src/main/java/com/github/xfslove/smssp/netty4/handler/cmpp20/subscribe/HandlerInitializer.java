package com.github.xfslove.smssp.netty4.handler.cmpp20.subscribe;

import com.github.xfslove.smssp.message.seq.SequenceGenerator;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.cmpp20.MessageCodec;
import com.github.xfslove.smssp.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.ActiveTestHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.TerminateHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class HandlerInitializer extends ChannelInitializer<Channel> {

  private LogLevel logLevel = LogLevel.INFO;

  private int idleCheckInterval = 5 * 60;

  private String loginName;

  private String loginPassword;

  private DeliverConsumer deliverConsumer;

  private SequenceGenerator sequenceGenerator;

  public HandlerInitializer(String loginName, String loginPassword, DeliverConsumer deliverConsumer, SequenceGenerator sequenceGenerator) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.deliverConsumer = deliverConsumer;
    this.sequenceGenerator = sequenceGenerator;
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {
    channel.pipeline().addLast("cmppSocketLogging", new LoggingHandler(logLevel));
    channel.pipeline().addLast("cmppIdleState", new IdleStateHandler(0, 0, idleCheckInterval, TimeUnit.SECONDS));
    channel.pipeline().addLast("cmppMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("cmppMessageCodec", new MessageCodec());
    channel.pipeline().addLast("cmppMessageLogging", new LoggingHandler(logLevel));

    channel.pipeline().addLast("cmppConnectHandler", new ConnectHandler(loginName, loginPassword, logLevel));
    channel.pipeline().addLast("cmppActiveTestHandler", new ActiveTestHandler(sequenceGenerator, loginName, true, logLevel));
    channel.pipeline().addLast("cmppTerminateHandler", new TerminateHandler(sequenceGenerator, loginName, logLevel));
    channel.pipeline().addLast("cmppDeliverHandler", new DeliverHandler(deliverConsumer, loginName, logLevel));
    channel.pipeline().addLast("cmppException", new ExceptionHandler(loginName, logLevel));
  }

  public void setIdleCheckInterval(int idleCheckInterval) {
    this.idleCheckInterval = idleCheckInterval;
  }
}