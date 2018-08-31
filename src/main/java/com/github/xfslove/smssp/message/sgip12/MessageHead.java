package com.github.xfslove.smssp.message.sgip12;

import java.io.Serializable;

/**
 * sgip 头
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MessageHead implements Serializable {

  private final int commandId;

  private SequenceNumber sequenceNumber;

  public MessageHead(int commandId) {
    this.commandId = commandId;
    // todo
    sequenceNumber = SequenceNumber.create(1, 444444444, 223323);
  }

  /**
   * 整个message长度
   * head + body
   *
   * @return 头长度 bytes
   */
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

  public void setSequenceNumber(SequenceNumber sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  @Override
  public String toString() {
    return "MessageHead{" +
        "commandId=" + commandId +
        ", sequenceNumber=" + sequenceNumber +
        '}';
  }
}
