package com.github.xfslove.smssp.netty4.handler.sgip12.subscriber;

import com.github.xfslove.smssp.message.sgip12.DeliverMessage;
import com.github.xfslove.smssp.message.sgip12.ReportMessage;

/**
 * @author hanwen
 * created at 2018/9/3
 */
public interface DeliverConsumer {

  void apply(DeliverMessage deliver);

  void apply(ReportMessage report);
}
