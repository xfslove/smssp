package com.github.xfslove.cmpp20.codec;

import com.github.xfslove.cmpp20.message.CmppMessage;
import com.github.xfslove.cmpp20.message.SubmitMessage;
import com.github.xfslove.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

/**
 * @author hanwen
 * created at 2018/8/29
 */
public class SubmitMessageCodec extends MessageToMessageCodec<CmppMessage, SubmitMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, SubmitMessage msg, List<Object> out) throws Exception {
    ByteBuf buffer = ctx.alloc().buffer();

    // 8 bytes
    byte[] bytes = msg.getMsgId().getBytes();
    buffer.writeBytes(bytes);
    // 1 byte
    buffer.writeByte(msg.getPkTotal());
    buffer.writeByte(msg.getPkNumber());
    buffer.writeByte(msg.getRegisteredDelivery());
    buffer.writeByte(msg.getMsgLevel());
    // 10 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getServiceId(), 10));
    // 1 byte
    buffer.writeByte(msg.getFeeUserType());
    // 21 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getFeeTerminalId(), 21));
    // 1 byte
    buffer.writeByte(msg.getTpPid());
    buffer.writeByte(msg.getTpUdhi());

    buffer.writeByte(msg.getDcs().getValue());

    // 6 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getMsgSrc(), 6));
    // 2 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getFeeType(), 2));
    // 6 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getFeeCode(), 6));
    // 17 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getValIdTime(), 17));
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getAtTime(), 17));
    // 21 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getSrcId(), 21));

    buffer.writeByte(msg.getDestTerminalIds().size());
    for (String destTerminalId : msg.getDestTerminalIds()) {
      buffer.writeBytes(StringUtil.getOctetStringBytes(destTerminalId, 21));
    }

    byte[] udh = msg.getUdhBytes();
    byte[] ud = msg.getUdBytes();
    buffer.writeByte(udh.length + ud.length);
    buffer.writeBytes(udh);
    buffer.writeBytes(ud);

    // 8 bytes
    if (msg.getReserve() != null && msg.getReserve().length() > 0) {
      buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getReserve(), 8));
    }

    out.add(buffer);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, CmppMessage msg, List<Object> out) throws Exception {
    // no need implement
  }
}
