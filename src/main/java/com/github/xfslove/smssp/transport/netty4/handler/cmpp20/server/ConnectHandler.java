package com.github.xfslove.smssp.transport.netty4.handler.cmpp20.server;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.ConnectMessage;
import com.github.xfslove.smssp.message.cmpp20.ConnectRespMessage;
import com.github.xfslove.smssp.transport.netty4.handler.SessionEvent;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * smg -> sp 链接session管理handler
 *
 * @author hanwen
 * created at 2018/9/2
 */
@ChannelHandler.Sharable
public class ConnectHandler extends ChannelDuplexHandler {

  private static final AttributeKey<Boolean> SESSION_VALID = AttributeKey.valueOf("sessionValid");

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private final String loginName;

  private final String loginPassword;

  public ConnectHandler(String loginName, String loginPassword) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // connect
    if (msg instanceof ConnectMessage) {
      ConnectMessage connect = (ConnectMessage) msg;

      ConnectRespMessage resp = new ConnectRespMessage(connect.getHead().getSequenceId());
      resp.setVersion(ConnectMessage.VERSION_20);


      byte[] sourceBytes = loginName.getBytes(StandardCharsets.ISO_8859_1);
      byte[] secretBytes = loginPassword.getBytes(StandardCharsets.ISO_8859_1);
      byte[] timestampBytes = StringUtils.leftPad(String.valueOf(connect.getTimestamp()), 10, "0").getBytes(StandardCharsets.ISO_8859_1);
      byte[] authenticatorSource = DigestUtils.md5(ByteUtil.concat(sourceBytes, new byte[9], secretBytes, timestampBytes));

      if (!connect.getSourceAddr().equals(loginName) ||
          !Arrays.equals(authenticatorSource, connect.getAuthenticatorSource())) {

        // 认证错误
        resp.setStatus(3);
        ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              logger.info("connect failure[result:{}] and close channel", 3);
              channel.close();
            }
          }
        });
        return;
      }

      if (connect.getVersion() != ConnectMessage.VERSION_20) {

        // 版本太高
        resp.setStatus(4);
        ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              logger.info("connect failure[result:{}] and close channel", 3);
              channel.close();
            }
          }
        });
        return;
      }

      // connect 成功
      ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
        @Override
        public void operationComplete(Future<? super Void> future) throws Exception {
          if (future.isSuccess()) {
            logger.info("connect success");
            channel.attr(SESSION_VALID).set(true);
          }
        }
      });

      return;
    }

    if (!Boolean.TRUE.equals(channel.attr(SESSION_VALID).get())) {
      // 没有注册 session 收到消息
      logger.info("received message when session not valid, fire SESSION_EVENT[NOT_VALID]", msg);

      ctx.fireUserEventTriggered(SessionEvent.NOT_VALID((Message) msg));
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    ctx.channel().attr(SESSION_VALID).set(null);

    ctx.fireChannelInactive();
  }
}
