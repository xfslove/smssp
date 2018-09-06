package com.github.xfslove.smssp.netty4.handler.sgip12.client;

import com.github.xfslove.smssp.client.ResponseConsumer;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author hanwen
 * created at 2018/9/1
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private ResponseConsumer consumer;

  private String loginName;

  public SubmitHandler(String loginName, ResponseConsumer consumer, LogLevel level) {
    this.loginName = loginName;
    this.consumer = consumer;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // submitResp
    if (msg instanceof SubmitRespMessage) {
      if (!consumer.apply((SubmitRespMessage) msg)) {
        logger.log(internalLevel, "{} drop received unrelated submit resp message {}", loginName, msg);
      }
      return;
    }

    ctx.fireChannelRead(msg);

  }
}
