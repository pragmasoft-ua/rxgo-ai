package com.rxgo;

import java.util.concurrent.Delayed;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RateLimiter is a class that limits the rate at which a particular function
 * can be called.
 * 
 */
public class DelayQueueRateLimiter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DelayQueueRateLimiter.class);

  private final DelayQueue<DelayedPermit> delayQueue;
  private final long rateLimit;
  private final TimeUnit timeUnit;
  private long delayNanos;

  public DelayQueueRateLimiter(long rateLimit, TimeUnit timeUnit) {
    this.rateLimit = rateLimit;
    this.timeUnit = timeUnit;
    this.delayNanos = timeUnit.toNanos(1) / rateLimit;
    this.delayQueue = new DelayQueue<>();
    final var permit = new DelayedPermit(System.nanoTime());
    // Place one permit
    this.delayQueue.put(permit);
    LOGGER.trace("Constructed; Stored permit: {}", permit);
  }

  @Override
  public String toString() {
    return "RateLimiter [" + this.rateLimit + " per " + this.timeUnit + "]. Has " + this.delayQueue.size()
        + " permits.";
  }

  /**
   * Blocks until a permit is available.
   * 
   * @throws InterruptedException
   */
  public void blockingAcquirePermit() throws InterruptedException {
    var permit = this.delayQueue.take();
    LOGGER.trace("Acquired permit: {}", permit);
    permit = new DelayedPermit(System.nanoTime() + this.delayNanos);
    this.delayQueue.put(permit);
    LOGGER.trace("Stored permit: {}", permit);
  }

  private record DelayedPermit(long expirationTimeNanos) implements Delayed {

    @Override
    public long getDelay(TimeUnit unit) {
      return unit.convert(this.expirationTimeNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
      return Long.compare(this.expirationTimeNanos, ((DelayedPermit) other).expirationTimeNanos);
    }
  }
}
