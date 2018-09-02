package com.github.xfslove.smssp.netty4.handler.sgip12.sender;

import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.sgip12.MessageCodec;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.xfslove.smssp.netty4.handler.AttributeKeyConstants.RESP_QUEUE;

/**
 * @author hanwen
 * created at 2018/9/1
 */
@ChannelHandler.Sharable
public class SenderPoolMessageHandler extends ChannelDuplexHandler implements ChannelPoolHandler {

  private final LogLevel level = LogLevel.DEBUG;
  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private int idleTime = 5 * 60;

  private int windowSize = 32;

  private String loginName;

  private String loginPassword;

  public SenderPoolMessageHandler(String loginName, String loginPassword) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelReleased(Channel channel) throws Exception {
    // nothing
  }

  @Override
  public void channelAcquired(Channel channel) throws Exception {
    // nothing
  }

  @Override
  public void channelCreated(Channel channel) throws Exception {

    channel.pipeline().addLast("sgipSocketLogging", new LoggingHandler(LogLevel.DEBUG));
    channel.pipeline().addLast("sgipIdleState", new IdleStateHandler(0, 0, idleTime, TimeUnit.SECONDS));
    channel.pipeline().addLast("sgipMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("sgipMessageCodec", new MessageCodec());
    channel.pipeline().addLast("sgipMessageLogging", new LoggingHandler(level));
    channel.pipeline().addLast("sgipSessionHandler", new SenderSessionHandler(loginName, loginPassword, level));
    channel.pipeline().addLast("sgipMessageHandler", this);

    logger.log(internalLevel, "initialized sender pipeline[sgipSocketLogging, sgipIdleState, sgipMessageLengthCodec, sgipMessageCodec, sgipMessageLogging, sgipSessionHandler, sgipMessageHandler]");

    // 接受resp的queue
    Attribute<LinkedBlockingQueue> respQueue = channel.attr(RESP_QUEUE);
    if (respQueue.get() == null) {
      respQueue.set(new LinkedBlockingQueue<>(windowSize));
    }

    logger.log(internalLevel, "initialized sender req & resp queue");
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    // sender会接受submitResp
    if (msg instanceof SubmitRespMessage) {
      LinkedBlockingQueue respQueue = ctx.channel().attr(RESP_QUEUE).get();

      respQueue.offer(msg);
      logger.log(internalLevel, "current sender resp queue size:[{}]", respQueue.size());
      return;
    }

    logger.log(internalLevel, "received unknown sgip message {}, drop it", msg);
    ReferenceCountUtil.release(msg);
  }

  public void setIdleTime(int idleTime) {
    this.idleTime = idleTime;
  }

  public void setWindowSize(int windowSize) {
    this.windowSize = windowSize;
  }
}
