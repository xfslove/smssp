package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.message.Sequence;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public class DefaultSequence implements Sequence<SequenceNumber> {

  private AtomicInteger seq = new AtomicInteger(1);

  private final int nodeId;

  public DefaultSequence(int nodeId) {
    this.nodeId = nodeId;
  }

  @Override
  public SequenceNumber next() {
    int next = seq.getAndIncrement();
    if (next == 0) {
      next = seq.getAndIncrement();
    }
    int timestamp = Integer.valueOf(DateFormatUtils.format(new Date(), "MMddHHmmss"));
    return SequenceNumber.create(nodeId, timestamp, next);
  }
}
