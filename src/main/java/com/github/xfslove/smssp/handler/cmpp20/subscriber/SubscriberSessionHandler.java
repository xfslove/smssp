package com.github.xfslove.smssp.handler.cmpp20.subscriber;

import com.github.xfslove.smssp.message.SessionEvent;
import com.github.xfslove.smssp.message.cmpp20.*;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.channel.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.github.xfslove.smssp.handler.AttributeKeyConstants.SESSION_VALID;

/**
 * smg -> sp 链接session管理handler
 *
 * @author hanwen
 * created at 2018/9/2
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
    Channel channel = ctx.channel();

    // connect
    if (msg instanceof ConnectMessage) {
      ConnectRespMessage resp = new ConnectRespMessage();
      resp.setVersion(0x20);

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
      channel.writeAndFlush(resp).addListener(future -> {
        if (future.isSuccess()) {
          logger.log(internalLevel, "{} connect success", loginName);
          channel.attr(SESSION_VALID).set(true);
        }
      });

      return;
    }

    // terminate
    if (msg instanceof TerminateMessage) {
      // 直接回复resp
      channel.writeAndFlush(new TerminateRespMessage()).addListener(future -> {
        if (future.isSuccess()) {
          channel.close();
          logger.log(internalLevel, "{} terminate success and channel closed", loginName);
        }
      });

      return;
    }

    // terminateResp
    if (msg instanceof TerminateRespMessage) {
      logger.log(internalLevel, "{} received terminate resp message and close channel", loginName);
      channel.close();
      return;
    }

    // activeTest
    if (msg instanceof ActiveTestMessage) {
      channel.writeAndFlush(new ActiveTestRespMessage()).addListener(future -> {
        if (future.isSuccess()) {
          logger.log(internalLevel, "{} received active test message", loginName);
        }
      });

      return;
    }

    if (!Boolean.TRUE.equals(channel.attr(SESSION_VALID).get())) {
      // 没有注册 session 收到消息
      logger.log(internalLevel, "{} received message when session not valid, fire SESSION_EVENT[NOT_VALID]", loginName, msg);

      ctx.fireUserEventTriggered(SessionEvent.NOT_VALID(msg));
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
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    // 处理空闲链接
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent iEvt = (IdleStateEvent) evt;
      if (iEvt.state().equals(IdleState.ALL_IDLE)) {

        // 发送terminate
        ctx.channel().writeAndFlush(new TerminateMessage()).addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {

            ctx.executor().schedule(() -> {
              ctx.channel().close();
              logger.log(internalLevel, "{} channel closed due to not received resp", loginName);
            }, 500, TimeUnit.MILLISECONDS);

            logger.log(internalLevel, "{} request terminate when idle and delay 500ms close channel if no resp", loginName);
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

  private void connectRespError(ChannelHandlerContext ctx, int result) {
    ConnectRespMessage resp = new ConnectRespMessage();
    resp.setStatus(result);
    ctx.channel().writeAndFlush(resp).addListener(future -> {
      if (future.isSuccess()) {
        ctx.channel().close();
        logger.log(internalLevel, "{} connect failure[result:{}] and channel closed", loginName, result);
      }
    });
  }
}
