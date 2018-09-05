package com.github.xfslove.smssp.netty4.handler.cmpp20.send;

import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import com.github.xfslove.smssp.message.seq.SequenceGenerator;
import io.netty.channel.*;
import io.netty.handler.logging.LogLevel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private static final AttributeKey<Map<Integer, SubmitMessage>> SUBMIT_HOLDER = AttributeKey.valueOf("submitHolder");

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private String loginName;

  private int windowSize;

  private SequenceGenerator sequenceGenerator;

  private SubmitBiConsumer submitBiConsumer;

  public SubmitHandler(SequenceGenerator sequenceGenerator, SubmitBiConsumer submitBiConsumer, String loginName, int windowSize, LogLevel level) {
    this.sequenceGenerator = sequenceGenerator;
    this.loginName = loginName;
    this.windowSize = windowSize;

    this.submitBiConsumer = submitBiConsumer;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();

    // init msg holder
    Attribute<Map<Integer, SubmitMessage>> holder = channel.attr(SUBMIT_HOLDER);
    if (holder.get() == null) {
      holder.set(new HashMap<Integer, SubmitMessage>(windowSize));
    }

    logger.log(internalLevel, "{} initialized submit msg holder", loginName);
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // sender会接受submitResp
    if (msg instanceof SubmitRespMessage) {
      SubmitRespMessage resp = (SubmitRespMessage) msg;

      Map<Integer, SubmitMessage> msgHolder = ctx.channel().attr(SUBMIT_HOLDER).get();

      SubmitMessage submit = msgHolder.get((resp.getHead().getSequenceId()));
      if (submit == null) {
        // drop it
        logger.log(internalLevel, "{} drop received unrelated submit resp message {}", loginName, msg);
        ReferenceCountUtil.release(msg);
        return;
      }

      submitBiConsumer.apply(submit, resp);
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    // submit的时候把消息放到当前channel中
    if (msg instanceof SubmitMessage) {
      SubmitMessage submit = (SubmitMessage) msg;
      // rewrite sequenceId
      submit.getHead().setSequenceId(sequenceGenerator.next());

      Map<Integer, SubmitMessage> msgHolder = ctx.channel().attr(SUBMIT_HOLDER).get();

      if (msgHolder.size() == windowSize) {
        // drop it
        logger.log(internalLevel, "{} drop request message {}, ratio up to limitation:[{}]", loginName, msg, windowSize);
        ReferenceCountUtil.release(msg);
        return;
      }

      msgHolder.put(submit.getHead().getSequenceId(), submit);
      logger.log(internalLevel, "{} current send request queue size:[{}]", loginName, msgHolder.size());
    }

    ctx.write(msg, promise);
  }
}
