package com.github.xfslove.smssp.netty4.handler.cmpp20.subscriber;

import com.github.xfslove.smssp.message.Message;
import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;
import com.github.xfslove.smssp.message.cmpp20.DeliverRespMessage;
import com.github.xfslove.smssp.netty4.SessionEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.logging.LogLevel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.function.Consumer;

/**
 * todo concatmessage holder
 * smg -&gt; sp 消息的handler
 *
 * @author hanwen
 * created at 2018/8/31
 */
@ChannelHandler.Sharable
public class SubscriberMessageHandler extends ChannelDuplexHandler {

  private Consumer<Message> consumer;

  private final InternalLogger logger;
  private final InternalLogLevel internalLevel;

  public SubscriberMessageHandler(Consumer<Message> consumer, LogLevel level) {
    this.consumer = consumer;
    logger = InternalLoggerFactory.getInstance(getClass());
    internalLevel = level.toInternalLevel();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    if (msg instanceof DeliverMessage) {

      DeliverMessage deliver = (DeliverMessage) msg;

      DeliverRespMessage deliverResp = new DeliverRespMessage();
      deliverResp.setMsgId(deliver.getMsgId());
      deliverResp.setResult(0);

      ctx.writeAndFlush(deliverResp);

      if (deliver.getRegisteredDelivery() == 1) {
        // 状态报告
        ByteBuf in = Unpooled.wrappedBuffer(deliver.getUdBytes());
        DeliverMessage.Report report = deliver.createReport();
        report.read(in);
        consumer.accept(report);

        ReferenceCountUtil.release(in);
        return;
      }

      consumer.accept(deliver);
      return;
    }

    logger.log(internalLevel, "received unknown cmpp message {}, drop it", msg);
    ReferenceCountUtil.release(msg);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    if (evt instanceof SessionEvent) {

      SessionEvent sEvt = (SessionEvent) evt;

      Message msg = sEvt.getMessage();

      if (msg instanceof DeliverMessage) {

        // 需要先登录
        DeliverRespMessage deliverResp = new DeliverRespMessage();
        deliverResp.setMsgId(((DeliverMessage) msg).getMsgId());
        deliverResp.setResult(9);

        ctx.writeAndFlush(deliverResp).addListener(listener -> {
          ctx.channel().close();
          logger.log(internalLevel, "discard[NOT_VALID] deliver message {}, channel closed", msg);
        });

        return;
      }

      logger.log(internalLevel, "received unknown cmpp message {}, drop it", msg);
      ReferenceCountUtil.release(msg);
      return;
    }

    ctx.fireUserEventTriggered(evt);

  }
}
