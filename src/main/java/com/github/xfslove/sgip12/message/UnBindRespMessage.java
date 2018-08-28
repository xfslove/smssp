package com.github.xfslove.sgip12.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class UnBindRespMessage implements SgipMessage {

  private final MessageHead head = new MessageHead(SgipConstants.COMMAND_ID_UNBIND_RESP);

  @Override
  public MessageHead getHead() {
    return head;
  }
}
