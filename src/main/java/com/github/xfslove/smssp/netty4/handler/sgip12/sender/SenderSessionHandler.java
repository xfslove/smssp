package com.github.xfslove.smssp.netty4.handler.sgip12.sender;

import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.message.sgip12.UnBindMessage;
import com.github.xfslove.smssp.message.sgip12.UnBindRespMessage;
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
 * sp -> smg session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class SenderSessionHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private String loginName;

  private String loginPassword;

  public SenderSessionHandler(String loginName, String loginPassword, LogLevel level) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 发送bind请求
    BindMessage bind = new BindMessage();
    bind.setLoginName(loginName);
    bind.setLoginPassword(loginPassword);
    ctx.channel().writeAndFlush(bind);
    logger.log(internalLevel, "{} send bind request", loginName);

    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // 获取bindResp
    if (msg instanceof BindRespMessage) {
      BindRespMessage bindResp = (BindRespMessage) msg;

      int result = bindResp.getResult();

      if (result == 0) {
        // bind 成功
        logger.log(internalLevel, "{} bind success", loginName);

      } else {

        channel.close();
        logger.log(internalLevel, "{} bind failure[result:{}]", loginName, result);
      }

      return;
    }

    // unbind
    if (msg instanceof UnBindMessage) {
      // 直接回复UnbindResp
      channel.writeAndFlush(new UnBindRespMessage()).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            channel.close();
            logger.log(internalLevel, "{} unbind success and channel closed", loginName);
          }
        }
      });

      return;
    }

    // unbindResp
    if (msg instanceof UnBindRespMessage) {
      logger.log(internalLevel, "{} received unbind resp message and close channel", loginName);
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
        ctx.channel().writeAndFlush(new UnBindMessage()).addListener(new GenericFutureListener<Future<? super Void>>() {
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

              logger.log(internalLevel, "{} request unbind when idle and delay 500ms close channel if no resp", loginName);
            }
          }
        });

        return;
      }
    }

    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    ctx.channel().close();
    logger.log(internalLevel, "{} catch exception and channel closed, {}", loginName, cause);
  }
}
