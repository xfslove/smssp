package com.github.xfslove.cmpp20.codec;

import com.github.xfslove.cmpp20.message.CmppMessage;
import com.github.xfslove.cmpp20.message.ConnectMessage;
import com.github.xfslove.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ConnectMessageCodec extends MessageToMessageCodec<CmppMessage, ConnectMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, ConnectMessage msg, List<Object> out) throws Exception {
    ByteBuf buffer = ctx.alloc().buffer();

    // 6 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getSourceAddr(), 6));
    // 16 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getAuthenticatorSource(), 16));
    // 1 byte
    buffer.writeByte(msg.getVersion());
    // 4 bytes
    buffer.writeInt(msg.getTimestamp());
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, CmppMessage msg, List<Object> out) throws Exception {
    // no need implement
  }
}
