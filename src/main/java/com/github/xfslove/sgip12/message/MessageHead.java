package com.github.xfslove.sgip12.message;

import java.io.Serializable;

/**
 * sgip 头
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MessageHead implements Serializable {

  private final int commandId;

  /**
   * 整个message长度
   * head + body
   */
  private int messageLength;

  private SequenceNumber sequenceNumber;

  public MessageHead(int commandId) {
    this.commandId = commandId;
  }

  public int getCommandId() {
    return commandId;
  }

  public int getMessageLength() {
    return messageLength;
  }

  public void setMessageLength(int messageLength) {
    this.messageLength = messageLength;
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
        ", messageLength=" + messageLength +
        ", sequenceNumber=" + sequenceNumber +
        '}';
  }
}
