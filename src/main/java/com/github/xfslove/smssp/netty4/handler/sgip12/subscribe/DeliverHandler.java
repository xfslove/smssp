package com.github.xfslove.smssp.netty4.handler.sgip12.subscribe;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.sgip12.DeliverMessage;
import com.github.xfslove.smssp.message.sgip12.DeliverRespMessage;
import com.github.xfslove.smssp.netty4.handler.SessionEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
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

  private DeliverConsumer consumer;

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  public DeliverHandler(DeliverConsumer consumer, LogLevel level) {
    this.consumer = consumer;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof DeliverMessage) {
      DeliverMessage deliver = (DeliverMessage) msg;

      DeliverRespMessage deliverResp = new DeliverRespMessage();
      deliverResp.getHead().setSequenceNumber(deliver.getHead().getSequenceNumber());
      deliverResp.setResult(0);

      ctx.writeAndFlush(deliverResp);

      consumer.apply(deliver);
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
        DeliverRespMessage deliverResp = new DeliverRespMessage();
        deliverResp.getHead().setSequenceNumber(deliver.getHead().getSequenceNumber());
        deliverResp.setResult(1);

        ctx.writeAndFlush(deliverResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> listener) throws Exception {
            ctx.channel().close();
            logger.log(internalLevel, "discard[NOT_VALID] deliver message {}, channel closed", msg);
          }
        });

        return;
      }

    }

    ctx.fireUserEventTriggered(evt);

  }
}
