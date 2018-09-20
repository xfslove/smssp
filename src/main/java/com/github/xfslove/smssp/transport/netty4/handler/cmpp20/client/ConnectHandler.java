package com.github.xfslove.smssp.transport.netty4.handler.cmpp20.client;

import com.github.xfslove.smssp.message.cmpp20.ConnectMessage;
import com.github.xfslove.smssp.message.cmpp20.ConnectRespMessage;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.transport.netty4.handler.AttributeConstant;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
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

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private String name;

  private String password;

  private Sequence<Integer> sequence;

  public ConnectHandler(String name, String password, Sequence<Integer> sequence) {
    this.sequence = sequence;
    this.name = name;
    this.password = password;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 发送connect请求
    ConnectMessage connect = new ConnectMessage(sequence);

    connect.setTimestamp(Integer.valueOf(DateFormatUtils.format(new Date(), "MMddHHmmss")));
    connect.setSourceAddr(name);
    byte[] sourceBytes = connect.getSourceAddr().getBytes(StandardCharsets.ISO_8859_1);
    byte[] secretBytes = password.getBytes(StandardCharsets.ISO_8859_1);
    byte[] timestampBytes = StringUtils.leftPad(String.valueOf(connect.getTimestamp()), 10, "0").getBytes(StandardCharsets.ISO_8859_1);
    byte[] authenticationBytes = DigestUtils.md5(ByteUtil.concat(sourceBytes, new byte[9], secretBytes, timestampBytes));
    connect.setAuthenticatorSource(authenticationBytes);

    ctx.writeAndFlush(connect);
    logger.info("{} connect request", name);

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
        logger.info("{} connect success", name);
        channel.attr(AttributeConstant.NAME).set(name);
      } else {

        logger.info("{} connect failure[result:{}] and close channel", name, result);
        channel.close();
      }

      return;
    }

    ctx.fireChannelRead(msg);
  }
}