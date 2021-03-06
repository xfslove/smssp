package com.github.xfslove.smssp.transport.netty4.handler.sgip12.server;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.transport.netty4.handler.AttributeConstants;
import com.github.xfslove.smssp.transport.netty4.handler.SessionEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * smg -&gt; sp 链接session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class BindHandler extends ChannelDuplexHandler {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(BindHandler.class);

  private final String username;

  private final String password;

  public BindHandler(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // bind 请求
    if (msg instanceof BindMessage) {
      final BindMessage bind = (BindMessage) msg;

      BindRespMessage bindResp = new BindRespMessage(bind.getHead().getSequenceNumber());

      if (Boolean.TRUE.equals(ctx.channel().attr(AttributeConstants.SESSION).get())) {
        // 重复登录
        bindResp.setResult(2);
        ctx.writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              LOGGER.info("bind failure[result:{}] and close channel", 2);
              ctx.channel().close();
            }
          }
        });
        return;
      }


      if (!username.equals(bind.getLoginName()) || !password.equals(bind.getLoginPassword())) {
        // 用户名密码错误
        bindResp.setResult(1);
        ctx.writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              LOGGER.info("bind failure[result:{}] and close channel", 1);
              ctx.channel().close();
            }
          }
        });
        return;
      }

      if (2 != bind.getLoginType()) {
        // 登录类型不对
        bindResp.setResult(4);
        ctx.writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              LOGGER.info("bind failure[result:{}] and close channel", 4);
              ctx.channel().close();
            }
          }
        });
        return;
      }

      // bind 成功
      ctx.writeAndFlush(bindResp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            LOGGER.info("bind success");
            channel.attr(AttributeConstants.SESSION).set(true);
          }
        }
      });

      return;
    }

    if (!Boolean.TRUE.equals(channel.attr(AttributeConstants.SESSION).get())) {
      // 没有注册 session 收到消息
      LOGGER.info("received message when session not valid, fire SESSION_EVENT[NOT_VALID]", msg);

      ctx.fireUserEventTriggered(SessionEvent.NOT_VALID((Message) msg));
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    ctx.channel().attr(AttributeConstants.SESSION).set(null);

    ctx.fireChannelInactive();
  }
}
