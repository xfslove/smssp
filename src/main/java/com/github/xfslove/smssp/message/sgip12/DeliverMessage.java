package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.ud.SmsUdhElement;
import com.github.xfslove.smsj.sms.ud.SmsUdhUtil;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.notification.Notification;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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
   * 保留，扩展用
   */
  private String reserve;

  public DeliverMessage(SequenceNumber sequenceNumber) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_DELIVER, sequenceNumber);
  }

  public DeliverMessage(Sequence<SequenceNumber> sequence) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_DELIVER, sequence.next());
  }

  @Override
  public String getId() {
    return getHead().getSequenceNumber().stringId();
  }

  @Override
  public Partition getPartition() {
    if (getUserDateHeaders() == null) {
      return new Partition(1, 1, null);
    }

    SmsUdhElement[] udh = getUserDateHeaders();

    // notice: 暂时只做普通短信的合并
    if (udh.length > 1) {
      return null;
    }
    SmsUdhElement firstUdh = udh[0];
    int[] concatUdh = SmsUdhUtil.parse8BitConcatUdh(firstUdh);

    if (concatUdh == null) {
      return null;
    }

    int refNr = concatUdh[0];
    int total = concatUdh[1];
    int seqNr = concatUdh[2];
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

    int tpUdhi = in.readUnsignedByte();

    int dcs = in.readUnsignedByte();

    int msgLength = in.readInt();

    // deliver
    if (tpUdhi == 0) {

      byte[] content = new byte[msgLength];
      in.readBytes(content);
      setUserData(content, new SmsDcs((byte) dcs));
    } else {

      int udhl = in.readUnsignedByte();

      // udh exclude udhl
      byte[] udh = new byte[udhl];
      in.readBytes(udh);
      SmsUdhElement[] udhElements = SmsUdhUtil.deserialize(udh);

      setUserDataHeaders(udhElements);

      int udl = msgLength - udhl - 1;
      byte[] ud = new byte[udl];
      in.readBytes(ud);

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
        ", userDataHeader=" + Arrays.toString(getUdhBytes()) +
        ", userData=" + Arrays.toString(getUdBytes()) +
        ", spNumber='" + spNumber + '\'' +
        ", userNumber='" + userNumber + '\'' +
        ", tpPid=" + tpPid +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
