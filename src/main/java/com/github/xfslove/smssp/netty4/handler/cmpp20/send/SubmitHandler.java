package com.github.xfslove.smssp.netty4.handler.cmpp20.send;

import com.github.xfslove.smssp.client.ResponseConsumer;
import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;
import com.github.xfslove.smssp.message.sequence.Sequence;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class SubmitHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private ResponseConsumer consumer;

  private String loginName;

  private Sequence sequence;

  public SubmitHandler(String loginName, Sequence sequence, ResponseConsumer consumer, LogLevel level) {
    this.loginName = loginName;
    this.sequence = sequence;
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

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

    // submit
    if (msg instanceof SubmitMessage) {
      SubmitMessage submit = (SubmitMessage) msg;
      // rewrite sequenceId
      submit.getHead().setSequenceId(sequence.next());
    }

    ctx.write(msg, promise);
  }
}
