package com.github.xfslove.sgip12.codec;

import com.github.xfslove.sgip12.message.SgipMessage;
import com.github.xfslove.sgip12.message.SubmitMessage;
import com.github.xfslove.util.StringUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SubmitMessageCodec extends MessageToMessageCodec<SgipMessage, SubmitMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, SubmitMessage msg, List<Object> out) throws Exception {
    ByteBuf buffer = ctx.alloc().buffer();

    // 21 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getSpNumber(), 21));
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getChargeNumber(), 21));
    // 1 byte
    buffer.writeByte(msg.getUserNumbers().size());
    for (String userNumber : msg.getUserNumbers()) {
      // 21 bytes
      buffer.writeBytes(StringUtil.getOctetStringBytes(userNumber, 21));
    }
    // 5 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getCorpId(), 5));
    // 10 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getServiceType(), 10));
    // 1 byte
    buffer.writeByte(msg.getFeeType());
    // 6 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getFeeValue(), 6));
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getGivenValue(), 6));
    // 1 byte
    buffer.writeByte(msg.getAgentFlag());
    buffer.writeByte(msg.getMorelatetoMTFlag());
    buffer.writeByte(msg.getPriority());
    // 16 bytes
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getExpireTime(), 16));
    buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getScheduleTime(), 16));
    // 1 byte
    buffer.writeByte(msg.getReportFlag());
    buffer.writeByte(msg.getTpPid());
    buffer.writeByte(msg.getTpUdhi());

    // 1 byte
    buffer.writeByte(msg.getDcs().getValue());
    buffer.writeByte(msg.getMessageType());
    // 4 bytes
    byte[] udh = msg.getUdhBytes();
    byte[] ud = msg.getUdBytes();
    buffer.writeInt(udh.length + ud.length);
    buffer.writeBytes(udh);
    buffer.writeBytes(ud);
    // 8 bytes
    if (msg.getReserve() != null && msg.getReserve().length() > 0) {
      buffer.writeBytes(StringUtil.getOctetStringBytes(msg.getReserve(), 8));
    }

    out.add(buffer);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, SgipMessage msg, List<Object> out) throws Exception {
    // no need implement
  }
}
