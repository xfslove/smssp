package com.github.xfslove.smssp.netty4.handler.cmpp20.send;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import io.netty.channel.*;
import io.netty.handler.logging.LogLevel;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.xfslove.smssp.netty4.handler.AttributeKeyConstants.MSG_QUEUE;

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

  private SubmitBiConsumer submitBiConsumer;

  public SubmitHandler(String loginName, SubmitBiConsumer submitBiConsumer, int windowSize, LogLevel level) {
    this.loginName = loginName;
    this.windowSize = windowSize;

    this.submitBiConsumer = submitBiConsumer;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();

    // 接受resp的queue
    Attribute<LinkedBlockingQueue<Message>> respQueue = channel.attr(MSG_QUEUE);
    if (respQueue.get() == null) {
      respQueue.set(new LinkedBlockingQueue<Message>(windowSize));
    }

    logger.log(internalLevel, "{} initialized send req & resp queue", loginName);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // sender会接受submitResp
    if (msg instanceof SubmitRespMessage) {
      LinkedBlockingQueue<Message> msgQueue = ctx.channel().attr(MSG_QUEUE).get();

      // blocking poll
      SubmitMessage submit = (SubmitMessage) msgQueue.poll(1, TimeUnit.SECONDS);
      if (submit == null) {
        // drop it
        logger.log(internalLevel, "{} drop received unrelated submit resp message {}", loginName, msg);
        ReferenceCountUtil.release(msg);
        return;
      }

      submitBiConsumer.apply(submit, (SubmitRespMessage) msg);
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    // submit的时候把消息放到当前channel的queue
    if (msg instanceof SubmitMessage) {
      LinkedBlockingQueue<Message> msgQueue = ctx.channel().attr(MSG_QUEUE).get();

      // blocking offer
      boolean offer = msgQueue.offer((SubmitMessage) msg, 1, TimeUnit.SECONDS);
      if (!offer) {
        // drop it
        logger.log(internalLevel, "{} drop request message {}, ratio up to limitation:[{}]", loginName, msg, windowSize);
        ReferenceCountUtil.release(msg);
        return;
      }

      logger.log(internalLevel, "{} current send request queue size:[{}]", loginName, msgQueue.size());
    }

    ctx.write(msg, promise);
  }
}
