package com.github.xfslove.smssp.netty4.codec.sgip12;

import com.github.xfslove.smssp.message.sgip12.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

/**
 * sgip1.2 消息的codec
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MessageCodec extends ByteToMessageCodec<SgipMessage> {

  @Override
  protected void encode(ChannelHandlerContext ctx, SgipMessage msg, ByteBuf out) throws Exception {
    SgipHead head = msg.getHead();
    // 4 bytes
    out.writeInt(head.getLength() + msg.getLength());
    out.writeInt(head.getCommandId());

    byte[] bytes = head.getSequenceNumber().getBytes();
    out.writeBytes(bytes);

    msg.write(out);
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    // 4 bytes
    int messageLength = in.readInt();
    int commandId = in.readInt();
    // 4 bytes
    int nodeId = in.readInt();
    int timestamp = in.readInt();
    int sequenceId = in.readInt();
    SequenceNumber sequenceNumber = SequenceNumber.create(nodeId, timestamp, sequenceId);

    SgipMessage message;

    switch (commandId) {
      case SgipConstants.COMMAND_ID_BIND:
        message = new BindMessage(sequenceNumber);
        break;
      case SgipConstants.COMMAND_ID_BIND_RESP:
        message = new BindRespMessage(sequenceNumber);
        break;
      case SgipConstants.COMMAND_ID_SUBMIT_RESP:
        message = new SubmitRespMessage(sequenceNumber);
        break;
      case SgipConstants.COMMAND_ID_DELIVER:
        message = new DeliverMessage(sequenceNumber);
        break;
      case SgipConstants.COMMAND_ID_REPORT:
        message = new ReportMessage(sequenceNumber);
        break;
      case SgipConstants.COMMAND_ID_UNBIND:
        message = new UnBindMessage(sequenceNumber);
        break;
      case SgipConstants.COMMAND_ID_UNBIND_RESP:
        message = new UnBindRespMessage(sequenceNumber);
        break;
      default:
        throw new IllegalArgumentException("unsupported commandId: " + commandId);
    }

    message.read(in);

    out.add(message);
  }
}
