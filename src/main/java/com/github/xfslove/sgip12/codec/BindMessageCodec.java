package com.github.xfslove.sgip12.codec;

import com.github.xfslove.sgip12.message.BindMessage;
import com.github.xfslove.sgip12.message.SgipMessage;
import com.github.xfslove.sgip12.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class BindMessageCodec extends MessageToMessageCodec<SgipMessage, BindMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, BindMessage msg, List<Object> out) throws Exception {
    ByteBuf buffer = ctx.alloc().buffer();

    // 1 byte
    buffer.writeByte(msg.getLoginType());
    // 16 bytes
    buffer.writeBytes(StringUtil.ensure(msg.getLoginName(), 16));
    buffer.writeBytes(StringUtil.ensure(msg.getLoginPassword(), 16));
    // 8 bytes
    if (msg.getReserve() != null && msg.getReserve().length() > 0) {
      buffer.writeBytes(StringUtil.ensure(msg.getReserve(), 8));
    }

    out.add(buffer);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, SgipMessage msg, List<Object> out) throws Exception {
    // no need implement
  }
}
