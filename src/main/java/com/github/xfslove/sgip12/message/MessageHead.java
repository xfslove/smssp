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

  private int messageLength;

  private SequenceNumber sequenceNumber;

  public MessageHead(int commandId) {
    this.commandId = commandId;
  }

  public void setMessageLength(int messageLength) {
    this.messageLength = messageLength;
  }

  public void setSequenceNumber(SequenceNumber sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public int getMessageLength() {
    return messageLength;
  }

  public int getCommandId() {
    return commandId;
  }

  public SequenceNumber getSequenceNumber() {
    return sequenceNumber;
  }
}
