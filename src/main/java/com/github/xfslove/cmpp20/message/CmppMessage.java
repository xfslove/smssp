package com.github.xfslove.cmpp20.message;

import io.netty.buffer.ByteBuf;

import java.io.Serializable;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public interface CmppMessage extends Serializable {

  MessageHead getHead();

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
