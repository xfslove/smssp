package com.github.xfslove.sgip12.codec;

import com.github.xfslove.sgip12.message.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * sgip 消息头的codec，应该放在其它codec之前
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MesssageHeadCodec extends ByteToMessageCodec<SgipMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, SgipMessage message, ByteBuf out) throws Exception {
    MessageHead head = message.getHead();
    // 4 bytes
    out.writeInt(head.getMessageLength());
    out.writeInt(head.getCommandId());

    byte[] bytes = head.getSequenceNumber().getBytes();
    out.writeBytes(bytes);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    // 4 bytes
    int messageLength = in.readInt();
    int commandId = in.readInt();

    SgipMessage message;

    switch (commandId) {
      case SgipConstants.COMMAND_ID_BIND:
        message = new BindMessage();
        break;
      case SgipConstants.COMMAND_ID_BIND_RESP:
        message = new BindRespMessage();
        break;
      case SgipConstants.COMMAND_ID_SUBMIT:
        message = new SubmitMessage();
        break;
      case SgipConstants.COMMAND_ID_SUBMIT_RESP:
        message = new SubmitRespMessage();
        break;
      case SgipConstants.COMMAND_ID_DELIVER:
        message = new DeliverMessage();
        break;
      case SgipConstants.COMMAND_ID_DELIVER_RESP:
        message = new DeliverRespMessage();
        break;
      case SgipConstants.COMMAND_ID_REPORT:
        message = new ReportMessage();
        break;
      case SgipConstants.COMMAND_ID_REPORT_RESP:
        message = new ReportRespMessage();
        break;
      case SgipConstants.COMMAND_ID_UNBIND:
        message = new UnBindMessage();
        break;
      case SgipConstants.COMMAND_ID_UNBIND_RESP:
        message = new UnBindRespMessage();
        break;
      default:
        throw new IllegalArgumentException("unsupported commandId: " + commandId);
    }

    message.getHead().setMessageLength(messageLength);

    // 12 bytes
    byte[] bytes = new byte[12];
    in.readBytes(bytes);
    SequenceNumber sequenceNumber = SequenceNumber.create(bytes);
    message.getHead().setSequenceNumber(sequenceNumber);

    out.add(message);
  }
}
