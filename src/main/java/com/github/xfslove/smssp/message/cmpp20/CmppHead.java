package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.message.MessageHead;

/**
 * cmpp 头
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class CmppHead implements MessageHead {

  private final int commandId;

  private int sequenceId = 1;

  public CmppHead(int commandId) {
    this.commandId = commandId;
  }

  /**
   * 整个message长度
   * head + body
   *
   * @return 头长度  bytes
   */
  @Override
  public final int getLength() {
    // include message length 4 bytes
    return 4 + 4 + 4;
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
