package com.github.xfslove.netty.codec.sgip12;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * sgip1.2 消息长度codec，应该放在最前面
 *
 * @author hanwen
 * created at 2018/8/28
 */
public class MesssageLengthCodec extends LengthFieldBasedFrameDecoder {

  public MesssageLengthCodec(boolean failfast) {
    // header first 4 bytes is message length
    // & include message length to downstream codec
    super(4 * 1024, 0, 4, -4, 0, failfast);
  }
}
