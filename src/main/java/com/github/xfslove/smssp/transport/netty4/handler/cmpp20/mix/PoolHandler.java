package com.github.xfslove.smssp.transport.netty4.handler.cmpp20.mix;

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;

/**
 * @author hanwen
 * created at 2018/9/7
 */
public class PoolHandler extends AbstractChannelPoolHandler {

  private HandlerInitializer initializer;

  public PoolHandler(HandlerInitializer initializer) {
    this.initializer = initializer;
  }

  @Override
  public void channelCreated(Channel ch) throws Exception {
    initializer.initChannel(ch);
  }
}
