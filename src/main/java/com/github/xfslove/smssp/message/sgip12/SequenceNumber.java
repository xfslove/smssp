package com.github.xfslove.smssp.message.sgip12;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author hanwen
 * created at 2018/8/29
 */
public class SequenceNumber implements Serializable {

  private final int nodeId;

  private final int timestamp;

  private final int sequenceId;

  public SequenceNumber(int nodeId, int timestamp, int sequenceId) {
    this.nodeId = nodeId;
    this.timestamp = timestamp;
    this.sequenceId = sequenceId;
  }

  /**
   * @return sequenceNumber 长度 bytes
   */
  public final int getLength() {
    return 4 + 4 + 4;
  }

  public int getNodeId() {
    return nodeId;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public int getSequenceId() {
    return sequenceId;
  }

  public String stringId() {
    // nodeId:32bits, sequenceId:32bits
    return String.format("%1$010d%2$010d%3$010d",
        nodeId & 0xffffffffL, timestamp, sequenceId);
  }

  public byte[] getBytes() {
    byte[] bytes = new byte[12];
    ByteBuffer.wrap(bytes).putInt(getNodeId()).putInt(getTimestamp()).putInt(getSequenceId());
    return bytes;
  }

  public static SequenceNumber create(int nodeId, int timestamp, int sequenceId) {
    return new SequenceNumber(nodeId, timestamp, sequenceId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SequenceNumber that = (SequenceNumber) o;
    return nodeId == that.nodeId &&
        timestamp == that.timestamp &&
        sequenceId == that.sequenceId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodeId, timestamp, sequenceId);
  }

  @Override
  public String toString() {
    return "SequenceNumber{" +
        "nodeId=" + (nodeId & 0xffffffffL) +
        ", timestamp=" + timestamp +
        ", sequenceId=" + sequenceId +
        ", stringId=" + stringId() +
        '}';
  }
}
