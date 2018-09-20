package com.github.xfslove.smssp.transport.netty4.handler.cmpp20.client;

import com.github.xfslove.smssp.exchange.ResponseListener;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private ResponseListener consumer;

  public SubmitHandler(ResponseListener consumer) {
    this.consumer = consumer;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // submitResp
    if (msg instanceof SubmitRespMessage) {
      consumer.done((SubmitRespMessage) msg);
      return;
    }

    ctx.fireChannelRead(msg);
  }

}