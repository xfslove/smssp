package com.github.xfslove.smssp.netty4.handler.cmpp20.sender;

import com.github.xfslove.smssp.message.cmpp20.*;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * sp -> smg session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class SenderSessionHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private String loginName;

  private String loginPassword;

  public SenderSessionHandler(String loginName, String loginPassword, LogLevel level) {
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 发送connect请求
    ConnectMessage connect = new ConnectMessage();

    connect.setTimestamp(Integer.valueOf(DateFormatUtils.format(new Date(), "MMddHHmmss")));
    connect.setSourceAddr(loginName);
    byte[] sourceBytes = connect.getSourceAddr().getBytes(StandardCharsets.ISO_8859_1);
    byte[] secretBytes = loginPassword.getBytes(StandardCharsets.ISO_8859_1);
    byte[] timestampBytes = StringUtils.leftPad(String.valueOf(connect.getTimestamp()), 10, "0").getBytes(StandardCharsets.ISO_8859_1);
    byte[] authenticationBytes = DigestUtils.md5(ByteUtil.concat(sourceBytes, new byte[9], secretBytes, timestampBytes));
    connect.setAuthenticatorSource(authenticationBytes);

    ctx.channel().writeAndFlush(connect);
    logger.log(internalLevel, "send connect request");

    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel channel = ctx.channel();

    // 获取connectResp
    if (msg instanceof ConnectRespMessage) {
      ConnectRespMessage resp = (ConnectRespMessage) msg;

      int result = resp.getStatus();

      if (result == 0) {
        // bind 成功
        logger.log(internalLevel, "{} connect success", loginName);

      } else {

        channel.close();
        logger.log(internalLevel, "{} connect failure[result:{}]", loginName, result);
      }

      return;
    }

    // terminate
    if (msg instanceof TerminateMessage) {
      // 直接回复UnbindResp
      channel.writeAndFlush(new TerminateRespMessage()).addListener(future -> {
        if (future.isSuccess()) {
          channel.close();
          logger.log(internalLevel, "{} terminate success and channel closed", loginName);
        }
      });

      return;
    }

    // activeTestResp
    if (msg instanceof ActiveTestRespMessage) {
      logger.log(internalLevel, "{} received active test resp message", loginName);
    }

    ctx.fireChannelRead(msg);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    // 处理空闲链接
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent iEvt = (IdleStateEvent) evt;
      if (iEvt.state().equals(IdleState.ALL_IDLE)) {

        // 发送unbind
        ctx.channel().writeAndFlush(new ActiveTestMessage()).addListener(future -> {
          if (future.isSuccess()) {
            logger.log(internalLevel, "{} request active test when idle", loginName);
          }
        });

        return;
      }
    }

    ctx.fireUserEventTriggered(evt);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    ctx.channel().close();
    logger.log(internalLevel, "{} catch exception and channel closed, {}", loginName, cause);
  }
}
