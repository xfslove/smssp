package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ConnectMessage implements CmppMessage {

  public static final int VERSION_2_0 = 0x20;

  private final CmppHead head;

  /**
   * 双方协商的版本号(高位4bit表示主版本号,低位4bit表示次版本号)
   * cmpp2.0
   */
  private final int version = VERSION_2_0;

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
  private byte[] authenticatorSource;


  /**
   * 时间戳的明文,由客户端产生,格式为MMDDHHMMSS，即月日时分秒，10位数字的整型，右对齐
   */
  private int timestamp;

  public ConnectMessage(int sequenceId) {
    head = new CmppHead(CmppConstants.CMPP_CONNECT, sequenceId);
  }

  public ConnectMessage(Sequence<Integer> sequence) {
    head = new CmppHead(CmppConstants.CMPP_CONNECT, sequence.next());
  }

  @Override
  public CmppHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    return 6 + 16 + 1 + 4;
  }

  @Override
  public void write(ByteBuf out) {
    // 6 bytes
    out.writeBytes(ByteUtil.getStringOctetBytes(getSourceAddr(), 6, StandardCharsets.ISO_8859_1));
    // 16 bytes
    out.writeBytes(ByteUtil.getOctetBytes(getAuthenticatorSource(), 16));
    // 1 byte
    out.writeByte(getVersion());
    // 4 bytes
    out.writeInt(getTimestamp());
  }

  @Override
  public void read(ByteBuf in) {
    setSourceAddr(in.readCharSequence(6, StandardCharsets.ISO_8859_1).toString().trim());
    byte[] bytes = new byte[16];
    in.readBytes(bytes);
    setAuthenticatorSource(bytes);
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

  public byte[] getAuthenticatorSource() {
    return authenticatorSource;
  }

  public void setAuthenticatorSource(byte[] authenticatorSource) {
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
        ", timestamp=" + timestamp +
        '}';
  }
}
