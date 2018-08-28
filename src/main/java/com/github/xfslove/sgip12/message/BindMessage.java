package com.github.xfslove.sgip12.message;

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

  private String loginName;

  private String loginPassword;

  private String reserve;

  @Override
  public MessageHead getHead() {
    return head;
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
}
