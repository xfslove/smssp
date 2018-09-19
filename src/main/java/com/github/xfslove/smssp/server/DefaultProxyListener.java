package com.github.xfslove.smssp.server;

import com.github.xfslove.smssp.cache.DefaultCache;
import com.github.xfslove.smssp.client.DefaultFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author hanwen
 * created at 2018/9/8
 */
public class DefaultProxyListener implements NotificationListener {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(DefaultFuture.class);

  private static final ConcurrentMap<String, DefaultCache<String, Notification>> MSG_CACHE = new ConcurrentHashMap<>(64);
  private static final ConcurrentMap<String, DefaultCache<String, String[]>> MERGE_CACHE = new ConcurrentHashMap<>(64);

  private String cacheKey;
  private NotificationListener target;

  public DefaultProxyListener(String cacheKey, NotificationListener target) {
    this.target = target;
    this.cacheKey = cacheKey;
    MSG_CACHE.putIfAbsent(cacheKey, new DefaultCache<String, Notification>());
    MERGE_CACHE.putIfAbsent(cacheKey, new DefaultCache<String, String[]>());
  }

  @Override
  public void done(Notification notification) {

    Notification.Partition partition = notification.getPartition();
    if (partition == null) {
      LOGGER.warn("drop received notification message {}, maybe it's not extract partition info", notification);
      return;
    }

    if (!partition.isPartOf()) {
      target.done(notification);
      return;
    }

    LOGGER.info("received notification message {}, partition {}, cache it 30m to wait other parts", notification, partition);
    MSG_CACHE.get(cacheKey).put(notification.getId(), notification);

    MERGE_CACHE.get(cacheKey).putIfAbsent(partition.getKey(), new String[partition.getTotal()]);
    String[] exists = MERGE_CACHE.get(cacheKey).get(partition.getKey());

    exists[partition.getIndex() - 1] = notification.getId();

    for (int i = 0; i < partition.getTotal(); i++) {
      String id = exists[i];
      if (id == null) {
        return;
      }
    }


    Notification full = null;
    for (int i = 0; i < partition.getTotal(); i++) {
      Notification one = MSG_CACHE.get(cacheKey).remove(exists[i]);

      if (full == null) {
        full = one;
      } else {

        if (!full.concat(one)) {
          LOGGER.warn("drop message {} cause by it can't merged", notification);
        }
      }
    }

    if (full != null) {
      target.done(full);
    }

  }

  public static void cleanUp(String cacheKey) {
    DefaultCache<String, Notification> cache = MSG_CACHE.remove(cacheKey);
    if (cache != null) {
      cache.cleanUp();
    }
    DefaultCache<String, String[]> cache1 = MERGE_CACHE.get(cacheKey);
    if (cache1 != null) {
      cache1.cleanUp();
    }
  }
}
