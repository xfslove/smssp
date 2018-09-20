package com.github.xfslove.smssp.transport.netty4.handler;

import io.netty.util.AttributeKey;

/**
 * @author hanwen
 * created at 2018/9/20
 */
public class AttributeConstant {

  public static final AttributeKey<String> NAME = AttributeKey.valueOf("name");
  public static final AttributeKey<Boolean> SESSION = AttributeKey.valueOf("session");
}
