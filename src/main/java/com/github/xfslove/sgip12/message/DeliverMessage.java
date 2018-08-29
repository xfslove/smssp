package com.github.xfslove.sgip12.message;

import com.github.xfslove.sms.SmsPdu;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverMessage extends SmsPdu implements SgipMessage {

  private final MessageHead head = new MessageHead(SgipConstants.COMMAND_ID_DELIVER);

  /**
   * SP的接入号码
   */
  private String spNumber;

  /**
   * 发送短消息的用户手机号，手机号码前加“86”国别标志
   */
  private String userNumber;

  /**
   * GSM协议类型。详细解释请参考GSM03.40中的9.2.3.9
   */
  private int tpPid = 0;

  /**
   * GSM协议类型。详细解释请参考GSM03.40中的9.2.3.23,仅使用1位
   * 0: 没有udh
   * 1: 有udh
   */
  private int tpUdhi = 0;

  /**
   * 保留，扩展用
   */
  private String reserve;

  @Override
  public MessageHead getHead() {
    return head;
  }

  @Override
  public void write(ByteBuf out) {
    // no need implement
  }

  @Override
  public void read(ByteBuf in) {
    setUserNumber(in.readCharSequence(21, StandardCharsets.ISO_8859_1).toString().trim());
    setSpNumber(in.readCharSequence(21, StandardCharsets.ISO_8859_1).toString().trim());
    setTpPid(in.readUnsignedByte());
    setTpUdhi(in.readUnsignedByte());

    // todo 内容
    int dcs = in.readUnsignedByte();

    int msgLength = in.readInt();
    byte[] contentbytes = new byte[msgLength];
    in.readBytes(contentbytes);

    setReserve(in.readCharSequence(8, StandardCharsets.ISO_8859_1).toString().trim());
  }

  public String getSpNumber() {
    return spNumber;
  }

  public void setSpNumber(String spNumber) {
    this.spNumber = spNumber;
  }

  public String getUserNumber() {
    return userNumber;
  }

  public void setUserNumber(String userNumber) {
    this.userNumber = userNumber;
  }

  public int getTpPid() {
    return tpPid;
  }

  public void setTpPid(int tpPid) {
    this.tpPid = tpPid;
  }

  public int getTpUdhi() {
    return tpUdhi;
  }

  public void setTpUdhi(int tpUdhi) {
    this.tpUdhi = tpUdhi;
  }

  public String getReserve() {
    return reserve;
  }

  public void setReserve(String reserve) {
    this.reserve = reserve;
  }

  @Override
  public String toString() {
    // todo 内容
    return "DeliverMessage{" +
        "head=" + head +
        ", spNumber='" + spNumber + '\'' +
        ", userNumber='" + userNumber + '\'' +
        ", tpPid=" + tpPid +
        ", tpUdhi=" + tpUdhi +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
