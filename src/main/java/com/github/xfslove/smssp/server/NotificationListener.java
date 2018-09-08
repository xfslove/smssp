package com.github.xfslove.smssp.server;

/**
 * @author hanwen
 * created at 2018/9/8
 */
public interface NotificationListener {

  /**
   * 处理smg的通知
   *
   * @param notification notification
   */
  void done(Notification notification);
}
