package com.rxgo;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ResteasyReactiveClientRequestFilter;

import jakarta.ws.rs.ext.Provider;

@Provider
public class RateLimitingFilter implements ResteasyReactiveClientRequestFilter {

  final DelayQueueRateLimiter rateLimiter;

  public RateLimitingFilter(@ConfigProperty(name = "openai.rpm", defaultValue = "60") final long rpm) {
    this.rateLimiter = new DelayQueueRateLimiter(rpm, TimeUnit.MINUTES);
  }

  @Override
  public void filter(ResteasyReactiveClientRequestContext requestContext) {
    try {
      requestContext.suspend();
      rateLimiter.blockingAcquirePermit();
      requestContext.resume();
    } catch (InterruptedException e) {
      requestContext.resume(e);
    }
  }

}
