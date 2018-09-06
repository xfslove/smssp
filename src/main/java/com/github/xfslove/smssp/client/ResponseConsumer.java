package com.github.xfslove.smssp.client;

/**
 * @author hanwen
 * created at 2018/9/6
 */
public interface ResponseConsumer {

  /**
   * 处理response
   *
   * @param response response
   * @return true if success
   */
  boolean apply(Response response);
}
