package com.github.xfslove.smssp.message.sequence;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public class DefaultCmppSequence implements Sequence<Integer> {

  private AtomicInteger seq = new AtomicInteger(1);

  @Override
  public Integer next() {
    int next = seq.getAndIncrement();
    if (next == 0) {
      next = seq.getAndIncrement();
    }
    return next;
  }
}
