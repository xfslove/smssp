package com.github.xfslove.smssp.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author hanwen
 * created at 2018/9/5
 */
public class DefaultFuture implements ResponseFuture {

  private static final Map<String, DefaultFuture> FUTURES = new ConcurrentHashMap<>(512);

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

  public static boolean received(Response response) {
    DefaultFuture future = FUTURES.remove(response.getId());

    if (future != null) {
      future.receive(response);
      return true;
    }

    return false;
  }

  public static class DefaultConsumer implements ResponseConsumer {

    @Override
    public boolean apply(Response response) {
      return DefaultFuture.received(response);
    }
  }
}
