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

  private final String name;
  private final Request request;
  private volatile Response response;
  private volatile ResponseListener listener;

  private final Lock lock = new ReentrantLock();
  private final Condition done = lock.newCondition();

  public DefaultFuture(String name, Request request, int expireSeconds) {
    this.request = request;
    this.name = name;
    CACHE.putIfAbsent(name, new DefaultCache<String, DefaultFuture>(1024, expireSeconds));
    CACHE.get(name).put(request.getId(), this);
  }

  public static DefaultFuture newFuture(String name, Request request) {
    return new DefaultFuture(name, request, 30);
  }

  public static DefaultFuture newAsyncFuture(String name, Request request, ResponseListener listener) {
    DefaultFuture future = new DefaultFuture(name, request, -1);
    future.setListener(listener);
    return future;
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

  public void setListener(ResponseListener listener) {
    if (isDone()) {
      invokeListener(listener);
    } else {
      boolean isdone = false;
      lock.lock();
      try {
        if (!isDone()) {
          this.listener = listener;
        } else {
          isdone = true;
        }
      } finally {
        lock.unlock();
      }
      if (isdone) {
        invokeListener(listener);
      }
    }
  }

  @Override
  public boolean isDone() {
    return response != null;
  }

  private void invokeListener(ResponseListener listener) {
    if (listener == null) {
      throw new NullPointerException("listener cannot be null.");
    }
    if (response == null) {
      throw new IllegalStateException("response cannot be null.");
    }
    listener.done(response);
  }

  private void receive(Response response) {
    lock.lock();
    try {
      this.response = response;
      done.signal();
    } finally {
      lock.unlock();
    }
    if (listener != null) {
      invokeListener(listener);
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

    private final String cacheKey;

    public DefaultListener(String cacheKey) {
      this.cacheKey = cacheKey;
    }

    @Override
    public void done(Response response) {
      DefaultFuture.received(cacheKey, response);
    }
  }
}
