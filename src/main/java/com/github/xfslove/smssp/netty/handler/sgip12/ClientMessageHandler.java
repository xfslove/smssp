package com.github.xfslove.smssp.netty.handler.sgip12;

import com.github.xfslove.smssp.message.sgip12.SubmitMessage;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.AttributeKey;

import java.util.concurrent.BlockingQueue;

/**
 * todo
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class ClientMessageHandler extends ChannelDuplexHandler {

  private AttributeKey<BlockingQueue<SubmitRespMessage>> submitRespQueue = AttributeKey.valueOf("submitRespQueue");

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof SubmitRespMessage) {
      SubmitRespMessage submitResp = (SubmitRespMessage) msg;

      ctx.channel().attr(submitRespQueue).get().offer(submitResp);
    }

  }



  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    if (msg instanceof SubmitMessage) {
      SubmitMessage submit = (SubmitMessage) msg;


      ctx.writeAndFlush(msg);
    }

  }
}
