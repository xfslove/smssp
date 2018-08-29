package com.github.xfslove.sgip12.message;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class UnBindMessage implements SgipMessage {

  private final MessageHead head = new MessageHead(SgipConstants.COMMAND_ID_UNBIND);

  @Override
  public MessageHead getHead() {
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
}
