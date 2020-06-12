package com.github.xfslove.smssp.transport.netty4.handler;

import com.github.xfslove.smssp.message.Message;

/**
 * @author hanwen
 * created at 2018/8/31
 */
public class SessionEvent {

  /**
   * @param message message
   * @return session还为注册
   */
  public static SessionEvent NOT_VALID(Message message) {
    return new SessionEvent("NOT_VALID", message);
  }

  private String state;

  private Message message;

  SessionEvent(String state, Message message) {
    this.state = state;
    this.message = message;
  }

  public String getState() {
    return state;
  }

  public Message getMessage() {
    return message;
  }
}
