package com.github.xfslove.smssp.netty4.handler.cmpp20;

import com.github.xfslove.smssp.message.cmpp20.TerminateMessage;
import com.github.xfslove.smssp.message.cmpp20.TerminateRespMessage;
import com.github.xfslove.smssp.message.sequence.Sequence;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class TerminateHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private Sequence sequence;
  private String loginName;

  public TerminateHandler(String loginName, Sequence sequence, LogLevel level) {
    this.sequence = sequence;
    this.loginName = loginName;

    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    if (msg instanceof TerminateRespMessage) {
      logger.log(internalLevel, "{} received terminate resp message and close channel", loginName);
      channel.close();
      return;
    }

    // terminate
    if (msg instanceof TerminateMessage) {
      TerminateMessage terminate = (TerminateMessage) msg;
      TerminateRespMessage resp = new TerminateRespMessage();
      resp.getHead().setSequenceId(terminate.getHead().getSequenceId());

      // 直接回复UnbindResp
      channel.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.log(internalLevel, "{} terminate success and close channel", loginName);
            channel.close();
          }
        }
      });

      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {

    // 处理空闲链接
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent iEvt = (IdleStateEvent) evt;
      if (iEvt.state().equals(IdleState.ALL_IDLE)) {

        TerminateMessage terminate = new TerminateMessage();
        terminate.getHead().setSequenceId(sequence.next());

        ctx.channel().writeAndFlush(terminate).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              ctx.executor().schedule(new Runnable() {
                @Override
                public void run() {
                  ctx.channel().close();
                  logger.log(internalLevel, "{} channel closed due to not received resp", loginName);
                }
              }, 500, TimeUnit.MILLISECONDS);

              logger.log(internalLevel, "{} request terminate when idle and delay 500ms close channel if no resp", loginName);
            }
          }
        });

        return;
      }
    }

    ctx.fireUserEventTriggered(evt);
  }
}
