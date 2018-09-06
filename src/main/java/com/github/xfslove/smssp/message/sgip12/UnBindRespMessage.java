package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.message.sequence.Sequence;
import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class UnBindRespMessage implements SgipMessage {

  private final SgipHead head;

  public UnBindRespMessage(SequenceNumber sequenceNumber) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_UNBIND_RESP, sequenceNumber);
  }

  public UnBindRespMessage(Sequence sequence) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_UNBIND_RESP, (SequenceNumber) sequence.next());
  }

  @Override
  public SgipHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public void write(ByteBuf out) {
    // nothing
  }

  @Override
  public void read(ByteBuf in) {
    // nothing
  }

  @Override
  public String toString() {
    return "UnBindRespMessage{" +
        "head=" + head +
        '}';
  }
}
