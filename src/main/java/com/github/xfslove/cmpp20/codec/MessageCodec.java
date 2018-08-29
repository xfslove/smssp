package com.github.xfslove.cmpp20.codec;

import com.github.xfslove.cmpp20.message.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

/**
 * cmpp 消息的codec
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MessageCodec extends MessageToMessageCodec<ByteBuf, CmppMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, CmppMessage msg, List<Object> out) throws Exception {
    MessageHead head = msg.getHead();
    ByteBuf buf = Unpooled.buffer(head.getMessageLength());
    // 4 bytes
    buf.writeInt(head.getMessageLength());
    buf.writeInt(head.getCommandId());
    buf.writeInt(head.getSequenceId());

    msg.write(buf);

    out.add(buf);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
    // 4 bytes
    int messageLength = in.readInt();
    int commandId = in.readInt();
    int sequence = in.readInt();

    CmppMessage message;
    switch (commandId) {
      case CmppConstants.CMPP_CONNECT:
        message = new ConnectMessage();
        break;
      case CmppConstants.CMPP_CONNECT_RESP:
        message = new ConnectRespMessage();
        break;
      case CmppConstants.CMPP_SUBMIT:
        message = new SubmitMessage();
        break;
      case CmppConstants.CMPP_SUBMIT_RESP:
        message = new SubmitRespMessage();
        break;
      case CmppConstants.CMPP_DELIVER:
        message = new DeliverMessage();
        break;
      case CmppConstants.CMPP_DELIVER_RESP:
        message = new DeliverRespMessage();
        break;
      case CmppConstants.CMPP_TERMINATE:
        message = new TerminateMessage();
        break;
      case CmppConstants.CMPP_TERMINATE_RESP:
        message = new TerminateRespMessage();
        break;
      default:
        throw new IllegalArgumentException("unsupported commandId: " + commandId);
    }

    message.getHead().setMessageLength(messageLength);
    message.getHead().setSequenceId(sequence);

    message.read(in);

    list.add(message);
  }
}
