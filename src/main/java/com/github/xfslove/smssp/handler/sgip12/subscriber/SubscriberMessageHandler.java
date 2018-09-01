package com.github.xfslove.smssp.handler.sgip12.subscriber;

import com.github.xfslove.smssp.subscriber.sgip12.dispatcher.MessageDispatcher;
import com.github.xfslove.smssp.message.SessionEvent;
import com.github.xfslove.smssp.message.sgip12.DeliverMessage;
import com.github.xfslove.smssp.message.sgip12.DeliverRespMessage;
import com.github.xfslove.smssp.message.sgip12.ReportMessage;
import com.github.xfslove.smssp.message.sgip12.ReportRespMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
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
public class SubscriberMessageHandler extends ChannelDuplexHandler {

  private MessageDispatcher dispatcher;

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  public SubscriberMessageHandler(MessageDispatcher dispatcher, LogLevel level) {
    this.dispatcher = dispatcher;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof DeliverMessage) {

      dispatcher.deliver((DeliverMessage) msg);

      DeliverRespMessage deliverResp = new DeliverRespMessage();
      deliverResp.setResult(0);

      ctx.writeAndFlush(deliverResp);
      return;
    }

    if (msg instanceof ReportMessage) {

      dispatcher.report((ReportMessage) msg);

      ReportRespMessage reportResp = new ReportRespMessage();
      reportResp.setResult(0);

      ctx.writeAndFlush(reportResp);
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    if (evt instanceof SessionEvent) {

      SessionEvent sEvt = (SessionEvent) evt;

      Object msg = sEvt.getMessage();

      if (msg instanceof DeliverMessage) {

        // 需要先登录
        DeliverRespMessage deliverResp = new DeliverRespMessage();
        deliverResp.setResult(1);

        ctx.writeAndFlush(deliverResp).addListener(listener -> {
          ctx.channel().close();
          logger.log(internalLevel, "discard[NOT_VALID] deliver message {}, channel closed", msg);
        });

        return;
      }

      if (msg instanceof ReportMessage) {

        // 需要先登录
        ReportRespMessage reportResp = new ReportRespMessage();
        reportResp.setResult(1);

        ctx.writeAndFlush(reportResp).addListener(listener -> {
          ctx.channel().close();
          logger.log(internalLevel, "discard[NOT_VALID] report message {}, channel closed", msg);
        });

        return;
      }
    }

    ctx.fireUserEventTriggered(evt);

  }
}
