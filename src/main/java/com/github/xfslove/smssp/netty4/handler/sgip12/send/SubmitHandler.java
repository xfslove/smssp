package com.github.xfslove.smssp.netty4.handler.sgip12.send;

import com.github.xfslove.smssp.client.ResponseConsumer;
import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import com.github.xfslove.smssp.message.sgip12.SubmitMessage;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;

/**
 * @author hanwen
 * created at 2018/9/1
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private ResponseConsumer consumer;

  private int nodeId;

  private String loginName;

  private Sequence sequence;

  public SubmitHandler(int nodeId, String loginName, ResponseConsumer consumer, Sequence sequence, LogLevel level) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.consumer = consumer;
    this.sequence = sequence;
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

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    // submit
    if (msg instanceof SubmitMessage) {
      SubmitMessage submit = (SubmitMessage) msg;
      // rewrite sequenceNumber
      submit.getHead().setSequenceNumber(SequenceNumber.create(nodeId, Integer.valueOf(DateFormatUtils.format(new Date(), "MMddHHmmss")), sequence.next()));
    }

    ctx.write(msg, promise);

  }
}
