package com.github.xfslove.smssp.netty4.handler;

import com.github.xfslove.smssp.message.Message;
import io.netty.util.AttributeKey;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class AttributeKeyConstants {

  public static final AttributeKey<Boolean> SESSION_VALID = AttributeKey.valueOf("sessionValid");

  public static final AttributeKey<LinkedBlockingQueue<Message>> RESP_QUEUE = AttributeKey.valueOf("respQueue");

}
