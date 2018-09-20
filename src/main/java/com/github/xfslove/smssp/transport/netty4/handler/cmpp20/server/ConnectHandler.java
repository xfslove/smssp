package com.github.xfslove.smssp.transport.netty4.handler.cmpp20.server;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.ConnectMessage;
import com.github.xfslove.smssp.message.cmpp20.ConnectRespMessage;
import com.github.xfslove.smssp.transport.netty4.handler.AttributeConstant;
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

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private final String name;

  private final String password;

  public ConnectHandler(String name, String password) {
    this.name = name;
    this.password = password;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // connect
    if (msg instanceof ConnectMessage) {
      final ConnectMessage connect = (ConnectMessage) msg;

      ConnectRespMessage resp = new ConnectRespMessage(connect.getHead().getSequenceId());
      resp.setVersion(ConnectMessage.VERSION_20);


      byte[] sourceBytes = name.getBytes(StandardCharsets.ISO_8859_1);
      byte[] secretBytes = password.getBytes(StandardCharsets.ISO_8859_1);
      byte[] timestampBytes = StringUtils.leftPad(String.valueOf(connect.getTimestamp()), 10, "0").getBytes(StandardCharsets.ISO_8859_1);
      byte[] authenticatorSource = DigestUtils.md5(ByteUtil.concat(sourceBytes, new byte[9], secretBytes, timestampBytes));

      if (!connect.getSourceAddr().equals(name) ||
          !Arrays.equals(authenticatorSource, connect.getAuthenticatorSource())) {

        // 认证错误
        resp.setStatus(3);
        ctx.writeAndFlush(resp).addListener(new GenericFutureListener<Future<? super Void>>() {
          @Override
          public void operationComplete(Future<? super Void> future) throws Exception {
            if (future.isSuccess()) {
              logger.info("{} connect failure[result:{}] and close channel", connect.getSourceAddr(), 3);
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
              logger.info("{} connect failure[result:{}] and close channel", connect.getSourceAddr(), 3);
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
            logger.info("{} connect success", connect.getSourceAddr());
            channel.attr(AttributeConstant.SESSION).set(true);
            channel.attr(AttributeConstant.NAME).set(connect.getSourceAddr());
          }
        }
      });

      return;
    }

    if (!Boolean.TRUE.equals(channel.attr(AttributeConstant.SESSION).get())) {
      // 没有注册 session 收到消息
      channel.attr(AttributeConstant.NAME).set(name);
      logger.info("{} received message when session not valid, fire SESSION_EVENT[NOT_VALID]", name, msg);

      ctx.fireUserEventTriggered(SessionEvent.NOT_VALID((Message) msg));
      return;
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    ctx.channel().attr(AttributeConstant.SESSION).set(null);

    ctx.fireChannelInactive();
  }
}
