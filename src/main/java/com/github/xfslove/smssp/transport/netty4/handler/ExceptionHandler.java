package com.github.xfslove.smssp.transport.netty4.handler;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * sp -> smg session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class ExceptionHandler extends ChannelDuplexHandler {

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    logger.warn("catch exception message: {}, and close channel", cause.getMessage());
    ctx.channel().close();
  }
}