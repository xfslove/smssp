package com.github.xfslove.smssp.message.cmpp20;

import java.io.Serializable;

/**
 * cmpp 头
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MessageHead implements Serializable {

  private final int commandId;

  private int sequenceId;

  /**
   * 整个message长度
   * head + body
   *
   * @return 头长度  bytes
   */
  public final int getLength() {
    // include message length 4 bytes
    return 4 + 4 + 12;
  }

  public MessageHead(int commandId) {
    this.commandId = commandId;
  }

  public int getCommandId() {
    return commandId;
  }

  public int getSequenceId() {
    return sequenceId;
  }

  public void setSequenceId(int sequenceId) {
    this.sequenceId = sequenceId;
  }

  @Override
  public String toString() {
    return "MessageHead{" +
        "commandId=" + commandId +
        ", sequenceId=" + sequenceId +
        '}';
  }
}
