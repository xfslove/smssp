package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class BindRespMessage implements SgipMessage {

  private final SgipHead head = new SgipHead(SgipConstants.COMMAND_ID_BIND_RESP);

  /**
   * Bind命令是否成功接收
   * 0：接收成功
   * 其它：错误码参考http://baijiahao.baidu.com/s?id=1554746383651964&wfr=spider&for=pc
   */
  private int result = 0;

  private String reserve;

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
    return "BindRespMessage{" +
        "head=" + head +
        ", result=" + result +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
