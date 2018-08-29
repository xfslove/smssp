package com.github.xfslove.cmpp20.message;

import com.github.xfslove.sms.SmsPdu;
import com.github.xfslove.util.StringUtil;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SubmitMessage extends SmsPdu implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_SUBMIT);

  /**
   * 信息标识，由SP侧短信网关本身产生，本处填空
   */
  private MsgId msgId;

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
  private int feeUserType;

  /**
   * 被计费用户的号码（如本字节填空，则表示本字段无效，对谁计费参见Fee_UserType字段，本字段与Fee_UserType字段互斥）
   */
  private String feeTerminalId;

  /**
   * GSM协议类型。详细是解释请参考GSM03.40中的9.2.3.9
   */
  private int tpPid = 0;

  /**
   * GSM协议类型。详细是解释请参考GSM03.40中的9.2.3.23,仅使用1位，右对齐
   */
  private int tpUdhi = 0;

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
  private String feeType;

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
  private List<String> destTerminalIds = new ArrayList<>();

  /**
   * 保留
   */
  private String reserve;

  @Override
  public MessageHead getHead() {
    return head;
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
    out.writeBytes(StringUtil.getOctetStringBytes(getServiceId(), 10));
    // 1 byte
    out.writeByte(getFeeUserType());
    // 21 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getFeeTerminalId(), 21));
    // 1 byte
    out.writeByte(getTpPid());
    out.writeByte(getTpUdhi());

    out.writeByte(getDcs().getValue());

    // 6 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getMsgSrc(), 6));
    // 2 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getFeeType(), 2));
    // 6 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getFeeCode(), 6));
    // 17 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getValIdTime(), 17));
    out.writeBytes(StringUtil.getOctetStringBytes(getAtTime(), 17));
    // 21 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getSrcId(), 21));

    out.writeByte(getDestTerminalIds().size());
    for (String destTerminalId : getDestTerminalIds()) {
      out.writeBytes(StringUtil.getOctetStringBytes(destTerminalId, 21));
    }

    byte[] udh = getUdhBytes();
    byte[] ud = getUdBytes();
    out.writeByte(udh.length + ud.length);
    out.writeBytes(udh);
    out.writeBytes(ud);

    // 8 bytes
    if (getReserve() != null && getReserve().length() > 0) {
      out.writeBytes(StringUtil.getOctetStringBytes(getReserve(), 8));
    }
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

  public int getTpUdhi() {
    return tpUdhi;
  }

  public void setTpUdhi(int tpUdhi) {
    this.tpUdhi = tpUdhi;
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
}
