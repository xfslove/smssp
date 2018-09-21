package com.github.xfslove.smssp.transport.netty4.handler.sgip12;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import com.github.xfslove.smssp.message.sgip12.UnBindMessage;
import com.github.xfslove.smssp.message.sgip12.UnBindRespMessage;
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
 * sp -> smg session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class UnBindHandler extends ChannelDuplexHandler {

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private Sequence<SequenceNumber> sequence;

  public UnBindHandler(Sequence<SequenceNumber> sequence) {
    this.sequence = sequence;
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // unbind
    if (msg instanceof UnBindMessage) {
      UnBindMessage unbind = (UnBindMessage) msg;

      // 直接回复UnbindResp
      UnBindRespMessage resp = new UnBindRespMessage(unbind.getHead().getSequenceNumber());

      ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.info("unbind success and close channel");
            channel.close();
          }
        }
      });

      return;
    }

    // unbindResp
    if (msg instanceof UnBindRespMessage) {
      logger.info("received unbind resp message and close channel");
      channel.close();
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

        // 发送unbind
        UnBindMessage unbind = new UnBindMessage(sequence);

        ctx.writeAndFlush(unbind).addListener(new GenericFutureListener<Future<? super Void>>() {
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

              logger.info("request unbind when idle and delay 500ms close channel if no resp message");
            }
          }
        });

        return;
      }
    }

    ctx.fireUserEventTriggered(evt);
  }
}
