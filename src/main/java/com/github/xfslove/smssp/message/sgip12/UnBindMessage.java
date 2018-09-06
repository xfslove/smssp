package com.github.xfslove.smssp.message.sgip12;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class UnBindMessage implements SgipMessage {

  private final SgipHead head = new SgipHead(SgipConstants.COMMAND_ID_UNBIND);

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
    return "UnBindMessage{" +
        "head=" + head +
        '}';
  }
}
