package com.github.xfslove.message.cmpp20;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class TerminateMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_TERMINATE);

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
  public MessageHead getHead() {
    return head;
  }

  @Override
  public String toString() {
    return "TerminateMessage{" +
        "head=" + head +
        '}';
  }
}
