package com.github.xfslove.sgip12.message;

import java.io.Serializable;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SequenceNumber implements Serializable {

  private int nodeId;

  private int timestamp;

  private int sequence;

  public SequenceNumber(int nodeId, int timestamp, int sequence) {
    this.nodeId = nodeId;
    this.timestamp = timestamp;
    this.sequence = sequence;
  }

  public int getNodeId() {
    return nodeId;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public int getSequence() {
    return sequence;
  }
}
