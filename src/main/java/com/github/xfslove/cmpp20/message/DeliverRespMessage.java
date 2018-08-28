package com.github.xfslove.cmpp20.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class DeliverRespMessage implements CmppMessage {

  private MessageHead head = new MessageHead(CmppConstants.CMPP_DELIVER_RESP);

  @Override
  public MessageHead getHead() {
    return head;
  }
}
