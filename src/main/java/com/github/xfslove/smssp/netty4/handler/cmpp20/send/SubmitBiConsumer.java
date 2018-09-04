package com.github.xfslove.smssp.netty4.handler.cmpp20.send;

import com.github.xfslove.smssp.message.cmpp20.SubmitMessage;
import com.github.xfslove.smssp.message.cmpp20.SubmitRespMessage;

/**
 * @author hanwen
 * created at 2018/9/4
 */
public interface SubmitBiConsumer {

  void apply(SubmitMessage submit, SubmitRespMessage response);
}
