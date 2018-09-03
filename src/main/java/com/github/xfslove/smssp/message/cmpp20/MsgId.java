package com.github.xfslove.smssp.message.cmpp20;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class MsgId implements Serializable {

  private final int gateId;

  private final int month;

  private final int day;

  private final int hour;

  private final int minute;

  private final int second;

  private final int sequenceId;

  public MsgId() {
    this(0,0,0,0,0,0,0);
  }

  public MsgId(int gateId, int month, int day, int hour, int minute, int second, int sequenceId) {
    this.gateId = gateId;
    this.month = month;
    this.day = day;
    this.hour = hour;
    this.minute = minute;
    this.second = second;
    this.sequenceId = sequenceId;
  }

  /**
   *
   * @return msgId长度 bytes
   */
  public final int getLength() {
    return 8;
  }

  public int getGateId() {
    return gateId;
  }

  public int getMonth() {
    return month;
  }

  public int getDay() {
    return day;
  }

  public int getHour() {
    return hour;
  }

  public int getMinute() {
    return minute;
  }

  public int getSecond() {
    return second;
  }

  public int getSequenceId() {
    return sequenceId;
  }

  public byte[] getBytes() {
    // 64 bits
    byte[] bytes = new byte[8];
    long result = 0;
    result |= (long) getMonth() << 60;
    result |= (long) getDay() << 55;
    result |= (long) getHour() << 50;
    result |= (long) getMinute() << 44;
    result |= (long) getSecond() << 38;
    result |= (long) getGateId() << 16;
    result |= getSequenceId() & 0xffff;
    ByteBuffer.wrap(bytes).putLong(result);
    return bytes;
  }

  public static MsgId create(long msgId) {
    int month = (int) ((msgId >>> 60) & 0xf);
    int day = (int) ((msgId >>> 55) & 0x1f);
    int hour = (int) ((msgId >>> 50) & 0x1f);
    int min = (int) ((msgId >>> 44) & 0x3f);
    int sec = (int) ((msgId >>> 38) & 0x3f);
    int gateId = (int) ((msgId >>> 16) & 0x3fffff);
    int sequenceId = (int) (msgId & 0xffff);
    return new MsgId(gateId, month, day, hour, min, sec, sequenceId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MsgId msgId = (MsgId) o;
    return gateId == msgId.gateId &&
        month == msgId.month &&
        day == msgId.day &&
        hour == msgId.hour &&
        minute == msgId.minute &&
        second == msgId.second &&
        sequenceId == msgId.sequenceId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gateId, month, day, hour, minute, second, sequenceId);
  }

  @Override
  public String toString() {
    byte[] bytes = getBytes();
    return "MsgId{" +
        "msgId=" + ByteBuffer.wrap(bytes).getLong() +
        '}';
  }
}
