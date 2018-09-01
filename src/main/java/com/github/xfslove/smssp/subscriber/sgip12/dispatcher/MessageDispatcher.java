package com.github.xfslove.smssp.subscriber.sgip12.dispatcher;

import com.github.xfslove.smssp.message.sgip12.DeliverMessage;
import com.github.xfslove.smssp.message.sgip12.ReportMessage;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface MessageDispatcher {

  void deliver(DeliverMessage deliver);

  void report(ReportMessage report);
}
