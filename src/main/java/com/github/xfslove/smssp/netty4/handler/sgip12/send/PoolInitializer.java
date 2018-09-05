package com.github.xfslove.smssp.netty4.handler.sgip12.send;

import com.github.xfslove.smssp.message.sequence.SequenceGenerator;
import com.github.xfslove.smssp.netty4.codec.MesssageLengthCodec;
import com.github.xfslove.smssp.netty4.codec.sgip12.MessageCodec;
import com.github.xfslove.smssp.netty4.handler.ExceptionHandler;
import com.github.xfslove.smssp.netty4.handler.sgip12.UnBindHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/1
 */
@ChannelHandler.Sharable
public class PoolInitializer implements ChannelPoolHandler {

  private final LogLevel logLevel = LogLevel.INFO;

  private int idleCheckInterval = 5 * 60;

  private int nodeId;

  private String loginName;

  private String loginPassword;

  private SubmitBiConsumer submitBiConsumer;

  private SequenceGenerator sequenceGenerator;

  public PoolInitializer(int nodeId, String loginName, String loginPassword, SubmitBiConsumer submitBiConsumer, SequenceGenerator sequenceGenerator) {
    this.nodeId = nodeId;
    this.loginName = loginName;
    this.loginPassword = loginPassword;
    this.submitBiConsumer = submitBiConsumer;
    this.sequenceGenerator = sequenceGenerator;
  }

  @Override
  public void channelReleased(Channel channel) throws Exception {
    // nothing
  }

  @Override
  public void channelAcquired(Channel channel) throws Exception {
    // nothing
  }

  @Override
  public void channelCreated(Channel channel) throws Exception {

    channel.pipeline().addLast("sgipSocketLogging", new LoggingHandler(LogLevel.INFO));
    channel.pipeline().addLast("sgipIdleState", new IdleStateHandler(0, 0, idleCheckInterval, TimeUnit.SECONDS));
    channel.pipeline().addLast("sgipMessageLengthCodec", new MesssageLengthCodec(true));
    channel.pipeline().addLast("sgipMessageCodec", new MessageCodec());
    channel.pipeline().addLast("sgipMessageLogging", new LoggingHandler(logLevel));

    channel.pipeline().addLast("sgipBindHandler", new BindHandler(nodeId, loginName, loginPassword, sequenceGenerator, logLevel));
    channel.pipeline().addLast("sgipUnBindHandler", new UnBindHandler(nodeId, loginName, sequenceGenerator, logLevel));
    channel.pipeline().addLast("sgipSubmitHandler", new SubmitHandler(nodeId, loginName, submitBiConsumer, sequenceGenerator, logLevel));
    channel.pipeline().addLast("sgipException", new ExceptionHandler(loginName, logLevel));
  }
}
