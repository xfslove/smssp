package com.github.xfslove.smssp.message.sequence;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public interface Sequence {

  /**
   * @return 下一个sequence
   */
  int next();
}
