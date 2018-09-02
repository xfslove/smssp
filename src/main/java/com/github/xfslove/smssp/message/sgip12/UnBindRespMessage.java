package com.github.xfslove.smssp.message.sgip12;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class UnBindRespMessage implements SgipMessage {

  private final Sgip12Head head = new Sgip12Head(SgipConstants.COMMAND_ID_UNBIND_RESP);

  @Override
  public Sgip12Head getHead() {
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
