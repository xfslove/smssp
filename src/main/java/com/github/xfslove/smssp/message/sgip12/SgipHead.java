package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.message.MessageHead;

/**
 * sgip 头
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class SgipHead implements MessageHead {

  private final int commandId;

  private final SequenceNumber sequenceNumber;

  public SgipHead(int commandId, SequenceNumber sequenceNumber) {
    this.commandId = commandId;
    this.sequenceNumber = sequenceNumber;
  }

  /**
   * 整个message长度
   * head + body
   *
   * @return 头长度 bytes
   */
  @Override
  public final int getLength() {
    // include message length 4 bytes
    return 4 + 4 + getSequenceNumber().getLength();
  }

  public int getCommandId() {
    return commandId;
  }

  public SequenceNumber getSequenceNumber() {
    return sequenceNumber;
  }

  @Override
  public String toString() {
    return "MessageHead{" +
        "commandId=" + commandId +
        ", sequenceNumber=" + sequenceNumber +
        '}';
  }
}
