package com.github.xfslove.smssp.subscriber;

import java.util.function.Consumer;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface Subscriber<MSG> {

  void bind(String host, int port);

  void handleMessage(Consumer<MSG> consumer);

  /**
   * 不要主动去调用
   */
  void close();

}