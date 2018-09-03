package com.github.xfslove.smssp.netty4.handler.cmpp20.subscriber;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.ConnectMessage;
import com.github.xfslove.smssp.message.cmpp20.ConnectRespMessage;
import com.github.xfslove.smssp.netty4.SessionEvent;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.github.xfslove.smssp.netty4.handler.AttributeKeyConstants.SESSION_VALID;

/**
 * smg -> sp 链接session管理handler
 *
 * @author hanwen
 * created at 2018/9/2
 */
@ChannelHandler.Sharable
public class ConnectHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private final String loginName;

  private final String loginPassword;

  public ConnectHandler(String loginName, String loginPassword, LogLevel level) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // connect
    if (msg instanceof ConnectMessage) {
      ConnectRespMessage resp = new ConnectRespMessage();
      resp.setVersion(ConnectMessage.VERSION_20);

      ConnectMessage connect = (ConnectMessage) msg;

      byte[] sourceBytes = loginName.getBytes(StandardCharsets.ISO_8859_1);
      byte[] secretBytes = loginPassword.getBytes(StandardCharsets.ISO_8859_1);
      byte[] timestampBytes = StringUtils.leftPad(String.valueOf(connect.getTimestamp()), 10, "0").getBytes(StandardCharsets.ISO_8859_1);
      byte[] authenticatorSource = DigestUtils.md5(ByteUtil.concat(sourceBytes, new byte[9], secretBytes, timestampBytes));

      if (!connect.getSourceAddr().equals(loginName) ||
          !Arrays.equals(authenticatorSource, connect.getAuthenticatorSource())) {

        // 认证错误
        connectRespError(ctx, 3);
        return;
      }

      if (connect.getVersion() != ConnectMessage.VERSION_20) {

        // 版本太高
        connectRespError(ctx, 4);
        return;
      }

      // connect 成功
      channel.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.log(internalLevel, "{} connect success", loginName);
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

  private void connectRespError(ChannelHandlerContext ctx, final int result) {
    ConnectRespMessage resp = new ConnectRespMessage();
    resp.setStatus(result);
    final Channel channel = ctx.channel();
    channel.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
      @Override
      public void operationComplete(Future<? super Void> future) throws Exception {
        if (future.isSuccess()) {
          channel.close();
          logger.log(internalLevel, "{} connect failure[result:{}] and channel closed", loginName, result);
        }
      }
    });
  }
}
