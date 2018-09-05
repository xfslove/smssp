package com.github.xfslove.smssp.netty4.handler.sgip12.send;

import com.github.xfslove.smssp.message.sequence.SequenceGenerator;
import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;

/**
 * sp -> smg session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class BindHandler extends ChannelDuplexHandler {

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  private SequenceGenerator sequenceGenerator;

  private int nodeId;

  private String loginName;

  private String loginPassword;

  public BindHandler(int nodeId, String loginName, String loginPassword, SequenceGenerator sequenceGenerator, LogLevel level) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.sequenceGenerator = sequenceGenerator;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 发送bind请求
    BindMessage bind = new BindMessage();
    bind.getHead().setSequenceNumber(SequenceNumber.create(nodeId, Integer.valueOf(DateFormatUtils.format(new Date(), "MMddHHmmss")), sequenceGenerator.next()));

    bind.setLoginName(loginName);
    bind.setLoginPassword(loginPassword);
    ctx.channel().writeAndFlush(bind);
    logger.log(internalLevel, "{} send bind request", loginName);

    ctx.fireChannelActive();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    final Channel channel = ctx.channel();

    // 获取bindResp
    if (msg instanceof BindRespMessage) {
      BindRespMessage bindResp = (BindRespMessage) msg;

      int result = bindResp.getResult();

      if (result == 0) {
        // bind 成功
        logger.log(internalLevel, "{} bind success", loginName);

      } else {

        channel.close();
        logger.log(internalLevel, "{} bind failure[result:{}]", loginName, result);
      }

      return;
    }

    ctx.fireChannelRead(msg);
  }
}
