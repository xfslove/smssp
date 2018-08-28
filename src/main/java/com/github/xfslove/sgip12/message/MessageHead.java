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

  private int nodeId;

  private int timestamp;

  private int sequence;

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

  public int getNodeId() {
    return nodeId;
  }

  public void setNodeId(int nodeId) {
    this.nodeId = nodeId;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(int timestamp) {
    this.timestamp = timestamp;
  }

  public int getSequence() {
    return sequence;
  }

  public void setSequence(int sequence) {
    this.sequence = sequence;
  }
}
