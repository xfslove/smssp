package com.github.xfslove.smssp.netty4.handler.cmpp20.server;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;
import com.github.xfslove.smssp.message.cmpp20.DeliverRespMessage;
import com.github.xfslove.smssp.netty4.handler.SessionEvent;
import com.github.xfslove.smssp.server.NotificationListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;


/**
 * smg -&gt; sp 消息的handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class DeliverHandler extends ChannelDuplexHandler {

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private NotificationListener consumer;

  public DeliverHandler(NotificationListener consumer) {
    this.consumer = consumer;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof DeliverMessage) {

      DeliverMessage deliver = (DeliverMessage) msg;

      DeliverRespMessage deliverResp = new DeliverRespMessage(deliver.getHead().getSequenceId());
      deliverResp.setMsgId(deliver.getMsgId());
      deliverResp.setResult(0);

      ctx.writeAndFlush(deliverResp);

      if (deliver.getRegisteredDelivery() == 1) {
        // 状态报告
        ByteBuf in = Unpooled.wrappedBuffer(deliver.getUdBytes());
        DeliverMessage.Report report = deliver.createReport();
        report.read(in);
        in.release();

        consumer.done(report);
        return;
      }

      consumer.done(deliver);
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {

    if (evt instanceof SessionEvent) {

      SessionEvent sEvt = (SessionEvent) evt;

      final Message msg = sEvt.getMessage();

      if (msg instanceof DeliverMessage) {
        DeliverMessage deliver = (DeliverMessage) msg;

        // 需要先登录
        DeliverRespMessage deliverResp = new DeliverRespMessage(deliver.getHead().getSequenceId());
        deliverResp.setMsgId(deliver.getMsgId());
        deliverResp.setResult(9);

        ctx.writeAndFlush(deliverResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> listener) throws Exception {
            logger.warn("discard[NOT_VALID] deliver message {} and close channel", msg);
            ctx.channel().close();
          }
        });

        return;
      }

    }
    ctx.fireUserEventTriggered(evt);

  }
}
