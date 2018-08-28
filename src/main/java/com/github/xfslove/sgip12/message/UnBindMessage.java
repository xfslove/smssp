package com.github.xfslove.sgip12.message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class UnBindMessage implements SgipMessage {

  private final MessageHead head = new MessageHead(SgipConstants.COMMAND_ID_UNBIND);

  @Override
  public MessageHead getHead() {
    return head;
  }
}
