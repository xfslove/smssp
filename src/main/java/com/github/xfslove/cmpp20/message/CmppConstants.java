package com.github.xfslove.cmpp20.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class CmppConstants {

  /*
   * cmpp sp使用的 command id
   */

  public static final int CMPP_CONNECT = 0x00000001;
  public static final int CMPP_CONNECT_RESP = 0x80000001;
  public static final int CMPP_TERMINATE = 0x00000002;
  public static final int CMPP_TERMINATE_RESP = 0x80000002;
  public static final int CMPP_SUBMIT = 0x00000004;
  public static final int CMPP_SUBMIT_RESP = 0x80000004;
  public static final int CMPP_DELIVER = 0x00000005;
  public static final int CMPP_DELIVER_RESP = 0x80000005;
}
