package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.message.sequence.Sequence;
import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class TerminateRespMessage implements CmppMessage {

  private final CmppHead head;

  public TerminateRespMessage(int sequenceId) {
    head = new CmppHead(CmppConstants.CMPP_TERMINATE_RESP, sequenceId);
  }

  public TerminateRespMessage(Sequence sequence) {
    head = new CmppHead(CmppConstants.CMPP_TERMINATE_RESP, (int) sequence.next());
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public CmppHead getHead() {
    return head;
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
    return "TerminateRespMessage{" +
        "head=" + head +
        '}';
  }
}
