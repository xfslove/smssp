package com.github.xfslove.cmpp20.message;

import com.github.xfslove.util.StringUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ConnectMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_CONNECT);

  /**
   * 双方协商的版本号(高位4bit表示主版本号,低位4bit表示次版本号)
   * cmpp2.0
   */
  private final int version = 0x20;

  /**
   * 源地址，此处为SP_Id，即SP的企业代码
   */
  private String sourceAddr;

  /**
   * 用于鉴别源地址。其值通过单向MD5 hash计算得出，表示如下：
   * AuthenticatorSource =
   * MD5（Source_Addr+9 字节的0 +shared secret+timestamp）
   * Shared secret 由中国移动与源地址实体事先商定，timestamp格式为：MMDDHHMMSS，即月日时分秒，10位
   */
  private String authenticatorSource;


  /**
   * 时间戳的明文,由客户端产生,格式为MMDDHHMMSS，即月日时分秒，10位数字的整型，右对齐
   */
  private int timestamp;

  @Override
  public MessageHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    return 6 + 16 + 1 + 4;
  }

  @Override
  public void write(ByteBuf out) {
    // 6 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getSourceAddr(), 6, StandardCharsets.ISO_8859_1));
    // 16 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getAuthenticatorSource(), 16, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getVersion());
    // 4 bytes
    out.writeInt(getTimestamp());
  }

  @Override
  public void read(ByteBuf in) {
    setSourceAddr(in.readCharSequence(6, StandardCharsets.ISO_8859_1).toString().trim());
    setAuthenticatorSource(in.readCharSequence(16, StandardCharsets.ISO_8859_1).toString().trim());
    int version = in.readUnsignedByte();
    // equal with 0x20
    setTimestamp(in.readInt());
  }

  public String getSourceAddr() {
    return sourceAddr;
  }

  public void setSourceAddr(String sourceAddr) {
    this.sourceAddr = sourceAddr;
  }

  public String getAuthenticatorSource() {
    return authenticatorSource;
  }

  public void setAuthenticatorSource(String authenticatorSource) {
    this.authenticatorSource = authenticatorSource;
  }

  public int getVersion() {
    return version;
  }

  public int getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(int timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return "ConnectMessage{" +
        "head=" + head +
        ", version=" + version +
        ", sourceAddr='" + sourceAddr + '\'' +
        ", authenticatorSource='" + authenticatorSource + '\'' +
        ", timestamp=" + timestamp +
        '}';
  }
}
