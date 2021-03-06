package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.message.Sequence;
import com.github.xfslove.smssp.util.ByteUtil;
import io.netty.buffer.ByteBuf;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ConnectRespMessage implements CmppMessage {

  private final CmppHead head;

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
  private byte[] authenticatorISMG;

  /**
   * 服务器支持的最高版本号
   */
  private int version;

  public ConnectRespMessage(int sequenceId) {
     head = new CmppHead(CmppConstants.CMPP_CONNECT_RESP, sequenceId);
  }

  public ConnectRespMessage(Sequence<Integer> sequence) {
    head = new CmppHead(CmppConstants.CMPP_CONNECT_RESP, sequence.next());
  }

  @Override
  public CmppHead getHead() {
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
    out.writeBytes(ByteUtil.getOctetBytes(getAuthenticatorISMG(), 16));
    // 1 byte
    out.writeByte(getVersion());
  }

  @Override
  public void read(ByteBuf in) {
    setStatus(in.readUnsignedByte());
    byte[] bytes = new byte[16];
    in.readBytes(bytes);
    setAuthenticatorISMG(bytes);
    setVersion(in.readUnsignedByte());
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public byte[] getAuthenticatorISMG() {
    return authenticatorISMG;
  }

  public void setAuthenticatorISMG(byte[] authenticatorISMG) {
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
        ", version=" + version +
        '}';
  }
}
