package com.github.xfslove.smssp.message;

import java.io.Serializable;

/**
 * @author hanwen
 * created at 2018/9/2
 */
public interface MessageHead extends Serializable {

  /**
   * @return 消息长度 bytes
   */
  int getLength();
}
