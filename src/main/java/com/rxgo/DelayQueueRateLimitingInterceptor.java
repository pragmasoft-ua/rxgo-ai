package com.rxgo;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Response;
import java.io.InterruptedIOException;

public class DelayQueueRateLimitingInterceptor implements Interceptor {

  protected final DelayQueueRateLimiter rateLimiter;

  public DelayQueueRateLimitingInterceptor(long rate, TimeUnit unit) {
    this.rateLimiter = new DelayQueueRateLimiter(rate, unit);
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    try {
      rateLimiter.blockingAcquirePermit();
      return chain.proceed(chain.request());
    } catch (InterruptedException e) {
      throw new InterruptedIOException(e.getLocalizedMessage());
    }
  }

}
