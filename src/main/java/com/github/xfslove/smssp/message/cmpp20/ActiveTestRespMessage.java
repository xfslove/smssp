package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.message.Sequence;
import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/31
 */
public class ActiveTestRespMessage implements CmppMessage {

  private final CmppHead head;

  /**
   * 保留
   */
  private int reserve;

  public ActiveTestRespMessage(int sequenceId) {
    head = new CmppHead(CmppConstants.CMPP_ACTIVE_TEST_RESP, sequenceId);
  }

  public ActiveTestRespMessage(Sequence<Integer> sequence) {
    head = new CmppHead(CmppConstants.CMPP_ACTIVE_TEST_RESP, sequence.next());
  }


  @Override
  public CmppHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    return 1;
  }

  @Override
  public void write(ByteBuf out) {
    // 1 byte
    out.writeByte(reserve);
  }

  @Override
  public void read(ByteBuf in) {
    setReserve(in.readUnsignedByte());
  }

  public int getReserve() {
    return reserve;
  }

  public void setReserve(int reserve) {
    this.reserve = reserve;
  }

  @Override
  public String toString() {
    return "ActiveTestRespMessage{" +
        "head=" + head +
        ", reserve=" + reserve +
        '}';
  }
}
