package com.github.xfslove.smssp.netty.handler.sgip12;

import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.message.sgip12.UnBindMessage;
import com.github.xfslove.smssp.message.sgip12.UnBindRespMessage;
import com.github.xfslove.smssp.netty.SessionEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
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
public class ServerSessionHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private AttributeKey<Boolean> sessionValid = AttributeKey.valueOf("sessionValid");

  private final String loginName;

  private final String loginPassword;

  public ServerSessionHandler(String loginName, String loginPassword, LogLevel level) {
    if (level == null) {
      level = LogLevel.DEBUG;
    }
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel channel = ctx.channel();

    // bind 请求
    if (msg instanceof BindMessage) {
      BindRespMessage bindResp = new BindRespMessage();

      if (Boolean.TRUE.equals(ctx.channel().attr(sessionValid).get())) {
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
      channel.writeAndFlush(bindResp).addListener(future -> {
        if (future.isSuccess()) {
          logger.log(internalLevel, "{} bind success", loginName);
          channel.attr(sessionValid).set(true);
        }
      });

      return;
    }

    // unbind
    if (msg instanceof UnBindMessage) {
      // 直接回复UnbindResp
      channel.writeAndFlush(new UnBindRespMessage()).addListener(future -> {
        if (future.isSuccess()) {
          ctx.close();
          logger.log(internalLevel, "{} unbind success and channel closed", loginName);
        }
      });

      return;
    }

    if (!Boolean.TRUE.equals(channel.attr(sessionValid).get())) {
      // 没有注册 session 收到消息
      logger.log(internalLevel, "{} received message when session not valid, fire SESSION_EVENT[NOT_VALID]", loginName, msg);

      ctx.fireUserEventTriggered(SessionEvent.NOT_VALID(msg));
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    ctx.channel().attr(sessionValid).set(false);

    ctx.fireChannelInactive();
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    // 处理空闲链接
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent iEvt = (IdleStateEvent) evt;
      if (iEvt.state().equals(IdleState.ALL_IDLE)) {

        // 发送unbind
        ctx.channel().writeAndFlush(new UnBindMessage()).addListener(future -> {
          if (future.isSuccess()) {
            ctx.close();
            logger.log(internalLevel, "{} request unbind when idle and channel closed", loginName);
          }
        });

        return;
      }
    }

    ctx.fireUserEventTriggered(evt);
  }

  private void bindRespError(ChannelHandlerContext ctx, int result) {
    BindRespMessage bindResp = new BindRespMessage();
    bindResp.setResult(result);
    ctx.channel().writeAndFlush(bindResp).addListener(future -> {
      if (future.isSuccess()) {
        ctx.close();
        logger.log(internalLevel, "{} bind failure[result:{}] and channel closed", loginName, result);
      }
    });
  }
}
