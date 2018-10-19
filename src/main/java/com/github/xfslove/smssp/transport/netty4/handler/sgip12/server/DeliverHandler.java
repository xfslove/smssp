package com.github.xfslove.smssp.transport.netty4.handler.sgip12.server;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.sgip12.DeliverMessage;
import com.github.xfslove.smssp.message.sgip12.DeliverRespMessage;
import com.github.xfslove.smssp.message.sgip12.ReportMessage;
import com.github.xfslove.smssp.message.sgip12.ReportRespMessage;
import com.github.xfslove.smssp.notification.NotificationListener;
import com.github.xfslove.smssp.transport.netty4.handler.SessionEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
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

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(DeliverHandler.class);

  private NotificationListener listener;

  public DeliverHandler(NotificationListener listener) {
    this.listener = listener;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof DeliverMessage) {
      DeliverMessage deliver = (DeliverMessage) msg;

      DeliverRespMessage deliverResp = new DeliverRespMessage(deliver.getHead().getSequenceNumber());
      deliverResp.setResult(0);

      ctx.writeAndFlush(deliverResp);

      listener.done(deliver);
      return;
    }

    if (msg instanceof ReportMessage) {
      ReportMessage report = (ReportMessage) msg;

      ReportRespMessage reportResp = new ReportRespMessage(report.getHead().getSequenceNumber());
      reportResp.setResult(0);

      ctx.writeAndFlush(reportResp);

      listener.done(report);
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
        DeliverRespMessage deliverResp = new DeliverRespMessage(deliver.getHead().getSequenceNumber());
        deliverResp.setResult(1);

        ctx.writeAndFlush(deliverResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> listener) throws Exception {
            LOGGER.warn("discard[NOT_VALID] deliver message {} and close channel", msg);
            ctx.channel().close();
          }
        });

        return;
      }

      if (msg instanceof ReportMessage) {
        ReportMessage report = (ReportMessage) msg;

        // 需要先登录
        ReportRespMessage reportResp = new ReportRespMessage(report.getHead().getSequenceNumber());
        reportResp.setResult(1);

        ctx.writeAndFlush(reportResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> listener) throws Exception {
            LOGGER.warn("discard[NOT_VALID] report message {} and close channel", msg);
            ctx.channel().close();
          }
        });

        return;
      }

    }

    ctx.fireUserEventTriggered(evt);

  }
}
