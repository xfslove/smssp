package com.github.xfslove.netty;

import com.github.xfslove.message.sgip12.SgipMessage;

/**
 * @author hanwen
 * created at 2018/8/31
 */
public class SessionEvent {

  /**
   * session还为注册
   */
  public static SessionEvent NOT_VALID(SgipMessage message) {
    return new SessionEvent("NOT_VALID", message);
  }

  private String state;

  private SgipMessage message;

  SessionEvent(String state, SgipMessage message) {
    this.state = state;
    this.message = message;
  }

  public String getState() {
    return state;
  }

  public SgipMessage getMessage() {
    return message;
  }
}
