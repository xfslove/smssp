package com.github.xfslove.smssp.client;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

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

  private static final InternalLogger LOGGER = InternalLoggerFactory.getInstance(DefaultFuture.class);

  private static final Map<String, DefaultFuture> FUTURES = new ConcurrentHashMap<>(256);

  private Request request;
  private Response response;
  private ResponseListener listener;

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
        LOGGER.info("drop timeout request message {}", request);
        return null;
      }
    }
    return response;
  }

  @Override
  public void setListener(ResponseListener listener) {
    if (isDone()) {
      invoke(listener);
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
        invoke(listener);
      }
    }
  }

  @Override
  public boolean isDone() {
    return response != null;
  }

  private void invoke(ResponseListener listener) {
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
    if (this.listener != null) {
      invoke(this.listener);
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

  public static class DefaultConsumer implements ResponseConsumer {

    @Override
    public void apply(Response response) {
      DefaultFuture.received(response);
    }
  }
}
