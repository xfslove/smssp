package com.github.xfslove.smssp.dispatcher.sgip12;

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
