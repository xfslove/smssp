package com.github.xfslove.smssp.subscriber;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.MessageProtocol;

import java.util.function.Consumer;

/**
 * @author hanwen
 * created at 2018/9/1
 */
public interface Subscriber {

  void bind(MessageProtocol protocol, String host, int port);

  /**
   * @param consumer 如何处理收到的消息
   */
  void handleMessage(Consumer<Message> consumer);

  /**
   * 不要主动去调用
   */
  void close();

}
