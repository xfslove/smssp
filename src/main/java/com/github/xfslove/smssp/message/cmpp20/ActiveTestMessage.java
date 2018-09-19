package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.message.Sequence;
import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/31
 */
public class ActiveTestMessage implements CmppMessage {

  private final CmppHead head;

  public ActiveTestMessage(int sequenceId) {
    head = new CmppHead(CmppConstants.CMPP_ACTIVE_TEST, sequenceId);
  }

  public ActiveTestMessage(Sequence<Integer> sequence) {
    head = new CmppHead(CmppConstants.CMPP_ACTIVE_TEST, sequence.next());
  }

  @Override
  public CmppHead getHead() {
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
    return "ActiveTestMessage{" +
        "head=" + head +
        '}';
  }
}
