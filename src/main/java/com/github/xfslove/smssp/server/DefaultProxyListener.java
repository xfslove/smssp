package com.github.xfslove.smssp.server;

import com.github.xfslove.smssp.client.DefaultFuture;
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

  private static final Cache<String, Notification> MSGS_CACHE = CacheBuilder.newBuilder().initialCapacity(256).expireAfterWrite(30, TimeUnit.MINUTES)
      .removalListener(new RemovalListener<String, Notification>() {
        @Override
        public void onRemoval(RemovalNotification<String, Notification> notification) {

          RemovalCause cause = notification.getCause();
          if (!RemovalCause.EXPLICIT.equals(cause)) {
            LOGGER.info("drop cached notification message {} cause by {}", notification.getValue(), cause);
          }

        }
      })
      .build();
  private static final ConcurrentMap<String, Notification> MSGS = MSGS_CACHE.asMap();

  private static final Cache<String, String[]> SAME_CACHE = CacheBuilder.newBuilder().initialCapacity(256).expireAfterWrite(30, TimeUnit.MINUTES).build();
  private static final ConcurrentMap<String, String[]> SAME = SAME_CACHE.asMap();

  private NotificationListener target;

  public DefaultProxyListener(NotificationListener target) {
    this.target = target;
  }

  @Override
  public void done(Notification notification) {

    Notification.Partition partition = notification.getPartition();
    if (partition == null) {
      LOGGER.info("drop received notification message {}, maybe it's not extract partition info", notification);
      return;
    }

    if (!partition.isPartOf()) {
      target.done(notification);
      return;
    }

    LOGGER.info("received notification message {}, partition {}, cache it 30m to wait other parts", notification, partition);
    MSGS.put(notification.getId(), notification);

    SAME.putIfAbsent(partition.getKey(), new String[partition.getTotal()]);
    String[] exists = SAME.get(partition.getKey());

    exists[partition.getIndex() - 1] = notification.getId();

    for (int i = 0; i < partition.getTotal(); i++) {
      String id = exists[i];
      if (id == null) {
        return;
      }
    }


    Notification full = null;
    for (int i = 0; i < partition.getTotal(); i++) {
      Notification one = MSGS.remove(exists[i]);

      if (full == null) {
        full = one;
      } else {

        if (!full.concat(one)) {
          LOGGER.info("drop message {} cause by it can't merged", notification);
        }
      }
    }

    if (full != null) {
      target.done(full);
    }

  }

}
