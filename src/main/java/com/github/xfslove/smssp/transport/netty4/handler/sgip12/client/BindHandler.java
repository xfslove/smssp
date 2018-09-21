package com.github.xfslove.smssp.transport.netty4.handler.sgip12.client;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * sp -> smg session管理handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class BindHandler extends ChannelDuplexHandler {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(BindHandler.class);

  private Sequence<SequenceNumber> sequence;

  private String username;

  private String password;

  public BindHandler(String username, String password, Sequence<SequenceNumber> sequence) {
    this.username = username;
    this.password = password;
    this.sequence = sequence;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 发送bind请求
    BindMessage bind = new BindMessage(sequence);

    bind.setLoginName(username);
    bind.setLoginPassword(password);
    ctx.writeAndFlush(bind);
    LOGGER.info("client bind request");

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
        LOGGER.info("bind success");
      } else {

        channel.close();
        LOGGER.info("bind failure[result:{}]", result);
      }

      return;
    }

    ctx.fireChannelRead(msg);
  }
}
