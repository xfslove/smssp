package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverRespMessage implements SgipMessage {

  private final SgipHead head;

  /**
   * Deliver命令是否成功接收
   * 0：接收成功
   * 其它：错误码<a href="http://baijiahao.baidu.com/s?id=1554746383651964&amp;wfr=spider&amp;for=pc">连接</a>
   */
  private int result = 0;

  /**
   * 保留，扩展用
   */
  private String reserve;


  public DeliverRespMessage(SequenceNumber sequenceNumber) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_DELIVER_RESP, sequenceNumber);
  }

  public DeliverRespMessage(Sequence<SequenceNumber> sequence) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_DELIVER_RESP, sequence.next());
  }

  @Override
  public SgipHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    return 1 + 8;
  }

  @Override
  public void write(ByteBuf out) {
    // 1 byte
    out.writeByte(result);
    // 8 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(reserve, 8, StandardCharsets.ISO_8859_1));
  }

  @Override
  public void read(ByteBuf in) {
    // no need implement
  }

  public int getResult() {
    return result;
  }

  public void setResult(int result) {
    this.result = result;
  }

  public String getReserve() {
    return reserve;
  }

  public void setReserve(String reserve) {
    this.reserve = reserve;
  }

  @Override
  public String toString() {
    return "DeliverRespMessage{" +
        "head=" + head +
        ", result=" + result +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
