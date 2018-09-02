package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smsj.sms.SmsPdu;
import com.github.xfslove.smsj.sms.dcs.SmsDcs;
import com.github.xfslove.smsj.sms.ud.SmsUdhElement;
import com.github.xfslove.smsj.sms.ud.SmsUdhUtil;
import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.MessageHead;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverMessage extends SmsPdu implements CmppMessage {

  private final CmppHead head = new CmppHead(CmppConstants.CMPP_DELIVER);

  /**
   * 信息标识
   * 生成算法如下：
   * 采用64位（8字节）的整数：
   * 时间（格式为MMDDHHMMSS，即月日时分秒）：bit64~bit39，其中
   * bit64~bit61：月份的二进制表示；
   * bit60~bit56：日的二进制表示；
   * bit55~bit51：小时的二进制表示；
   * bit50~bit45：分的二进制表示；
   * bit44~bit39：秒的二进制表示；
   * 短信网关代码：bit38~bit17，把短信网关的代码转换为整数填写到该字段中。
   * 序列号：bit16~bit1，顺序增加，步长为1，循环使用。
   * 各部分如不能填满，左补零，右对齐
   */
  private MsgId msgId;

  /**
   * 目的号码
   * SP的服务代码，一般4--6位，或者是前缀为服务代码的长号码；该号码是手机用户短消息的被叫号码
   */
  private String destId;

  /**
   * 业务类型，是数字、字母和符号的组合
   */
  private String serviceId;

  /**
   * GSM协议类型。详细解释请参考GSM03.40中的9.2.3.9
   */
  private int tpPid;

  /**
   * GSM协议类型。详细解释请参考GSM03.40中的9.2.3.23，仅使用1位，右对齐
   */
  private int tpUdhi;

  /**
   * 源终端MSISDN号码（状态报告时填为CMPP_SUBMIT消息的目的终端号码）
   */
  private String srcTerminalId;

  /**
   * 是否为状态报告
   * 0：非状态报告
   * 1：状态报告
   */
  private int registeredDelivery;

  /**
   * 保留
   */
  private String reserve;

  @Override
  public CmppHead getHead() {
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
    setMsgId(MsgId.create(in.readLong()));
    setDestId(in.readCharSequence(21, StandardCharsets.ISO_8859_1).toString().trim());
    setServiceId(in.readCharSequence(10, StandardCharsets.ISO_8859_1).toString().trim());
    setTpPid(in.readUnsignedByte());

    int tpUdhi = in.readUnsignedByte();
    setTpUdhi(tpUdhi);

    int dcs = in.readUnsignedByte();

    setSrcTerminalId(in.readCharSequence(21, StandardCharsets.ISO_8859_1).toString().trim());
    int registeredDelivery = in.readUnsignedByte();
    setRegisteredDelivery(registeredDelivery);

    int msgLength = in.readUnsignedByte();

    // deliver or report
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

  public MsgId getMsgId() {
    return msgId;
  }

  public void setMsgId(MsgId msgId) {
    this.msgId = msgId;
  }

  public String getDestId() {
    return destId;
  }

  public void setDestId(String destId) {
    this.destId = destId;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
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

  public String getSrcTerminalId() {
    return srcTerminalId;
  }

  public void setSrcTerminalId(String srcTerminalId) {
    this.srcTerminalId = srcTerminalId;
  }

  public int getRegisteredDelivery() {
    return registeredDelivery;
  }

  public void setRegisteredDelivery(int registeredDelivery) {
    this.registeredDelivery = registeredDelivery;
  }

  public String getReserve() {
    return reserve;
  }

  public void setReserve(String reserve) {
    this.reserve = reserve;
  }

  public Report createReport() {
    return new Report();
  }

  public class Report implements Message {

    /**
     * 信息标识
     * SP提交短信（CMPP_SUBMIT）操作时，与SP相连的ISMG产生的Msg_Id
     */
    private MsgId msgId;

    /**
     * 发送短信的应答结果，含义与SMPP协议要求中stat字段定义相同，详见表一。SP根据该字段确定CMPP_SUBMIT消息的处理状态
     * DELIVERED
     * NOT_VALID
     * DELETED
     * UNDELIVERABLE
     * ACCEPTED
     * UNKNOWN
     * REJECTED
     */
    private String stat;

    /**
     * YYMMDDHHMM（YY为年的后两位00-99，MM：01-12，DD：01-31，HH：00-23，MM：00-59）
     */
    private String submitTime;

    /**
     * YYMMDDHHMM
     */
    private String doneTime;

    /**
     * 目的终端MSISDN号码(SP发送CMPP_SUBMIT消息的目标终端)
     */
    private String destTerminalId;

    /**
     * 取自SMSC发送状态报告的消息体中的消息标识
     */
    private int smscSequence;

    @Override
    public MessageHead getHead() {
      return DeliverMessage.this.getHead();
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
      setMsgId(MsgId.create(in.readLong()));
      setStat(in.readCharSequence(7, StandardCharsets.ISO_8859_1).toString().trim());
      setSubmitTime(in.readCharSequence(10, StandardCharsets.ISO_8859_1).toString().trim());
      setDoneTime(in.readCharSequence(10, StandardCharsets.ISO_8859_1).toString().trim());
      setDestTerminalId(in.readCharSequence(21, StandardCharsets.ISO_8859_1).toString().trim());
      setSmscSequence(in.readInt());
    }

    public MsgId getMsgId() {
      return msgId;
    }

    public void setMsgId(MsgId msgId) {
      this.msgId = msgId;
    }

    public String getStat() {
      return stat;
    }

    public void setStat(String stat) {
      this.stat = stat;
    }

    public String getSubmitTime() {
      return submitTime;
    }

    public void setSubmitTime(String submitTime) {
      this.submitTime = submitTime;
    }

    public String getDoneTime() {
      return doneTime;
    }

    public void setDoneTime(String doneTime) {
      this.doneTime = doneTime;
    }

    public String getDestTerminalId() {
      return destTerminalId;
    }

    public void setDestTerminalId(String destTerminalId) {
      this.destTerminalId = destTerminalId;
    }

    public int getSmscSequence() {
      return smscSequence;
    }

    public void setSmscSequence(int smscSequence) {
      this.smscSequence = smscSequence;
    }

    @Override
    public String toString() {
      return "Report{" +
          "msgId=" + msgId +
          ", stat='" + stat + '\'' +
          ", submitTime='" + submitTime + '\'' +
          ", doneTime='" + doneTime + '\'' +
          ", destTerminalId='" + destTerminalId + '\'' +
          ", smscSequence=" + smscSequence +
          '}';
    }
  }

  @Override
  public String toString() {
    return "DeliverMessage{" +
        "head=" + head +
        ", msgId=" + msgId +
        ", userDataHeader='" + ByteUtil.getString(getUdhBytes(), getDcs().getAlphabet()) + '\'' +
        ", userData='" + ByteUtil.getString(getUdBytes(), getDcs().getAlphabet()) + '\'' +
        ", destId='" + destId + '\'' +
        ", serviceId='" + serviceId + '\'' +
        ", tpPid=" + tpPid +
        ", tpUdhi=" + tpUdhi +
        ", srcTerminalId='" + srcTerminalId + '\'' +
        ", registeredDelivery=" + registeredDelivery +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
