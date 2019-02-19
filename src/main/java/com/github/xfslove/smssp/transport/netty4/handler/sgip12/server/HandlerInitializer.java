package com.github.xfslove.smssp.transport.netty4.handler.sgip12.server;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import com.github.xfslove.smssp.notification.NotificationListener;
import com.github.xfslove.smssp.transport.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.transport.netty4.codec.sgip12.MessageCodec;
import com.github.xfslove.smssp.transport.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.transport.netty4.handler.sgip12.UnBindHandler;
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

  private NotificationListener consumer;

  private String username;

  private String password;

  private Sequence<SequenceNumber> sequence;

  private int idleCheckTime;

  public HandlerInitializer(String username, String password, NotificationListener consumer, Sequence<SequenceNumber> sequence, int idleCheckTime) {
    this.username = username;
    this.password = password;
    this.consumer = consumer;
    this.sequence = sequence;
    this.idleCheckTime = idleCheckTime;
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {

    // todo sharable add static handler
    channel.pipeline().addLast("sgipSocketLogging", new LoggingHandler());
    channel.pipeline().addLast("sgipIdleState", new IdleStateHandler(0, 0, idleCheckTime, TimeUnit.SECONDS));
    channel.pipeline().addLast("sgipMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("sgipMessageCodec", new MessageCodec());
    channel.pipeline().addLast("sgipMessageLogging", new LoggingHandler(LogLevel.DEBUG));

    channel.pipeline().addLast("sgipBindHandler", new BindHandler(username, password));
    channel.pipeline().addLast("sgipUnBindHandler", new UnBindHandler(sequence));
    channel.pipeline().addLast("sgipDeliverHandler", new DeliverHandler(consumer));
    channel.pipeline().addLast("sgipException", new ExceptionHandler());

  }
}