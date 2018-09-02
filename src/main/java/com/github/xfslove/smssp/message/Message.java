package com.github.xfslove.smssp.message;

import io.netty.buffer.ByteBuf;

import java.io.Serializable;

/**
 * @author hanwen
 * created at 2018/9/2
 */
public interface Message extends Serializable {

  /**
   * @return 消息头
   */
  MessageHead getHead();

  /**
   * @return 消息长度 bytes
   */
  int getLength();

  /**
   * 把 message body 写到 buf
   *
   * @param out buf
   */
  void write(ByteBuf out);

  /**
   * 从 buf 读 message body
   *
   * @param in buf
   */
  void read(ByteBuf in);
}
