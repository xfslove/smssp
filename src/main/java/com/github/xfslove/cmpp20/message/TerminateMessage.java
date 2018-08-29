package com.github.xfslove.cmpp20.message;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class TerminateMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_TERMINATE);

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
}
