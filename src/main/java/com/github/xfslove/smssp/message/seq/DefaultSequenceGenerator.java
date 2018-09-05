package com.github.xfslove.smssp.message.seq;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public class DefaultSequenceGenerator implements SequenceGenerator {

  private AtomicInteger seq = new AtomicInteger(1);

  @Override
  public int next() {
    return seq.getAndIncrement();
  }
}
