package com.github.xfslove.smssp.transport.netty4.handler.cmpp20.server;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.ConnectMessage;
import com.github.xfslove.smssp.message.cmpp20.ConnectRespMessage;
import com.github.xfslove.smssp.transport.netty4.handler.AttributeConstants;
import com.github.xfslove.smssp.transport.netty4.handler.SessionEvent;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
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

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(ConnectHandler.class);

  private final String username;

  private final String password;

  public ConnectHandler(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // connect
    if (msg instanceof ConnectMessage) {
      final ConnectMessage connect = (ConnectMessage) msg;

      ConnectRespMessage resp = new ConnectRespMessage(connect.getHead().getSequenceId());
      resp.setVersion(ConnectMessage.VERSION_2_0);


      byte[] sourceBytes = username.getBytes(StandardCharsets.ISO_8859_1);
      byte[] secretBytes = password.getBytes(StandardCharsets.ISO_8859_1);
      byte[] timestampBytes = StringUtils.leftPad(String.valueOf(connect.getTimestamp()), 10, "0").getBytes(StandardCharsets.ISO_8859_1);
      byte[] authenticatorSource = DigestUtils.md5(ByteUtil.concat(sourceBytes, new byte[9], secretBytes, timestampBytes));

      if (!connect.getSourceAddr().equals(username) ||
          !Arrays.equals(authenticatorSource, connect.getAuthenticatorSource())) {

        // 认证错误
        resp.setStatus(3);
        ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              LOGGER.info("connect failure[result:{}] and close channel", 3);
              channel.close();
            }
          }
        });
        return;
      }

      if (connect.getVersion() != ConnectMessage.VERSION_2_0) {

        // 版本太高
        resp.setStatus(4);
        ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              LOGGER.info("connect failure[result:{}] and close channel", 3);
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
            LOGGER.info("connect success");
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
