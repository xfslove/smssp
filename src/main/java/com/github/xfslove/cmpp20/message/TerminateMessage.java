package com.github.xfslove.cmpp20.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class TerminateMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_TERMINATE);

  @Override
  public MessageHead getHead() {
    return head;
  }
}
