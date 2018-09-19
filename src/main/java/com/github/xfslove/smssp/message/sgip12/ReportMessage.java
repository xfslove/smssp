package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.server.Notification;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ReportMessage implements SgipMessage, Notification {

  private final SgipHead head;

  /**
   * 该命令所涉及的Submit或deliver命令的序列号
   */
  private SequenceNumber submitSequenceNumber;

  /**
   * Report命令类型
   * 0：对先前一条Submit命令的状态报告
   * 1：对先前一条前转Deliver命令的状态报告
   */
  private int reportType;

  /**
   * 接收短消息的手机号，手机号码前加“86”国别标志
   */
  private String userNumber;

  /**
   * 该命令所涉及的短消息的当前执行状态
   * 0：发送成功
   * 1：等待发送
   * 2：发送失败
   */
  private int state = 0;

  /**
   * 当State=2时为错误码值，否则为0
   */
  private int errorCode = 0;

  /**
   * 保留，扩展用
   */
  private String reserve;

  public ReportMessage(SequenceNumber sequenceNumber) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_REPORT, sequenceNumber);
  }

  public ReportMessage(Sequence<SequenceNumber> sequence) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_REPORT, sequence.next());
  }

  @Override
  public String getId() {
    return getHead().getSequenceNumber().stringId();
  }

  @Override
  public Partition getPartition() {
    return new Partition(1, 1, null);
  }

  @Override
  public boolean concat(Notification next) {
    // no need implement
    return false;
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
    int nodeId = in.readInt();
    int timestamp = in.readInt();
    int sequenceId = in.readInt();
    setSubmitSequenceNumber(SequenceNumber.create(nodeId, timestamp, sequenceId));

    setReportType(in.readUnsignedByte());
    setUserNumber(in.readCharSequence(21, StandardCharsets.ISO_8859_1).toString().trim());
    setState(in.readUnsignedByte());
    setErrorCode(in.readUnsignedByte());

    setReserve(in.readCharSequence(8, StandardCharsets.ISO_8859_1).toString().trim());
  }

  public SequenceNumber getSubmitSequenceNumber() {
    return submitSequenceNumber;
  }

  public void setSubmitSequenceNumber(SequenceNumber submitSequenceNumber) {
    this.submitSequenceNumber = submitSequenceNumber;
  }

  public int getReportType() {
    return reportType;
  }

  public void setReportType(int reportType) {
    this.reportType = reportType;
  }

  public String getUserNumber() {
    return userNumber;
  }

  public void setUserNumber(String userNumber) {
    this.userNumber = userNumber;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public int getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(int errorCode) {
    this.errorCode = errorCode;
  }

  public String getReserve() {
    return reserve;
  }

  public void setReserve(String reserve) {
    this.reserve = reserve;
  }

  @Override
  public String toString() {
    return "ReportMessage{" +
        "head=" + head +
        ", submitSequenceNumber=" + submitSequenceNumber +
        ", reportType=" + reportType +
        ", userNumber='" + userNumber + '\'' +
        ", state=" + state +
        ", errorCode=" + errorCode +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
