package com.github.xfslove.smssp.sender;

import com.github.xfslove.smssp.message.sgip12.SgipMessage;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface Sender {

  void connect(String host, int port);

  SgipMessage send(SgipMessage message);

  /**
   * 不要主动去调用
   */
  void close();
}
