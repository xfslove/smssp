package com.github.xfslove.smssp.transport.netty4.handler.cmpp20.mix;

import com.github.xfslove.smssp.exchange.ResponseListener;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.transport.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.transport.netty4.codec.cmpp20.MessageCodec;
import com.github.xfslove.smssp.transport.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.transport.netty4.handler.cmpp20.ActiveTestHandler;
import com.github.xfslove.smssp.transport.netty4.handler.cmpp20.TerminateHandler;
import com.github.xfslove.smssp.transport.netty4.handler.cmpp20.client.ConnectHandler;
import com.github.xfslove.smssp.transport.netty4.handler.cmpp20.client.SubmitHandler;
import com.github.xfslove.smssp.transport.netty4.handler.cmpp20.server.DeliverHandler;
import com.github.xfslove.smssp.notification.NotificationListener;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.concurrent.TimeUnit;

/**
 * cmpp 长链接，deliver会提交到客户端发起的链接，这个init包含了{@link DeliverHandler}
 *
 * @author hanwen
 * created at 2018/9/1
 */
public class HandlerInitializer extends ChannelInitializer<Channel> {

  private String loginName;

  private String loginPassword;

  private NotificationListener consumer2;

  private ResponseListener consumer;

  private Sequence<Integer> sequence;

  private EventExecutorGroup bizEventGroup;

  private int idleCheckTime;

  public HandlerInitializer(String loginName, String loginPassword, NotificationListener consumer2, ResponseListener consumer, Sequence<Integer> sequence, EventExecutorGroup bizEventGroup, int idleCheckTime) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.consumer2 = consumer2;
    this.consumer = consumer;
    this.sequence = sequence;
    this.bizEventGroup = bizEventGroup;
    this.idleCheckTime = idleCheckTime;
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {

    channel.pipeline().addLast("cmppSocketLogging", new LoggingHandler());
    channel.pipeline().addLast("cmppIdleState", new IdleStateHandler(0, 0, idleCheckTime, TimeUnit.SECONDS));
    channel.pipeline().addLast("cmppMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("cmppMessageCodec", new MessageCodec());
    channel.pipeline().addLast("cmppMessageLogging", new LoggingHandler(LogLevel.DEBUG));

    channel.pipeline().addLast("cmppConnectHandler", new ConnectHandler(loginName, loginPassword, sequence));
    channel.pipeline().addLast("cmppActiveTestHandler", new ActiveTestHandler(sequence, true));
    channel.pipeline().addLast("cmppTerminateHandler", new TerminateHandler(sequence));
    channel.pipeline().addLast(bizEventGroup, "cmppSubmitHandler", new SubmitHandler(consumer));
    channel.pipeline().addLast(bizEventGroup, "cmppDeliverHandler", new DeliverHandler(consumer2));
    channel.pipeline().addLast("cmppException", new ExceptionHandler());

  }
}
