package com.github.xfslove.smssp.server;

import com.github.xfslove.smsj.sms.ud.SmsUdhElement;
import com.github.xfslove.smsj.sms.ud.SmsUdhIei;
import com.github.xfslove.smssp.client.DefaultFuture;
import com.github.xfslove.smssp.message.cmpp20.DeliverMessage;
import com.github.xfslove.smssp.util.ByteUtil;
import com.google.common.cache.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/8
 */
public class DefaultProxyListener implements NotificationListener {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(DefaultFuture.class);

  private static final Cache<String, DeliverMessage> MSGS_CACHE = CacheBuilder.newBuilder().initialCapacity(256).expireAfterWrite(30, TimeUnit.MINUTES)
      .removalListener(new RemovalListener<String, DeliverMessage>() {
        @Override
        public void onRemoval(RemovalNotification<String, DeliverMessage> notification) {

          RemovalCause cause = notification.getCause();
          if (!RemovalCause.EXPLICIT.equals(cause)) {
            LOGGER.info("drop cached message {} cause by {}", notification.getValue(), cause);
          }

        }
      })
      .build();
  private static final ConcurrentMap<String, DeliverMessage> MSGS = MSGS_CACHE.asMap();

  private static final Cache<String, String[]> ONE_IDS_CACHE = CacheBuilder.newBuilder().initialCapacity(256).expireAfterWrite(30, TimeUnit.MINUTES).build();
  private static final ConcurrentMap<String, String[]> IDS = ONE_IDS_CACHE.asMap();

  private NotificationListener target;

  public DefaultProxyListener(NotificationListener target) {
    this.target = target;
  }

  @Override
  public void done(Notification notification) {

    if (notification instanceof DeliverMessage) {
      DeliverMessage deliver = (DeliverMessage) notification;

      if (deliver.getTpUdhi() == 0) {
        target.done(notification);
        return;
      }

      SmsUdhElement[] udh = deliver.getUserDateHeaders();

      if (udh.length > 1) {
        LOGGER.info("drop received deliver message {} cause by it's not sms", deliver);
        return;
      }

      SmsUdhElement firstUdh = udh[0];
      if (!SmsUdhIei.APP_PORT_8BIT.equals(firstUdh.getUdhIei())) {
        LOGGER.info("drop received deliver message {} cause by it's not concat udh", deliver);
        return;
      }

      int refNr = firstUdh.getUdhIeiData()[0] & 0xff;
      int total = firstUdh.getUdhIeiData()[1] & 0xff;
      int seqNr = firstUdh.getUdhIeiData()[2] & 0xff;
      String key = deliver.getSrcTerminalId() + "-" + refNr;

      LOGGER.info("received deliver message {}, parts of [id:{} | idx:{}] , cache it 30 mins", deliver, key, seqNr);
      MSGS.put(deliver.getId(), deliver);

      IDS.putIfAbsent(key, new String[total]);
      String[] exists = IDS.get(key);

      exists[seqNr - 1] = deliver.getId();

      for (int i = 0; i < total; i++) {
        String id = exists[i];
        if (id == null) {
          return;
        }
      }


      DeliverMessage full = null;
      for (int i = 0; i < total; i++) {
        DeliverMessage one = MSGS.remove(exists[i]);

        if (full == null) {
          full = one;
        } else {

          if (full.getDcs().getValue() != one.getDcs().getValue()) {
            LOGGER.info("drop message [id:{}] contains different dcs[{}, {}] cause by it can't merged", key, full.getDcs().getValue(), one.getDcs().getValue());
            break;
          }

          full.setUserData(ByteUtil.concat(full.getUdBytes(), one.getUdBytes()), full.getDcs());
        }
      }

      if (full != null) {
        target.done(full);
      }

    } else {
      target.done(notification);
    }

  }

}
