package com.github.xfslove.smssp.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author hanwen
 * created at 2018/9/19
 */
public class DefaultCache<K, V> {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(DefaultCache.class);

  private final com.google.common.cache.Cache<K, V> cache;
  private final ConcurrentMap<K, V> map;

  public DefaultCache(int initCapacity, int expireSeconds) {
    cache = CacheBuilder.newBuilder().initialCapacity(initCapacity).expireAfterAccess(expireSeconds, TimeUnit.SECONDS)
        .removalListener(new RemovalListener<K, V>() {
          @Override
          public void onRemoval(RemovalNotification<K, V> notification) {

            RemovalCause cause = notification.getCause();
            if (!RemovalCause.EXPLICIT.equals(cause)) {
              LOGGER.warn("drop cache {} cause by {}", notification.getValue(), cause);
            }

          }
        })
        .build();
    map = cache.asMap();
  }

  public V putIfAbsent(K key, V value) {
    return map.putIfAbsent(key, value);
  }

  public V put(K key, V value) {
    return map.put(key, value);
  }

  public V remove(K key) {
    return map.remove(key);
  }

  public V get(K key) {
    return map.get(key);
  }

  public void cleanUp() {
    cache.cleanUp();
  }
}
