package com.github.xfslove.smssp.netty4.handler.sgip12.send;

import com.github.xfslove.smssp.message.sgip12.SubmitMessage;
import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public interface SubmitBiConsumer {

  void apply(SubmitMessage submit, SubmitRespMessage response);
}
