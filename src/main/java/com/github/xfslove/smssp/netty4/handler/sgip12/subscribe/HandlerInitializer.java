package com.github.xfslove.smssp.netty4.handler.sgip12.subscribe;

import com.github.xfslove.smssp.message.sequence.SequenceGenerator;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.sgip12.MessageCodec;
import com.github.xfslove.smssp.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.netty4.handler.sgip12.UnBindHandler;
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

  private final LogLevel logLevel = LogLevel.INFO;

  private DeliverConsumer deliverConsumer;

  private int idleCheckInterval = 5 * 60;

  private int nodeId;

  private String loginName;

  private String loginPassword;

  private SequenceGenerator sequenceGenerator;

  public HandlerInitializer(int nodeId, String loginName, String loginPassword, DeliverConsumer deliverConsumer, SequenceGenerator sequenceGenerator) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.deliverConsumer = deliverConsumer;
    this.sequenceGenerator = sequenceGenerator;
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {
    channel.pipeline().addLast("sgipSocketLogging", new LoggingHandler(LogLevel.INFO));
    channel.pipeline().addLast("sgipIdleState", new IdleStateHandler(0, 0, idleCheckInterval, TimeUnit.SECONDS));
    channel.pipeline().addLast("sgipMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("sgipMessageCodec", new MessageCodec());
    channel.pipeline().addLast("sgipMessageLogging", new LoggingHandler(logLevel));

    channel.pipeline().addLast("sgipBindHandler", new BindHandler(loginName, loginPassword, logLevel));
    channel.pipeline().addLast("sgipUnBindHandler", new UnBindHandler(nodeId, loginName, sequenceGenerator, logLevel));
    channel.pipeline().addLast("sgipReportHandler", new ReportHandler(deliverConsumer, logLevel));
    channel.pipeline().addLast("sgipDeliverHandler", new DeliverHandler(deliverConsumer, logLevel));
    channel.pipeline().addLast("sgipException", new ExceptionHandler(loginName, logLevel));

  }

  public void setIdleCheckInterval(int idleCheckInterval) {
    this.idleCheckInterval = idleCheckInterval;
  }
}