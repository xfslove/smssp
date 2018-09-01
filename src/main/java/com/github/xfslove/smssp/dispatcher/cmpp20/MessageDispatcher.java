package com.github.xfslove.smssp.dispatcher.cmpp20;

import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface MessageDispatcher {

  void deliver(DeliverMessage deliver);

  void report(DeliverMessage.Report report);
}
