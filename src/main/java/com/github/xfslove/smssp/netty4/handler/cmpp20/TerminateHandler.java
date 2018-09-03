package com.github.xfslove.smssp.netty4.handler.cmpp20;

import com.github.xfslove.smssp.message.cmpp20.TerminateMessage;
import com.github.xfslove.smssp.message.cmpp20.TerminateRespMessage;
import io.netty.channel.Channel;
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
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class TerminateHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private String loginName;

  public TerminateHandler(String loginName, LogLevel level) {
    this.loginName = loginName;

    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    if (msg instanceof TerminateRespMessage) {
      logger.log(internalLevel, "{} received terminate resp message and close channel", loginName);
      channel.close();
      return;
    }

    // terminate
    if (msg instanceof TerminateMessage) {
      // 直接回复UnbindResp
      channel.writeAndFlush(new TerminateRespMessage()).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.log(internalLevel, "{} terminate success and close channel", loginName);
            channel.close();
          }
        }
      });

      return;
    }

    ctx.fireChannelRead(msg);
  }
}
