package com.github.xfslove.smssp.transport.netty4.handler.cmpp20;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.cmpp20.ActiveTestMessage;
import com.github.xfslove.smssp.message.cmpp20.ActiveTestRespMessage;
import com.github.xfslove.smssp.transport.netty4.handler.AttributeConstant;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class ActiveTestHandler extends ChannelDuplexHandler {

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private Sequence<Integer> sequence;

  private boolean keepAlive;

  public ActiveTestHandler(Sequence<Integer> sequence, boolean keepAlive) {
    this.sequence = sequence;
    this.keepAlive = keepAlive;
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    // activeTest
    if (msg instanceof ActiveTestMessage) {
      ActiveTestMessage active = (ActiveTestMessage) msg;

      ActiveTestRespMessage resp = new ActiveTestRespMessage(active.getHead().getSequenceId());

      ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            String name = ctx.channel().attr(AttributeConstant.NAME).get();
            logger.info("{} received active test message", name);
          }
        }
      });

      return;
    }

    // activeTestResp
    if (msg instanceof ActiveTestRespMessage) {
      String name = ctx.channel().attr(AttributeConstant.NAME).get();
      logger.info("{} received active test resp message", name);
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

        if (keepAlive) {
          // 发送active test
          ActiveTestMessage active = new ActiveTestMessage(sequence);

          ctx.writeAndFlush(active).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
              if (future.isSuccess()) {
                String name = ctx.channel().attr(AttributeConstant.NAME).get();
                logger.info("{} request active test when idle", name);
              }
            }
          });

          return;
        }
      }

      ctx.fireUserEventTriggered(evt);
    }
  }
}
