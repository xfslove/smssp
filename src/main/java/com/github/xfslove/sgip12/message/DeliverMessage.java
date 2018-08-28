package com.github.xfslove.sgip12.message;

import com.github.xfslove.sms.SmsMessage;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverMessage implements SgipMessage {

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

  /**
   * 消息内容
   */
  private SmsMessage message;

  @Override
  public MessageHead getHead() {
    return head;
  }

  public SmsMessage getMessage() {
    return message;
  }

  public void setMessage(SmsMessage message) {
    this.message = message;
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
}
