package com.github.xfslove.smssp.sender;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.MessageProtocol;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface Sender {

  void connect(MessageProtocol protocol, String host, int port);

  Message send(Message message);

  /**
   * 不要主动去调用
   */
  void close();
}
