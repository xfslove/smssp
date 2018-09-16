package com.github.xfslove.smssp.netty4.handler.cmpp20;

import com.github.xfslove.smssp.message.cmpp20.TerminateMessage;
import com.github.xfslove.smssp.message.cmpp20.TerminateRespMessage;
import com.github.xfslove.smssp.message.Sequence;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class TerminateHandler extends ChannelDuplexHandler {

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private Sequence sequence;

  public TerminateHandler(Sequence sequence) {
    this.sequence = sequence;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    if (msg instanceof TerminateRespMessage) {
      logger.info("received terminate resp message and close channel");
      channel.close();
      return;
    }

    // terminate
    if (msg instanceof TerminateMessage) {
      TerminateMessage terminate = (TerminateMessage) msg;
      TerminateRespMessage resp = new TerminateRespMessage(terminate.getHead().getSequenceId());

      // 直接回复UnbindResp
      ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.info("terminate success and close channel");
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

        TerminateMessage terminate = new TerminateMessage(sequence);

        ctx.writeAndFlush(terminate).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              ctx.executor().schedule(new Runnable() {
                @Override
                public void run() {
                  if (ctx.channel().isActive()) {
                    logger.info("close channel due to not received resp message");
                    ctx.channel().close();
                  }
                }
              }, 500, TimeUnit.MILLISECONDS);

              logger.info("request terminate when idle and delay 500ms close channel if no resp message");
            }
          }
        });

        return;
      }
    }

    ctx.fireUserEventTriggered(evt);
  }
}
