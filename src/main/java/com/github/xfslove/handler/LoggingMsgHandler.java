package com.github.xfslove.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 消息打印logging handler
 *
 * @author hanwen
 * created at 2018/8/28
 */
@Sharable
public class LoggingMsgHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;

  private final InternalLogLevel internalLevel;

  public LoggingMsgHandler() {
    this(LogLevel.DEBUG);
  }

  public LoggingMsgHandler(LogLevel level) {
    if (level == null) {
      throw new NullPointerException("level");
    }
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, "Receive: {}", msg);
    }
    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (logger.isEnabled(internalLevel)) {
      logger.log(internalLevel, "Send: {}", msg);
    }
    ctx.write(msg, promise);
  }
}
