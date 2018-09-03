package com.github.xfslove.smssp.netty4.handler.cmpp20.sender;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.Attribute;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;

import static com.github.xfslove.smssp.netty4.handler.AttributeKeyConstants.RESP_QUEUE;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private String loginName;

  private int windowSize;

  public SubmitHandler(String loginName, int windowSize, LogLevel level) {
    this.loginName = loginName;
    this.windowSize = windowSize;

    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();

    // 接受resp的queue
    Attribute<LinkedBlockingQueue<Message>> respQueue = channel.attr(RESP_QUEUE);
    if (respQueue.get() == null) {
      respQueue.set(new LinkedBlockingQueue<Message>(windowSize));
    }

    logger.log(internalLevel, "{} initialized sender req & resp queue", loginName);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // sender会接受submitResp
    if (msg instanceof SubmitRespMessage) {
      LinkedBlockingQueue<Message> respQueue = ctx.channel().attr(RESP_QUEUE).get();

      respQueue.offer((SubmitRespMessage) msg);
      logger.log(internalLevel, "{} current sender resp queue size:[{}]", loginName, respQueue.size());
      return;
    }

    ctx.fireChannelRead(msg);
  }
}
