package com.rxgo;

import java.io.IOException;
import java.io.InterruptedIOException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import okhttp3.Interceptor;
import okhttp3.Response;

public class Bucket4jRateLimitingInterceptor implements Interceptor {

  protected final Bucket rateLimiter;

  public Bucket4jRateLimitingInterceptor(Bandwidth rate) {
    this.rateLimiter = Bucket.builder()
        .addLimit(rate)
        .build();
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    try {
      rateLimiter.asBlocking().consume(1);
      return chain.proceed(chain.request());
    } catch (InterruptedException e) {
      throw new InterruptedIOException(e.getLocalizedMessage());
    }
  }

}
