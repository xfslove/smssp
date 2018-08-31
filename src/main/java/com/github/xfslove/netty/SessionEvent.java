package com.github.xfslove.netty;

/**
 * @author hanwen
 * created at 2018/8/31
 */
public class SessionEvent {

  /**
   * session还为注册
   */
  public static SessionEvent NOT_VALID(Object message) {
    return new SessionEvent("NOT_VALID", message);
  }

  private String state;

  private Object message;

  SessionEvent(String state, Object message) {
    this.state = state;
    this.message = message;
  }

  public String getState() {
    return state;
  }

  public Object getMessage() {
    return message;
  }
}
