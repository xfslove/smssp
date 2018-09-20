package com.github.xfslove.smssp.transport.netty4.handler.sgip12.client;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.message.sgip12.BindMessage;
import com.github.xfslove.smssp.message.sgip12.BindRespMessage;
import com.github.xfslove.smssp.message.sgip12.SequenceNumber;
import com.github.xfslove.smssp.transport.netty4.handler.AttributeConstants;
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

  private final InternalLogger logger = InternalLoggerFactory.getInstance(getClass());

  private Sequence<SequenceNumber> sequence;

  private String name;

  private String password;

  public BindHandler(String name, String password, Sequence<SequenceNumber> sequence) {
    this.name = name;
    this.password = password;
    this.sequence = sequence;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {

    // 发送bind请求
    BindMessage bind = new BindMessage(sequence);

    bind.setLoginName(name);
    bind.setLoginPassword(password);
    ctx.writeAndFlush(bind);
    logger.info("{} client bind request", name);

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
        logger.info("{} bind success", name);
        channel.attr(AttributeConstants.NAME).set(name);
      } else {

        channel.close();
        logger.info("{} bind failure[result:{}]", name, result);
      }

      return;
    }

    ctx.fireChannelRead(msg);
  }
}
