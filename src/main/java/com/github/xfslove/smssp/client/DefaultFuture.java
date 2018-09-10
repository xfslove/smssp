package com.github.xfslove.smssp.client;

import com.google.common.cache.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

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

  private static final Cache<String, DefaultFuture> FUTURES_CACHE = CacheBuilder.newBuilder().initialCapacity(256).expireAfterAccess(30, TimeUnit.SECONDS)
      .removalListener(new RemovalListener<String, DefaultFuture>() {
        @Override
        public void onRemoval(RemovalNotification<String, DefaultFuture> notification) {

          RemovalCause cause = notification.getCause();
          if (!RemovalCause.EXPLICIT.equals(cause)) {
            LOGGER.info("drop cached future {} cause by {}", notification.getValue().request, cause);
          }

        }
      })
      .build();
  private static final ConcurrentMap<String, DefaultFuture> FUTURES = FUTURES_CACHE.asMap();

  private Request request;
  private Response response;

  private final Lock lock = new ReentrantLock();
  private final Condition done = lock.newCondition();

  public DefaultFuture(Request request) {
    this.request = request;
    FUTURES.put(request.getId(), this);
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
        FUTURES.remove(request.getId());
        LOGGER.info("drop request message {} cause by timeout", request);
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

  private static void received(Response response) {
    DefaultFuture future = FUTURES.remove(response.getId());

    if (future != null) {
      future.receive(response);
    } else {
      LOGGER.info("drop received unrelated response message {}", response);
    }
  }

  public static class DefaultListener implements ResponseListener {

    @Override
    public void done(Response response) {
      DefaultFuture.received(response);
    }
  }
}
