package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.ud.SmsUdhElement;
import com.github.xfslove.smsj.sms.ud.SmsUdhIei;
import com.github.xfslove.smsj.sms.ud.SmsUdhUtil;
import com.github.xfslove.smsj.util.StringUtil;
import com.github.xfslove.smssp.message.sequence.Sequence;
import com.github.xfslove.smssp.server.Notification;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverMessage extends SmsPdu implements SgipMessage, Notification {

  private final SgipHead head;

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

  public DeliverMessage(SequenceNumber sequenceNumber) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_DELIVER, sequenceNumber);
  }

  public DeliverMessage(Sequence sequence) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_DELIVER, (SequenceNumber) sequence.next());
  }

  @Override
  public String getId() {
    return getHead().getSequenceNumber().stringId();
  }

  @Override
  public Partition getPartition() {
    if (getTpUdhi() == 0) {
      return new Partition(1, 1, null);
    }

    SmsUdhElement[] udh = getUserDateHeaders();

    // notice: 暂时只做普通短信的合并
    if (udh.length > 1) {
      return null;
    }
    SmsUdhElement firstUdh = udh[0];
    if (!SmsUdhIei.APP_PORT_8BIT.equals(firstUdh.getUdhIei())) {
      return null;
    }

    int refNr = firstUdh.getUdhIeiData()[0] & 0xff;
    int total = firstUdh.getUdhIeiData()[1] & 0xff;
    int seqNr = firstUdh.getUdhIeiData()[2] & 0xff;
    String key = getUserNumber() + "-" + refNr;

    return new Partition(total, seqNr, key);
  }

  @Override
  public boolean concat(Notification next) {
    // notice: 暂时只做普通短信的合并
    DeliverMessage d = (DeliverMessage) next;
    if (getDcs().getValue() != d.getDcs().getValue()) {
      return false;
    }
    setUserData(ByteUtil.concat(getUdBytes(), d.getUdBytes()), getDcs());
    return true;
  }

  @Override
  public SgipHead getHead() {
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
        ", userDataHeader='" + StringUtil.bytesToHexString(getUdhBytes()) + '\'' +
        ", userData='" + StringUtil.getString(getUdBytes(), getDcs().getAlphabet(), Charset.forName("GBK")) + '\'' +
        ", spNumber='" + spNumber + '\'' +
        ", userNumber='" + userNumber + '\'' +
        ", tpPid=" + tpPid +
        ", tpUdhi=" + tpUdhi +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
