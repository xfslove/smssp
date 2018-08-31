package com.github.xfslove.smssp.netty.handler.sgip12;

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
import io.netty.util.AttributeKey;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * sp -> smg session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class ClientSessionHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private AttributeKey<Boolean> sessionValid = AttributeKey.valueOf("sessionValid");

  private final String loginName;

  private final String loginPassword;

  public ClientSessionHandler(String loginName, String loginPassword, LogLevel level) {
    if (level == null) {
      level = LogLevel.DEBUG;
    }
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 判断登录状态
    if (Boolean.TRUE.equals(ctx.channel().attr(sessionValid).get())) {

      ctx.fireChannelActive();
      return;
    }

    logger.log(internalLevel, "{} send bind request prepare login", loginName);
    // 发送bind请求
    BindMessage bind = new BindMessage();
    bind.setLoginName(loginName);
    bind.setLoginPassword(loginPassword);

    ctx.channel().writeAndFlush(bind);

    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel channel = ctx.channel();

    // 获取bindResp
    if (msg instanceof BindRespMessage) {
      BindRespMessage bindResp = (BindRespMessage) msg;

      int result = bindResp.getResult();

      if (result == 0) {
        // bind 成功
        channel.attr(sessionValid).set(true);
        ctx.fireChannelRead(msg);
        logger.log(internalLevel, "{} bind success", loginName);
      } else {

        ctx.close();
        logger.log(internalLevel, "{} bind failure[result:{}]", loginName, result);
      }

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

}
