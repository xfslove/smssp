package com.github.xfslove.smssp.netty.handler.sgip12;

import com.github.xfslove.smssp.message.sgip12.ReportRespMessage;
import com.github.xfslove.smssp.message.sgip12.SgipMessage;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class ClientMessageHandler extends ChannelDuplexHandler {

  // todo
  private AttributeKey<BlockingQueue<SgipMessage>> submitRespQueue = AttributeKey.valueOf("submitRespQueue");

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 接受submitResp的queue
    ctx.channel().attr(submitRespQueue).set(new LinkedBlockingQueue<>(16));

    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof ReportRespMessage) {
      ReportRespMessage submitResp = (ReportRespMessage) msg;

      BlockingQueue<SgipMessage> respQueue = ctx.channel().attr(submitRespQueue).get();

      respQueue.offer(submitResp);
      return;
    }

    ctx.fireChannelRead(msg);

  }

}
