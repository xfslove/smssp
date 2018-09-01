package com.github.xfslove.smssp.sender;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface Sender<REQ, RESP> {

  void connect(String host, int port);

  RESP send(REQ message);

  /**
   * 不要主动去调用
   */
  void close();
}
