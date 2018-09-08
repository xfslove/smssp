package com.github.xfslove.smssp.netty4.handler.cmpp20.server;

import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;


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

      if (deliver.getRegisteredDelivery() == 1) {
        // 状态报告
        ByteBuf in = Unpooled.wrappedBuffer(deliver.getUdBytes());
        DeliverMessage.Report report = deliver.createReport();
        report.read(in);
        ReferenceCountUtil.release(in);

        consumer.apply(report);
        return;
      }

      consumer.apply(deliver);
      return;
    }

    ctx.fireChannelRead(msg);
  }
}
