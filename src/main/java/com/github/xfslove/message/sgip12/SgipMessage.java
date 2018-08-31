package com.github.xfslove.message.sgip12;

import io.netty.buffer.ByteBuf;

import java.io.Serializable;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public interface SgipMessage extends Serializable {

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
