package com.github.xfslove.smssp.netty4.handler.cmpp20.client;

import com.github.xfslove.smssp.client.ResponseConsumer;
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

  private ResponseConsumer consumer;

  public SubmitHandler(ResponseConsumer consumer) {
    this.consumer = consumer;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // submitResp
    if (msg instanceof SubmitRespMessage) {
      consumer.apply((SubmitRespMessage) msg);
      return;
    }

    ctx.fireChannelRead(msg);
  }

}
