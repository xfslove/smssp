package com.github.xfslove.smssp.message;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public interface Sequence<R> {

  /**
   * @return 下一个sequence
   */
  R next();
}
