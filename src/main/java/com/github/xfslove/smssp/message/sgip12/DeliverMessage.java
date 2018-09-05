package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.ud.SmsUdhElement;
import com.github.xfslove.smsj.sms.ud.SmsUdhUtil;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverMessage extends SmsPdu implements SgipMessage {

  private final Sgip12Head head = new Sgip12Head(SgipConstants.COMMAND_ID_DELIVER);

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
  public Sgip12Head getHead() {
    return head;
  }

  @Override
  public int getLength() {
    // no need implement
    return -1;
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

    int dcs = in.readUnsignedByte();

    int msgLength = in.readInt();

    // deliver
    if (tpUdhi == 0) {

      byte[] content = new byte[msgLength];
      in.readBytes(content);
      setUserData(content, new SmsDcs((byte) dcs));
    } else {

      int udhl = in.readUnsignedByte();

      byte[] udh = new byte[udhl];
      in.readBytes(udh);
      // include udhl
      SmsUdhElement[] udhElements = SmsUdhUtil.deserialize(udh);

      setUserDataHeaders(udhElements);

      int udl = msgLength - udhl - 1;
      byte[] ud = new byte[udl];
      setUserData(ud, new SmsDcs((byte) dcs));
    }

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
    return "DeliverMessage{" +
        "head=" + head +
        ", dcs=" + getDcs().getValue() +
        ", userDataHeader='" + ByteUtil.getString(getUdhBytes(), getDcs().getAlphabet()) + '\'' +
        ", userData='" + ByteUtil.getString(getUdBytes(), getDcs().getAlphabet()) + '\'' +
        ", spNumber='" + spNumber + '\'' +
        ", userNumber='" + userNumber + '\'' +
        ", tpPid=" + tpPid +
        ", tpUdhi=" + tpUdhi +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
