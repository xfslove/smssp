package com.github.xfslove.sgip12.message;

import com.github.xfslove.util.StringUtil;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class BindMessage implements SgipMessage {

  private final MessageHead head = new MessageHead(SgipConstants.COMMAND_ID_BIND);

  /**
   * 登录类型
   * 1：SP向SMG建立的连接，用于发送命令
   * 2：SMG向SP建立的连接，用于发送命令
   * 3：SMG之间建立的连接，用于转发命令
   * 4：SMG向GNS建立的连接，用于路由表的检索和维护
   * 5：GNS向SMG建立的连接，用于路由表的更新
   * 6：主备GNS之间建立的连接，用于主备路由表的一致性
   * 11：SP与SMG以及SMG之间建立的测试连接，用于跟踪测试
   * 其它：保留
   */
  private int loginType = 1;

  /**
   * 服务器端给客户端分配的登录名
   */
  private String loginName;

  /**
   * 服务器端和Login Name对应的密码
   */
  private String loginPassword;

  /**
   * 保留
   */
  private String reserve;

  @Override
  public MessageHead getHead() {
    return head;
  }

  @Override
  public void write(ByteBuf out) {
    out.writeByte(getLoginType());
    out.writeBytes(StringUtil.getOctetStringBytes(getLoginName(), 16, StandardCharsets.ISO_8859_1));
    out.writeBytes(StringUtil.getOctetStringBytes(getLoginPassword(), 16, StandardCharsets.ISO_8859_1));
    out.writeBytes(StringUtil.getOctetStringBytes(getReserve(), 8, StandardCharsets.ISO_8859_1));
  }

  @Override
  public void read(ByteBuf in) {
    setLoginType(in.readUnsignedByte());
    setLoginName(in.readCharSequence(16, StandardCharsets.ISO_8859_1).toString().trim());
    setLoginPassword(in.readCharSequence(16, StandardCharsets.ISO_8859_1).toString().trim());
    setReserve(in.readCharSequence(8, StandardCharsets.ISO_8859_1).toString().trim());
  }

  public int getLoginType() {
    return loginType;
  }

  public void setLoginType(int loginType) {
    this.loginType = loginType;
  }

  public String getLoginName() {
    return loginName;
  }

  public void setLoginName(String loginName) {
    this.loginName = loginName;
  }

  public String getLoginPassword() {
    return loginPassword;
  }

  public void setLoginPassword(String loginPassword) {
    this.loginPassword = loginPassword;
  }

  public String getReserve() {
    return reserve;
  }

  public void setReserve(String reserve) {
    this.reserve = reserve;
  }

  @Override
  public String toString() {
    return "BindMessage{" +
        "head=" + head +
        ", loginType=" + loginType +
        ", loginName='" + loginName + '\'' +
        ", reserve='" + reserve + '\'' +
        '}';
  }
}
