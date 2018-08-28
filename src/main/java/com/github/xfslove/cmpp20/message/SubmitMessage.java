package com.github.xfslove.cmpp20.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class SubmitMessage implements CmppMessage {

  private MessageHead head = new MessageHead(CmppConstants.CMPP_SUBMIT);

  @Override
  public MessageHead getHead() {
    return head;
  }
}
