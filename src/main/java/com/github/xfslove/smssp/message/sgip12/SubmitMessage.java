package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.util.StringUtil;
import com.github.xfslove.smssp.exchange.Request;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SubmitMessage extends SmsPdu implements SgipMessage, Request {

  private final SgipHead head;

  /**
   * SP的接入号码
   */
  private String spNumber;

  /**
   * 付费号码，字符，手机号码前加“86”国别标志；
   * 当且仅当群发且对用户收费时为空；如果为空，则该条短消息产生的费用由UserNumber代表的用户支付；
   * 如果为全零字符串“000000000000000000000”，表示该条短消息产生的费用由SP支付
   */
  private String chargeNumber = "000000000000000000000";

  /**
   * 一个或多个接收该短消息的手机号，手机号之间用逗号(,)隔开，字符，手机号码前加“86”国别标志，如8613001125453,8613001132345
   * 最多100个
   */
  private List<String> userNumbers = new ArrayList<>(100);

  /**
   * 企业代码，由SP定义，取值范围0-99999，字符
   */
  private String corpId;

  /**
   * 业务代码，由SP定义，字符
   */
  private String serviceType;

  /**
   * 0	“短消息类型”为“发送”，对“计费用户号码”不计信息费，此类话单仅用于核减SP对称的信道费
   * 1	对“计费用户号码”免费
   * 2	对“计费用户号码”按条计信息费
   * 3	对“计费用户号码”按包月收取信息费
   * 4	对“计费用户号码”的收费是由SP实现
   */
  private int feeType = 1;

  /**
   * 取值范围0-99999，该条短消息的收费值，单位为分，由SP定义，字符
   * 对于包月制收费的用户，该值为月租费的值
   */
  private String feeValue = "0";

  /**
   * 取值范围0-99999，赠送用户的话费，单位为分，由SP定义，特指由SP向用户发送广告时的赠送话费，字符
   */
  private String givenValue = "0";

  /**
   * 代收费标志，0：应收；1：实收，字符
   */
  private int agentFlag = 1;

  /**
   * 引起MT消息的原因
   * 0-MO点播引起的第一条MT消息；
   * 1-MO点播引起的非第一条MT消息；
   * 2-非MO点播引起的MT消息；
   * 3-系统反馈引起的MT消息。
   * 字符
   */
  private int morelatetoMTFlag = 3;

  /**
   * 优先级0-9从低到高，默认为0，十六进制数字
   */
  private int priority = 9;

  /**
   * 短消息寿命的终止时间，如果为空，表示使用短消息中心的缺省值。
   * 时间内容为16个字符，格式为“yymmddhhmmsstnnp”，其中“tnnp”取固定值“032+”，即默认系统为北京时间
   */
  private String expireTime;

  /**
   * 短消息定时发送的时间，如果为空，表示立刻发送该短消息。
   * 时间内容为16个字符，格式为“yymmddhhmmsstnnp”，其中“tnnp”取固定值“032+”，即默认系统为北京时间
   */
  private String scheduleTime;

  /**
   * 状态报告标记
   * 0-该条消息只有最后出错时要返回状态报告
   * 1-该条消息无论最后是否成功都要返回状态报告
   * 2-该条消息不需要返回状态报告
   * 3-该条消息仅携带包月计费信息，不下发给用户，要返回状态报告
   * 其它-保留
   * 缺省设置为0，十六进制数字
   */
  private int reportFlag = 1;

  /**
   * GSM协议类型。详细解释请参考GSM03.40中的9.2.3.9
   */
  private int tpPid = 0;

  /**
   * 信息类型：
   * 0-短消息信息
   * 其它：待定
   * <p>
   * 十六进制数字
   */
  private int messageType = 0;

  /**
   * 保留，扩展用
   */
  private String reserve;

  public SubmitMessage(SequenceNumber sequenceNumber) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_SUBMIT, sequenceNumber);
  }

  public SubmitMessage(Sequence<SequenceNumber> sequence) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_SUBMIT, sequence.next());
  }

  @Override
  public String getId() {
    return getHead().getSequenceNumber().stringId();
  }

  @Override
  public SgipHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    byte[] udh = getUdhBytes();
    byte[] ud = getUdBytes();
    return 21 + 21 + 1 + 21 * getUserNumbers().size() + 5 + 10 + 1 + 6 + 6 + 1 + 1 + 1 + 16 + 16 + 1 + 1 + 1 + 1 + 1 + 4 + udh.length + ud.length + 8;
  }

  @Override
  public void write(ByteBuf out) {
    // 21 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getSpNumber(), 21, StandardCharsets.ISO_8859_1));
    out.writeBytes(ByteUtil.getStringOctetBytes(getChargeNumber(), 21, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getUserNumbers().size());
    for (String userNumber : getUserNumbers()) {
      // 21 bytes
      out.writeBytes(ByteUtil.getStringOctetBytes(userNumber, 21, StandardCharsets.ISO_8859_1));
    }
    // 5 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getCorpId(), 5, StandardCharsets.ISO_8859_1));
    // 10 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getServiceType(), 10, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getFeeType());
    // 6 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getFeeValue(), 6, StandardCharsets.ISO_8859_1));
    out.writeBytes(ByteUtil.getStringOctetBytes(getGivenValue(), 6, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getAgentFlag());
    out.writeByte(getMorelatetoMTFlag());
    out.writeByte(getPriority());
    // 16 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getExpireTime(), 16, StandardCharsets.ISO_8859_1));
    out.writeBytes(ByteUtil.getStringOctetBytes(getScheduleTime(), 16, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getReportFlag());
    out.writeByte(getTpPid());

    byte[] udh = getUdhBytes();
    // 1 byte
    int tpUdhi = udh.length == 0 ? 0 : 1;
    out.writeByte(tpUdhi);

    // 1 byte
    out.writeByte(getDcs().getValue());
    out.writeByte(getMessageType());
    // 4 bytes
    byte[] ud = getUdBytes();
    out.writeInt(udh.length + ud.length);
    out.writeBytes(udh);
    out.writeBytes(ud);
    // 8 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getReserve(), 8, StandardCharsets.ISO_8859_1));
  }

  @Override
  public void read(ByteBuf in) {
    // no need implement
  }

  public String getSpNumber() {
    return spNumber;
  }

  public void setSpNumber(String spNumber) {
    this.spNumber = spNumber;
  }

  public String getChargeNumber() {
    return chargeNumber;
  }

  public void setChargeNumber(String chargeNumber) {
    this.chargeNumber = chargeNumber;
  }

  public List<String> getUserNumbers() {
    return userNumbers;
  }

  public void setUserNumbers(List<String> userNumbers) {
    this.userNumbers = userNumbers;
  }

  public String getCorpId() {
    return corpId;
  }

  public void setCorpId(String corpId) {
    this.corpId = corpId;
  }

  public String getServiceType() {
    return serviceType;
  }

  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  public int getFeeType() {
    return feeType;
  }

  public void setFeeType(int feeType) {
    this.feeType = feeType;
  }

  public String getFeeValue() {
    return feeValue;
  }

  public void setFeeValue(String feeValue) {
    this.feeValue = feeValue;
  }

  public String getGivenValue() {
    return givenValue;
  }

  public void setGivenValue(String givenValue) {
    this.givenValue = givenValue;
  }

  public int getAgentFlag() {
    return agentFlag;
  }

  public void setAgentFlag(int agentFlag) {
    this.agentFlag = agentFlag;
  }

  public int getMorelatetoMTFlag() {
    return morelatetoMTFlag;
  }

  public void setMorelatetoMTFlag(int morelatetoMTFlag) {
    this.morelatetoMTFlag = morelatetoMTFlag;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public String getExpireTime() {
    return expireTime;
  }

  public void setExpireTime(String expireTime) {
    this.expireTime = expireTime;
  }

  public String getScheduleTime() {
    return scheduleTime;
  }

  public void setScheduleTime(String scheduleTime) {
    this.scheduleTime = scheduleTime;
  }

  public int getReportFlag() {
    return reportFlag;
  }

  public void setReportFlag(int reportFlag) {
    this.reportFlag = reportFlag;
  }

  public int getTpPid() {
    return tpPid;
  }

  public void setTpPid(int tpPid) {
    this.tpPid = tpPid;
  }

  public int getMessageType() {
    return messageType;
  }

  public void setMessageType(int messageType) {
    this.messageType = messageType;
  }

  public String getReserve() {
    return reserve;
  }

  public void setReserve(String reserve) {
    this.reserve = reserve;
  }

  @Override
  public String toString() {
    return "SubmitMessage{" +
        "head=" + head +
        ", dcs=" + getDcs().getValue() +
        ", userDataHeader='" + StringUtil.bytesToHexString(getUdhBytes()) + '\'' +
        ", userData='" + StringUtil.getString(getUdBytes(), getDcs().getAlphabet(), Charset.forName("GBK")) + '\'' +
        ", spNumber='" + spNumber + '\'' +
        ", chargeNumber='" + chargeNumber + '\'' +
        ", userNumbers=" + userNumbers +
        ", corpId='" + corpId + '\'' +
        ", serviceType='" + serviceType + '\'' +
        ", feeType=" + feeType +
        ", feeValue='" + feeValue + '\'' +
        ", givenValue='" + givenValue + '\'' +
        ", agentFlag=" + agentFlag +
        ", morelatetoMTFlag=" + morelatetoMTFlag +
        ", priority=" + priority +
        ", expireTime='" + expireTime + '\'' +
        ", scheduleTime='" + scheduleTime + '\'' +
        ", reportFlag=" + reportFlag +
        ", tpPid=" + tpPid +
        ", messageType=" + messageType +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
