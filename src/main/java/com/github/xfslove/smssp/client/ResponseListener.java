package com.github.xfslove.smssp.client;

/**
 * @author hanwen
 * created at 2018/9/6
 */
public interface ResponseListener {

  /**
   * 处理smg的response
   *
   * @param response response
   */
  void done(Response response);
}
