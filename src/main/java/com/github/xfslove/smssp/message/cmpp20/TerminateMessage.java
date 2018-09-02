package com.github.xfslove.smssp.message.cmpp20;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class TerminateMessage implements CmppMessage {

  private final CmppHead head = new CmppHead(CmppConstants.CMPP_TERMINATE);

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
  public CmppHead getHead() {
    return head;
  }

  @Override
  public String toString() {
    return "TerminateMessage{" +
        "head=" + head +
        '}';
  }
}
