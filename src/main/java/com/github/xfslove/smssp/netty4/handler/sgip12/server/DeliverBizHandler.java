package com.github.xfslove.smssp.netty4.handler.sgip12.server;

import com.github.xfslove.smssp.message.sgip12.DeliverMessage;
import com.github.xfslove.smssp.message.sgip12.ReportMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * smg -&gt; sp 消息的handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class DeliverBizHandler extends ChannelDuplexHandler {

  private DeliverConsumer consumer;

  public DeliverBizHandler(DeliverConsumer consumer) {
    this.consumer = consumer;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof DeliverMessage) {
      DeliverMessage deliver = (DeliverMessage) msg;
      consumer.apply(deliver);
      return;
    }

    if (msg instanceof ReportMessage) {
      ReportMessage report = (ReportMessage) msg;
      consumer.apply(report);
      return;
    }

    ctx.fireChannelRead(msg);
  }
}
