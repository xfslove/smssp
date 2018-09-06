package com.github.xfslove.smssp.netty4.handler.sgip12.client;

import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public interface SubmitConsumer {

  boolean apply(SubmitRespMessage response);
}
