package com.github.xfslove.smssp.handler.sgip12;

import com.github.xfslove.smssp.message.sgip12.SubmitRespMessage;
import io.netty.util.AttributeKey;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class AttributeKeyConstants {

  public static final AttributeKey<Boolean> SESSION_VALID = AttributeKey.valueOf("sessionValid");

  public static final AttributeKey<LinkedBlockingQueue<SubmitRespMessage>> RESP_QUEUE = AttributeKey.valueOf("respQueue");

}
