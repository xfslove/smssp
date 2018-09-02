package com.github.xfslove.smssp.handler;

import io.netty.util.AttributeKey;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public class AttributeKeyConstants {

  public static final AttributeKey<Boolean> SESSION_VALID = AttributeKey.valueOf("sessionValid");

  public static final AttributeKey<LinkedBlockingQueue> RESP_QUEUE = AttributeKey.valueOf("respQueue");

}