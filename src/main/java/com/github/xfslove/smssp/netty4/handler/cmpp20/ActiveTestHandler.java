package com.github.xfslove.smssp.netty4.handler.cmpp20;

import com.github.xfslove.smssp.message.cmpp20.ActiveTestMessage;
import com.github.xfslove.smssp.message.cmpp20.ActiveTestRespMessage;
import com.github.xfslove.smssp.message.cmpp20.TerminateMessage;
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
public class ActiveTestHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private String loginName;
  private boolean keepalive;

  public ActiveTestHandler(String loginName, boolean keepalive, LogLevel level) {
    this.loginName = loginName;
    this.keepalive = keepalive;

    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel channel = ctx.channel();

    // activeTest
    if (msg instanceof ActiveTestMessage) {
      channel.writeAndFlush(new ActiveTestRespMessage()).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.log(internalLevel, "{} received active test message", loginName);
          }
        }
      });

      return;
    }

    // activeTestResp
    if (msg instanceof ActiveTestRespMessage) {
      logger.log(internalLevel, "{} received active test resp message", loginName);
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

        if (keepalive) {
          // 发送active test
          ctx.channel().writeAndFlush(new ActiveTestMessage()).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
              if (future.isSuccess()) {
                logger.log(internalLevel, "{} request active test when idle", loginName);
              }
            }
          });

          return;
        }
      } else {

        ctx.channel().writeAndFlush(new TerminateMessage()).addListener(new GenericFutureListener<Future<? super Void>>() {
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
