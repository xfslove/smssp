package com.github.xfslove.cmpp20.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ConnectMessage implements CmppMessage {

  private MessageHead head = new MessageHead(CmppConstants.CMPP_CONNECT);

  @Override
  public MessageHead getHead() {
    return head;
  }
}
