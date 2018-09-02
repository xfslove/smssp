package com.github.xfslove.smssp.message.cmpp20;

import com.github.xfslove.smssp.message.Message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public interface CmppMessage extends Message {

  @Override
  CmppHead getHead();
}
