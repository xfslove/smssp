package com.github.xfslove.smssp.netty4.handler.sgip12.subscriber;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.message.sgip12.UnBindMessage;
import com.github.xfslove.smssp.message.sgip12.UnBindRespMessage;
import com.github.xfslove.smssp.netty4.handler.SessionEvent;
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

import static com.github.xfslove.smssp.netty4.handler.sgip12.AttributeKeyConstants.SESSION_VALID;

/**
 * smg -> sp 链接session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class SubscriberSessionHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private final String loginName;

  private final String loginPassword;

  public SubscriberSessionHandler(String loginName, String loginPassword, LogLevel level) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // bind 请求
    if (msg instanceof BindMessage) {
      BindRespMessage bindResp = new BindRespMessage();

      if (Boolean.TRUE.equals(ctx.channel().attr(SESSION_VALID).get())) {
        // 重复登录
        bindRespError(ctx, 2);
        return;
      }

      BindMessage bind = (BindMessage) msg;

      if (!loginName.equals(bind.getLoginName()) || !loginPassword.equals(bind.getLoginPassword())) {
        // 用户名密码错误
        bindRespError(ctx, 1);
        return;
      }

      if (2 != bind.getLoginType()) {
        // 登录类型不对
        bindRespError(ctx, 4);
        return;
      }

      // bind 成功
      channel.writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.log(internalLevel, "{} bind success", loginName);
            channel.attr(SESSION_VALID).set(true);
          }
        }
      });

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

    if (!Boolean.TRUE.equals(channel.attr(SESSION_VALID).get())) {
      // 没有注册 session 收到消息
      logger.log(internalLevel, "{} received message when session not valid, fire SESSION_EVENT[NOT_VALID]", loginName, msg);

      ctx.fireUserEventTriggered(SessionEvent.NOT_VALID((Message) msg));
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    ctx.channel().attr(SESSION_VALID).set(false);

    ctx.fireChannelInactive();
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

    if (!Boolean.TRUE.equals(ctx.channel().attr(SESSION_VALID).get())) {
      ctx.channel().close();
      logger.log(internalLevel, "{} catch exception when login and channel closed, {}", loginName, cause);

      return;
    }

    ctx.fireExceptionCaught(cause);
  }

  private void bindRespError(final ChannelHandlerContext ctx, final int result) {
    BindRespMessage bindResp = new BindRespMessage();
    bindResp.setResult(result);
    ctx.channel().writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
      @Override
      public void operationComplete(Future<? super Void> future) throws Exception {
        if (future.isSuccess()) {
          ctx.channel().close();
          logger.log(internalLevel, "{} bind failure[result:{}] and channel closed", loginName, result);
        }
      }
    });
  }
}
