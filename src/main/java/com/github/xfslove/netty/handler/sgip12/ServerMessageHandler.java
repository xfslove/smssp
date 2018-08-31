package com.github.xfslove.netty.handler.sgip12;

import com.github.xfslove.message.sgip12.*;
import com.github.xfslove.netty.SessionEvent;
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
public class ServerMessageHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  public ServerMessageHandler(LogLevel level) {
    if (level == null) {
      level = LogLevel.DEBUG;
    }
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof DeliverMessage) {

      DeliverRespMessage deliverResp = new DeliverRespMessage();
      deliverResp.setResult(0);

      ctx.writeAndFlush(deliverResp);
    }

    if (msg instanceof ReportMessage) {

      ReportRespMessage reportResp = new ReportRespMessage();
      reportResp.setResult(0);

      ctx.writeAndFlush(reportResp);
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    if (evt instanceof SessionEvent) {

      SessionEvent sEvt = (SessionEvent) evt;

      SgipMessage msg = sEvt.getMessage();

      if (msg instanceof DeliverMessage) {

        // 需要先登录
        DeliverRespMessage deliverResp = new DeliverRespMessage();
        deliverResp.setResult(1);

        ctx.writeAndFlush(deliverResp).addListener(listener -> {
          ctx.close();
          logger.log(internalLevel, "discard[NOT_VALID] deliver message {}, channel closed", msg);
        });
        return;
      }

      if (msg instanceof ReportMessage) {

        // 需要先登录
        ReportRespMessage reportResp = new ReportRespMessage();
        reportResp.setResult(1);

        ctx.writeAndFlush(reportResp).addListener(listener -> {
          ctx.close();
          logger.log(internalLevel, "discard[NOT_VALID] report message {}, channel closed", msg);
        });
      }
    }

    ctx.fireUserEventTriggered(evt);

  }
}
