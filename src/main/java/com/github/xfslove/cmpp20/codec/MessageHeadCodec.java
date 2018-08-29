package com.github.xfslove.cmpp20.codec;

import com.github.xfslove.cmpp20.message.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * cmpp 消息头的codec，应该放在其它codec之前
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MessageHeadCodec extends ByteToMessageCodec<CmppMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, CmppMessage msg, ByteBuf out) throws Exception {
    MessageHead head = msg.getHead();
    // 4 bytes
    out.writeInt(head.getMessageLength());
    out.writeInt(head.getCommandId());
    out.writeInt(head.getSequenceId());
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
    int sequence = in.readInt();
    message.getHead().setSequenceId(sequence);

    list.add(message);
  }
}
