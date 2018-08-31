package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.util.StringUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ConnectRespMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_CONNECT_RESP);

  /**
   * 状态
   * 0：正确
   * 1：消息结构错
   * 2：非法源地址
   * 3：认证错
   * 4：版本太高
   * 5~ ：其他错误
   */
  private int status;

  /**
   * ISMG认证码，用于鉴别ISMG。
   * 其值通过单向MD5 hash计算得出，表示如下：
   * AuthenticatorISMG =MD5（Status+AuthenticatorSource+shared secret），Shared secret 由中国移动与源地址实体事先商定，
   * AuthenticatorSource为源地址实体发送给ISMG的对应消息CMPP_Connect中的值
   *  认证出错时，此项为空
   */
  private String authenticatorISMG;

  /**
   * 服务器支持的最高版本号
   */
  private int version;

  @Override
  public MessageHead getHead() {
    return head;
  }

  @Override
  public int getLength() {
    return 4 + 16 + 1;
  }

  @Override
  public void write(ByteBuf out) {
    // 4 bytes
    out.writeByte(getStatus());
    // 16 bytes
    out.writeBytes(StringUtil.getOctetStringBytes(getAuthenticatorISMG(), 16, StandardCharsets.ISO_8859_1));
    // 1 byte
    out.writeByte(getVersion());
  }

  @Override
  public void read(ByteBuf in) {
    setStatus(in.readUnsignedByte());
    setAuthenticatorISMG(in.readCharSequence(16, StandardCharsets.ISO_8859_1).toString().trim());
    setVersion(in.readUnsignedByte());
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getAuthenticatorISMG() {
    return authenticatorISMG;
  }

  public void setAuthenticatorISMG(String authenticatorISMG) {
    this.authenticatorISMG = authenticatorISMG;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "ConnectRespMessage{" +
        "head=" + head +
        ", status=" + status +
        ", authenticatorISMG='" + authenticatorISMG + '\'' +
        ", version=" + version +
        '}';
  }
}
