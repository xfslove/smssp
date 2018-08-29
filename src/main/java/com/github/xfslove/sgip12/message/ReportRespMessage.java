package com.github.xfslove.sgip12.message;

import com.github.xfslove.util.StringUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ReportRespMessage implements SgipMessage {

  private final MessageHead head = new MessageHead(SgipConstants.COMMAND_ID_REPORT_RESP);

  /**
   * Submit命令是否成功接收
   * 0：接收成功
   * 其它：错误码参考http://baijiahao.baidu.com/s?id=1554746383651964&wfr=spider&for=pc
   */
  private int result = 0;

  /**
   * 保留，扩展用
   */
  private String reserve;

  @Override
  public MessageHead getHead() {
    return head;
  }

  @Override
  public void write(ByteBuf out) {
    // 1 byte
    out.writeByte(result);
    // 8 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(reserve, 8, StandardCharsets.ISO_8859_1));
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
}
