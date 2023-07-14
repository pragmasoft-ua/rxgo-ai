package com.rxgo.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.logging.Logger;

import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RateLimitedService {

  @Inject
  Logger log;
  private final AtomicInteger counter = new AtomicInteger(0);

  @RateLimit(value = 3, window = 1, windowUnit = java.time.temporal.ChronoUnit.SECONDS)
  public int increment() {
    var value = counter.incrementAndGet();
    log.infov("Incremented to {0}", value);
    return value;
  }

  public void reset() {
    counter.set(0);
    log.infov("Reset");
  }

  public int current() {
    return counter.get();
  }

}
