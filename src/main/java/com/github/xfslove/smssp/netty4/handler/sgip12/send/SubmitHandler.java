package com.github.xfslove.smssp.netty4.handler.sgip12.send;

import com.github.xfslove.smssp.message.sequence.SequenceGenerator;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import com.github.xfslove.smssp.message.sgip12.SubmitMessage;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.channel.*;
import io.netty.handler.logging.LogLevel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author hanwen
 * created at 2018/9/1
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private static final AttributeKey<Map<SequenceNumber, SubmitMessage>> SUBMIT_HOLDER = AttributeKey.valueOf("submitHolder");

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private int nodeId;

  private String loginName;

  private SubmitBiConsumer biConsumer;

  private SequenceGenerator sequenceGenerator;

  public SubmitHandler(int nodeId, String loginName, SubmitBiConsumer biConsumer, SequenceGenerator sequenceGenerator, LogLevel level) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.biConsumer = biConsumer;
    this.sequenceGenerator = sequenceGenerator;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    Channel channel = ctx.channel();

    // init msg holder
    Attribute<Map<SequenceNumber, SubmitMessage>> holder = channel.attr(SUBMIT_HOLDER);
    if (holder.get() == null) {
      holder.set(new HashMap<SequenceNumber, SubmitMessage>(16));
    }

    logger.log(internalLevel, "{} initialized submit msg holder", loginName);

    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // submitResp
    if (msg instanceof SubmitRespMessage) {
      SubmitRespMessage resp = (SubmitRespMessage) msg;

      Map<SequenceNumber, SubmitMessage> msgHolder = ctx.channel().attr(SUBMIT_HOLDER).get();

      SubmitMessage submit = msgHolder.get((resp.getHead().getSequenceNumber()));

      biConsumer.apply(submit, resp);
      return;
    }

    ctx.fireChannelRead(msg);

  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    // submit的时候把消息放到当前channel中
    if (msg instanceof SubmitMessage) {
      SubmitMessage submit = (SubmitMessage) msg;
      // rewrite sequenceNumber
      submit.getHead().setSequenceNumber(SequenceNumber.create(nodeId, Integer.valueOf(DateFormatUtils.format(new Date(), "MMddHHmmss")), sequenceGenerator.next()));

      Map<SequenceNumber, SubmitMessage> msgHolder = ctx.channel().attr(SUBMIT_HOLDER).get();

      msgHolder.put(submit.getHead().getSequenceNumber(), submit);
      logger.log(internalLevel, "{} current send request queue size:[{}]", loginName, msgHolder.size());
    }

    ctx.write(msg, promise);

  }
}
