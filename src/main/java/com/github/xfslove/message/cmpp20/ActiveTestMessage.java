package com.github.xfslove.message.cmpp20;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/31
 */
public class ActiveTestMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_ACTIVE_TEST);

  @Override
  public MessageHead getHead() {
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
}
