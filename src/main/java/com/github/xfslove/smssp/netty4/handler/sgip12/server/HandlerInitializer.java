package com.github.xfslove.smssp.netty4.handler.sgip12.server;

import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.sgip12.MessageCodec;
import com.github.xfslove.smssp.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.netty4.handler.sgip12.UnBindHandler;
import com.github.xfslove.smssp.server.NotificationListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class HandlerInitializer extends ChannelInitializer<Channel> {

  private NotificationListener consumer;

  private String loginName;

  private String loginPassword;

  private Sequence sequence;

  private EventExecutorGroup bizEventGroup;

  private int idleCheckTime;

  public HandlerInitializer(String loginName, String loginPassword, NotificationListener consumer, Sequence sequence, EventExecutorGroup bizEventGroup, int idleCheckTime) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.consumer = consumer;
    this.sequence = sequence;
    this.bizEventGroup = bizEventGroup;
    this.idleCheckTime = idleCheckTime;
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {
    channel.pipeline().addLast("sgipSocketLogging", new LoggingHandler());
    channel.pipeline().addLast("sgipIdleState", new IdleStateHandler(0, 0, idleCheckTime, TimeUnit.SECONDS));
    channel.pipeline().addLast("sgipMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("sgipMessageCodec", new MessageCodec());
    channel.pipeline().addLast("sgipMessageLogging", new LoggingHandler(LogLevel.INFO));

    channel.pipeline().addLast("sgipBindHandler", new BindHandler(loginName, loginPassword));
    channel.pipeline().addLast("sgipUnBindHandler", new UnBindHandler(sequence));
    channel.pipeline().addLast(bizEventGroup, "sgipDeliverHandler", new DeliverHandler(consumer));
    channel.pipeline().addLast("sgipException", new ExceptionHandler());

  }
}