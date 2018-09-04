package com.github.xfslove.smssp.netty4.handler.cmpp20.subscribe;

import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;

/**
 * @author hanwen
 * created at 2018/9/3
 */
public interface DeliverConsumer {

  void apply(DeliverMessage deliver);

  void apply(DeliverMessage.Report report);
}
