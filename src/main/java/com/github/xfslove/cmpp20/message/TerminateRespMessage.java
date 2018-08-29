package com.github.xfslove.cmpp20.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class TerminateRespMessage implements CmppMessage {

  private final MessageHead head = new MessageHead(CmppConstants.CMPP_TERMINATE_RESP);

  @Override
  public MessageHead getHead() {
    return head;
  }
}
