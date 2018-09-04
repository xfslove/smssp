package com.github.xfslove.smssp.netty4.handler.cmpp20.send;

import com.github.xfslove.smssp.message.cmpp20.ConnectMessage;
import com.github.xfslove.smssp.message.cmpp20.ConnectRespMessage;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @author hanwen
 * created at 2018/9/3
 */
@ChannelHandler.Sharable
public class ConnectHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private String loginName;

  private String loginPassword;

  public ConnectHandler(String loginName, String loginPassword, LogLevel level) {
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
    logger.log(internalLevel, "{} send connect request", loginName);

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
        // connect 成功
        logger.log(internalLevel, "{} connect success", loginName);

      } else {

        channel.close();
        logger.log(internalLevel, "{} connect failure[result:{}]", loginName, result);
      }

      return;
    }

    ctx.fireChannelRead(msg);
  }
}
