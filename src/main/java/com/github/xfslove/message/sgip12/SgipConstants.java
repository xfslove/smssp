package com.github.xfslove.message.sgip12;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SgipConstants {

  /*
   * sgip sp使用的 command id
   */

  public static final int COMMAND_ID_BIND = 0x1;
  public static final int COMMAND_ID_BIND_RESP = 0x80000001;
  public static final int COMMAND_ID_UNBIND = 0x2;
  public static final int COMMAND_ID_UNBIND_RESP = 0x80000002;
  public static final int COMMAND_ID_SUBMIT = 0x3;
  public static final int COMMAND_ID_SUBMIT_RESP = 0x80000003;
  public static final int COMMAND_ID_DELIVER = 0x4;
  public static final int COMMAND_ID_DELIVER_RESP = 0x80000004;
  public static final int COMMAND_ID_REPORT = 0x5;
  public static final int COMMAND_ID_REPORT_RESP = 0x80000005;
}
