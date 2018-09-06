package com.github.xfslove.smssp.netty4.handler.sgip12.server;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.netty4.handler.SessionEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * smg -> sp 链接session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class BindHandler extends ChannelDuplexHandler {

  private static final AttributeKey<Boolean> SESSION_VALID = AttributeKey.valueOf("sessionValid");

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private final String loginName;

  private final String loginPassword;

  public BindHandler(String loginName, String loginPassword, LogLevel level) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // bind 请求
    if (msg instanceof BindMessage) {
      BindMessage bind = (BindMessage) msg;

      BindRespMessage bindResp = new BindRespMessage(bind.getHead().getSequenceNumber());

      if (Boolean.TRUE.equals(ctx.channel().attr(SESSION_VALID).get())) {
        // 重复登录
        bindResp.setResult(2);
        ctx.channel().writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              ctx.channel().close();
              logger.log(internalLevel, "{} bind failure[result:{}] and channel closed", loginName, 2);
            }
          }
        });
        return;
      }


      if (!loginName.equals(bind.getLoginName()) || !loginPassword.equals(bind.getLoginPassword())) {
        // 用户名密码错误
        bindResp.setResult(1);
        ctx.channel().writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              ctx.channel().close();
              logger.log(internalLevel, "{} bind failure[result:{}] and channel closed", loginName, 1);
            }
          }
        });
        return;
      }

      if (2 != bind.getLoginType()) {
        // 登录类型不对
        bindResp.setResult(4);
        ctx.channel().writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              ctx.channel().close();
              logger.log(internalLevel, "{} bind failure[result:{}] and channel closed", loginName, 4);
            }
          }
        });
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
}