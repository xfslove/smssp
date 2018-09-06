package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.client.Response;
import com.github.xfslove.smssp.message.sequence.Sequence;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SubmitRespMessage implements SgipMessage, Response {

  private final SgipHead head;

  /**
   * Submit命令是否成功接收
   * 0：接收成功
   * 其它：错误码参考http://baijiahao.baidu.com/s?id=1554746383651964&wfr=spider&for=pc
   */
  private int result = 0;

  private String reserve;

  public SubmitRespMessage(SequenceNumber sequenceNumber) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_SUBMIT_RESP, sequenceNumber);
  }

  public SubmitRespMessage(Sequence sequence) {
    this.head = new SgipHead(SgipConstants.COMMAND_ID_SUBMIT_RESP, (SequenceNumber) sequence.next());
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
    // no need implement
    return -1;
  }

  @Override
  public void write(ByteBuf out) {
    // no need implement
  }

  @Override
  public void read(ByteBuf in) {
    setResult(in.readUnsignedByte());
    setReserve(in.readCharSequence(8, StandardCharsets.ISO_8859_1).toString().trim());
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
    return "SubmitRespMessage{" +
        "head=" + head +
        ", result=" + result +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
