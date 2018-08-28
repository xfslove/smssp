package com.github.xfslove.cmpp20.message;

import java.io.Serializable;

/**
 * cmpp 头
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

  private int sequenceId;

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

  public int getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(int sequenceId) {
    this.sequenceId = sequenceId;
  }
}
