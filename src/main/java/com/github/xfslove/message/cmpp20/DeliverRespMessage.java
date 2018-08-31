package com.github.xfslove.message.cmpp20;

import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverRespMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_DELIVER_RESP);

  /**
   * 信息标识，生成算法如下：
   * 采用64位（8字节）的整数：
   * 时间（格式为MMDDHHMMSS，即月日时分秒）：bit64~bit39，其中
   * bit64~bit61：月份的二进制表示；
   * bit60~bit56：日的二进制表示；
   * bit55~bit51：小时的二进制表示；
   * bit50~bit45：分的二进制表示；
   * bit44~bit39：秒的二进制表示；
   * 短信网关代码：bit38~bit17，把短信网关的代码转换为整数填写到该字段中。
   * 序列号：bit16~bit1，顺序增加，步长为1，循环使用。
   * 各部分如不能填满，左补零，右对齐。
   * <p>
   * （SP根据请求和应答消息的Sequence_Id一致性就可得到CMPP_Submit消息的Msg_Id）
   */
  private MsgId msgId;

  /**
   * 结果
   * 0：正确
   * 1：消息结构错
   * 2：命令字错
   * 3：消息序号重复
   * 4：消息长度错
   * 5：资费代码错
   * 6：超过最大信息长
   * 7：业务代码错
   * 8：流量控制错
   * 9~ ：其他错误码参考http://baijiahao.baidu.com/s?id=1554746383651964&wfr=spider&for=pc
   */
  private int result;

  @Override
  public MessageHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    return 8 + 1;
  }

  @Override
  public void write(ByteBuf out) {
    // 8 bytes
    byte[] bytes = getMsgId().getBytes();
    out.writeBytes(bytes);
    // 1 byte
    out.writeByte(getResult());
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

  public int getResult() {
    return result;
  }

  public void setResult(int result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return "DeliverRespMessage{" +
        "head=" + head +
        ", msgId=" + msgId +
        ", result=" + result +
        '}';
  }
}
