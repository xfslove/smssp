package com.github.xfslove.smssp.netty4.handler.cmpp20.mix;

import com.github.xfslove.smssp.client.ResponseConsumer;
import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.cmpp20.MessageCodec;
import com.github.xfslove.smssp.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.ActiveTestHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.TerminateHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.client.ConnectHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.client.SubmitHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.server.DeliverBizHandler;
import com.github.xfslove.smssp.netty4.handler.cmpp20.server.DeliverConsumer;
import com.github.xfslove.smssp.netty4.handler.cmpp20.server.DeliverHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

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

  private DeliverConsumer deliverConsumer;

  private ResponseConsumer consumer;

  private Sequence sequence;

  public HandlerInitializer(String loginName, String loginPassword, DeliverConsumer deliverConsumer, ResponseConsumer consumer, Sequence sequence) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.deliverConsumer = deliverConsumer;
    this.consumer = consumer;
    this.sequence = sequence;
  }

  @Override
  protected void initChannel(Channel channel) throws Exception {

    channel.pipeline().addLast("cmppSocketLogging", new LoggingHandler());
    channel.pipeline().addLast("cmppIdleState", new IdleStateHandler(0, 0, 5 * 60, TimeUnit.SECONDS));
    channel.pipeline().addLast("cmppMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("cmppMessageCodec", new MessageCodec());
    channel.pipeline().addLast("cmppMessageLogging", new LoggingHandler(LogLevel.INFO));

    channel.pipeline().addLast("cmppConnectHandler", new ConnectHandler(loginName, loginPassword, sequence));
    channel.pipeline().addLast("cmppActiveTestHandler", new ActiveTestHandler(sequence, true));
    channel.pipeline().addLast("cmppTerminateHandler", new TerminateHandler(sequence));
    channel.pipeline().addLast("cmppSubmitHandler", new SubmitHandler(consumer));
    channel.pipeline().addLast("cmppDeliverHandler", new DeliverHandler());
    channel.pipeline().addLast(new DefaultEventExecutorGroup(16), "cmppDeliverBizHandler", new DeliverBizHandler(deliverConsumer));
    channel.pipeline().addLast("cmppException", new ExceptionHandler());

  }
}
