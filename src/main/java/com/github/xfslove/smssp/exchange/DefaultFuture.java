package com.github.xfslove.smssp.exchange;

import com.github.xfslove.smssp.cache.DefaultCache;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hanwen
 * created at 2018/9/5
 */
public class DefaultFuture implements ResponseFuture {

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(DefaultFuture.class);

  private static final ConcurrentMap<String, DefaultCache<String, DefaultFuture>> CACHE = new ConcurrentHashMap<>(64);

  private String name;
  private Request request;
  private volatile Response response;

  private final Lock lock = new ReentrantLock();
  private final Condition done = lock.newCondition();

  public DefaultFuture(String name, Request request) {
    this.request = request;
    this.name = name;
    CACHE.putIfAbsent(name, new DefaultCache<String, DefaultFuture>());
    CACHE.get(name).put(request.getId(), this);
  }

  @Override
  public Response getResponse(int timeout) throws InterruptedException {

    if (!isDone()) {
      long start = System.currentTimeMillis();
      lock.lock();
      try {
        while (!isDone()) {
          done.await(timeout, TimeUnit.MILLISECONDS);
          if (isDone() || System.currentTimeMillis() - start > timeout) {
            break;
          }
        }
      } finally {
        lock.unlock();
      }
      if (!isDone()) {
        CACHE.get(name).remove(request.getId());
        LOGGER.warn("{} drop request message {} cause by timeout", name, request);
        return null;
      }
    }
    return response;
  }

  @Override
  public boolean isDone() {
    return response != null;
  }

  private void receive(Response response) {
    lock.lock();
    try {
      this.response = response;
      done.signal();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    return "DefaultFuture{" +
        "name='" + name + '\'' +
        ", request=" + request +
        ", response=" + response +
        '}';
  }

  private static void received(String name, Response response) {
    DefaultFuture future = CACHE.get(name).remove(response.getId());

    if (future != null) {
      future.receive(response);
    } else {
      LOGGER.warn("{} drop received unrelated response message {}", name, response);
    }
  }

  public static void cleanUp(String cacheKey) {
    DefaultCache<String, DefaultFuture> cache = CACHE.remove(cacheKey);
    if (cache != null) {
      cache.cleanUp();
    }
  }

  public static class DefaultListener implements ResponseListener {

    private String cacheKey;

    public DefaultListener(String cacheKey) {
      this.cacheKey = cacheKey;
    }

    @Override
    public void done(Response response) {
      DefaultFuture.received(cacheKey, response);
    }
  }
}
