package com.github.xfslove.smssp.netty4.handler.cmpp20.subscribe;

import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;

/**
 * @author hanwen
 * created at 2018/9/3
 */
public interface DeliverConsumer {

  boolean apply(DeliverMessage deliver);

  boolean apply(DeliverMessage.Report report);
}
