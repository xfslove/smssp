package com.github.xfslove.smssp.message.sgip12;

import com.github.xfslove.smssp.message.Message;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public interface SgipMessage extends Message {

  Sgip12Head getHead();
}
