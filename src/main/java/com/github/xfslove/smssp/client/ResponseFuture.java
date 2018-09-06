package com.github.xfslove.smssp.client;

/**
 * @author hanwen
 * created at 2018/9/5
 */
public interface ResponseFuture {

  /**
   * 异步获取submitResp
   *
   * @param timeout timeout
   * @return Response
   * @throws InterruptedException ex
   */
  Response getResponse(int timeout) throws InterruptedException;

  /**
   * isDone
   *
   * @return true isDone
   */
  boolean isDone();
}
