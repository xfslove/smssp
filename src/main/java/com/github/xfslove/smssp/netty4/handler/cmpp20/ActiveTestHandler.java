package com.github.xfslove.smssp.netty4.handler.cmpp20;

import com.github.xfslove.smssp.message.cmpp20.ActiveTestMessage;
import com.github.xfslove.smssp.message.cmpp20.ActiveTestRespMessage;
import com.github.xfslove.smssp.message.seq.SequenceGenerator;
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

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class ActiveTestHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private SequenceGenerator sequenceGenerator;

  private String loginName;
  private boolean keepAlive;

  public ActiveTestHandler(String loginName, SequenceGenerator sequenceGenerator, boolean keepAlive, LogLevel level) {
    this.sequenceGenerator = sequenceGenerator;
    this.loginName = loginName;
    this.keepAlive = keepAlive;

    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel channel = ctx.channel();

    // activeTest
    if (msg instanceof ActiveTestMessage) {
      ActiveTestMessage active = (ActiveTestMessage) msg;

      ActiveTestRespMessage resp = new ActiveTestRespMessage();
      resp.getHead().setSequenceId(active.getHead().getSequenceId());


      channel.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
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

        if (keepAlive) {
          // 发送active test
          ActiveTestMessage active = new ActiveTestMessage();
          active.getHead().setSequenceId(sequenceGenerator.next());

          ctx.channel().writeAndFlush(active).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
              if (future.isSuccess()) {
                logger.log(internalLevel, "{} request active test when idle", loginName);
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
