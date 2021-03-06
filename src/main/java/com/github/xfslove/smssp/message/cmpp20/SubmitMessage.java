package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.util.StringUtil;
import com.github.xfslove.smssp.exchange.Request;
import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SubmitMessage extends SmsPdu implements CmppMessage, Request {

  private final CmppHead head;

  /**
   * 信息标识，由SP侧短信网关本身产生，本处填空
   * sp 提交submit可以不需要msgId
   */
  private MsgId msgId = new MsgId(0, 0, 0, 0, 0, 0, 0);

  /**
   * 相同Msg_Id的信息总条数，从1开始
   */
  private int pkTotal = 1;

  /**
   * 相同Msg_Id的信息序号，从1开始
   */
  private int pkNumber = 1;

  /**
   * 是否要求返回状态确认报告：
   * 0：不需要
   * 1：需要
   * 2：产生SMC话单
   * （该类型短信仅供网关计费使用，不发送给目的终端)
   */
  private int registeredDelivery = 1;

  /**
   * 信息级别
   */
  private int msgLevel = 9;

  /**
   * 业务类型，是数字、字母和符号的组合
   */
  private String serviceId;

  /**
   * 计费用户类型字段
   * 0：对目的终端MSISDN计费；
   * 1：对源终端MSISDN计费；
   * 2：对SP计费;
   * 3：表示本字段无效，对谁计费参见Fee_terminal_Id字段
   */
  private int feeUserType = 2;

  /**
   * 被计费用户的号码（如本字节填空，则表示本字段无效，对谁计费参见Fee_UserType字段，本字段与Fee_UserType字段互斥）
   */
  private String feeTerminalId;

  /**
   * GSM协议类型。详细是解释请参考GSM03.40中的9.2.3.9
   */
  private int tpPid = 0;

  /**
   * 信息内容来源(SP_Id)
   */
  private String msgSrc;

  /**
   * 资费类别
   * 01：对“计费用户号码”免费
   * 02：对“计费用户号码”按条计信息费
   * 03：对“计费用户号码”按包月收取信息费
   * 04：对“计费用户号码”的信息费封顶
   * 05：对“计费用户号码”的收费是由SP实现
   */
  private String feeType = "05";

  /**
   * 资费代码（以分为单位）
   */
  private String feeCode;

  /**
   * 存活有效期，格式遵循SMPP3.3协议
   */
  private String valIdTime;

  /**
   * 定时发送时间，格式遵循SMPP3.3协议
   */
  private String atTime;

  /**
   * 源号码
   * SP的服务代码或前缀为服务代码的长号码, 网关将该号码完整的填到SMPP协议Submit_SM消息相应的source_addr字段，
   * 该号码最终在用户手机上显示为短消息的主叫号码
   */
  private String srcId;

  /**
   * 接收短信的MSISDN号码, 最多100个
   */
  private List<String> destTerminalIds = new ArrayList<>(100);

  /**
   * 保留
   */
  private String reserve;

  public SubmitMessage(int sequenceId) {
    head = new CmppHead(CmppConstants.CMPP_SUBMIT, sequenceId);
  }

  public SubmitMessage(Sequence<Integer> sequence) {
    head = new CmppHead(CmppConstants.CMPP_SUBMIT, sequence.next());
  }

  @Override
  public String getId() {
    return String.valueOf(getHead().getSequenceId());
  }

  @Override
  public CmppHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    byte[] udh = getUdhBytes();
    byte[] ud = getUdBytes();
    return getMsgId().getLength() + 1 + 1 + 1 + 1 + 10 + 1 + 21 + 1 + 1 + 1 + 6 + 2 + 6 + 17 + 17 + 21 + 1 + 21 * getDestTerminalIds().size() + 1 + udh.length + ud.length + 8;
  }

  @Override
  public void write(ByteBuf out) {
    // 8 bytes
    byte[] bytes = getMsgId().getBytes();
    out.writeBytes(bytes);
    // 1 byte
    out.writeByte(getPkTotal());
    out.writeByte(getPkNumber());
    out.writeByte(getRegisteredDelivery());
    out.writeByte(getMsgLevel());
    // 10 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getServiceId(), 10, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getFeeUserType());
    // 21 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getFeeTerminalId(), 21, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getTpPid());

    byte[] udh = getUdhBytes();
    // 1 byte
    int tpUdhi = udh.length == 0 ? 0 : 1;
    out.writeByte(tpUdhi);

    out.writeByte(getDcs().getValue());

    // 6 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getMsgSrc(), 6, StandardCharsets.ISO_8859_1));
    // 2 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getFeeType(), 2, StandardCharsets.ISO_8859_1));
    // 6 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getFeeCode(), 6, StandardCharsets.ISO_8859_1));
    // 17 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getValIdTime(), 17, StandardCharsets.ISO_8859_1));
    out.writeBytes(ByteUtil.getStringOctetBytes(getAtTime(), 17, StandardCharsets.ISO_8859_1));
    // 21 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getSrcId(), 21, StandardCharsets.ISO_8859_1));

    out.writeByte(getDestTerminalIds().size());
    for (String destTerminalId : getDestTerminalIds()) {
      out.writeBytes(ByteUtil.getStringOctetBytes(destTerminalId, 21, StandardCharsets.ISO_8859_1));
    }

    byte[] ud = getUdBytes();
    out.writeByte(udh.length + ud.length);
    out.writeBytes(udh);
    out.writeBytes(ud);
    // 8 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getReserve(), 8, StandardCharsets.ISO_8859_1));
  }

  @Override
  public void read(ByteBuf in) {
    // no need implement
  }

  public MsgId getMsgId() {
    return msgId;
  }

  public void setMsgId(MsgId msgId) {
    this.msgId = msgId;
  }

  public int getPkTotal() {
    return pkTotal;
  }

  public void setPkTotal(int pkTotal) {
    this.pkTotal = pkTotal;
  }

  public int getPkNumber() {
    return pkNumber;
  }

  public void setPkNumber(int pkNumber) {
    this.pkNumber = pkNumber;
  }

  public int getRegisteredDelivery() {
    return registeredDelivery;
  }

  public void setRegisteredDelivery(int registeredDelivery) {
    this.registeredDelivery = registeredDelivery;
  }

  public int getMsgLevel() {
    return msgLevel;
  }

  public void setMsgLevel(int msgLevel) {
    this.msgLevel = msgLevel;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public int getFeeUserType() {
    return feeUserType;
  }

  public void setFeeUserType(int feeUserType) {
    this.feeUserType = feeUserType;
  }

  public String getFeeTerminalId() {
    return feeTerminalId;
  }

  public void setFeeTerminalId(String feeTerminalId) {
    this.feeTerminalId = feeTerminalId;
  }

  public int getTpPid() {
    return tpPid;
  }

  public void setTpPid(int tpPid) {
    this.tpPid = tpPid;
  }

  public String getMsgSrc() {
    return msgSrc;
  }

  public void setMsgSrc(String msgSrc) {
    this.msgSrc = msgSrc;
  }

  public String getFeeType() {
    return feeType;
  }

  public void setFeeType(String feeType) {
    this.feeType = feeType;
  }

  public String getFeeCode() {
    return feeCode;
  }

  public void setFeeCode(String feeCode) {
    this.feeCode = feeCode;
  }

  public String getValIdTime() {
    return valIdTime;
  }

  public void setValIdTime(String valIdTime) {
    this.valIdTime = valIdTime;
  }

  public String getAtTime() {
    return atTime;
  }

  public void setAtTime(String atTime) {
    this.atTime = atTime;
  }

  public String getSrcId() {
    return srcId;
  }

  public void setSrcId(String srcId) {
    this.srcId = srcId;
  }

  public List<String> getDestTerminalIds() {
    return destTerminalIds;
  }

  public void setDestTerminalIds(List<String> destTerminalIds) {
    this.destTerminalIds = destTerminalIds;
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
        ", msgId=" + msgId +
        ", dcs=" + getDcs().getValue() +
        ", userDataHeader=" + Arrays.toString(getUdhBytes()) +
        ", userData=" + Arrays.toString(getUdBytes()) +
        ", pkTotal=" + pkTotal +
        ", pkNumber=" + pkNumber +
        ", registeredDelivery=" + registeredDelivery +
        ", msgLevel=" + msgLevel +
        ", serviceId='" + serviceId + '\'' +
        ", feeUserType=" + feeUserType +
        ", feeTerminalId='" + feeTerminalId + '\'' +
        ", tpPid=" + tpPid +
        ", msgSrc='" + msgSrc + '\'' +
        ", feeType='" + feeType + '\'' +
        ", feeCode='" + feeCode + '\'' +
        ", valIdTime='" + valIdTime + '\'' +
        ", atTime='" + atTime + '\'' +
        ", srcId='" + srcId + '\'' +
        ", destTerminalIds=" + destTerminalIds +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
