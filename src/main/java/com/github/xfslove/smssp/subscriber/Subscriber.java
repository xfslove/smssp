package com.github.xfslove.smssp.subscriber;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface Subscriber {

  void bind(String host, int port);

  /**
   * 不要主动去调用
   */
  void close();

}
