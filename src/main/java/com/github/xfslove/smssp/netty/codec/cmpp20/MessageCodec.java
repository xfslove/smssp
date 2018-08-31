package com.github.xfslove.smssp.netty.codec.cmpp20;

import com.github.xfslove.smssp.message.cmpp20.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * cmpp2.0 消息的codec
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MessageCodec extends ByteToMessageCodec<CmppMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, CmppMessage msg, ByteBuf out) throws Exception {
    MessageHead head = msg.getHead();
    // 4 bytes
    out.writeInt(head.getLength() + msg.getLength());
    out.writeInt(head.getCommandId());
    out.writeInt(head.getSequenceId());

    msg.write(out);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
    // 4 bytes
    int messageLength = in.readInt();
    int commandId = in.readInt();

    CmppMessage message;
    switch (commandId) {
      case CmppConstants.CMPP_CONNECT:
        message = new ConnectMessage();
        break;
      case CmppConstants.CMPP_CONNECT_RESP:
        message = new ConnectRespMessage();
        break;
      case CmppConstants.CMPP_SUBMIT_RESP:
        message = new SubmitRespMessage();
        break;
      case CmppConstants.CMPP_DELIVER:
        message = new DeliverMessage();
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

    int sequenceId = in.readInt();
    message.getHead().setSequenceId(sequenceId);

    message.read(in);

    list.add(message);
  }
}
