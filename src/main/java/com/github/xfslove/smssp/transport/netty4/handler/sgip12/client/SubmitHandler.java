package com.github.xfslove.smssp.transport.netty4.handler.sgip12.client;

import com.github.xfslove.smssp.exchange.ResponseListener;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author hanwen
 * created at 2018/9/1
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private ResponseListener listener;

  public SubmitHandler(ResponseListener listener) {
    this.listener = listener;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // submitResp
    if (msg instanceof SubmitRespMessage) {
      listener.done((SubmitRespMessage) msg);
      return;
    }

    ctx.fireChannelRead(msg);

  }
}
