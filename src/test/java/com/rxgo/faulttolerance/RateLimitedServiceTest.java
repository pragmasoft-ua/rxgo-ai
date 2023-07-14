package com.rxgo.faulttolerance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.faulttolerance.api.RateLimitException;
import jakarta.inject.Inject;

@QuarkusTest
public class RateLimitedServiceTest {

  @Inject
  RateLimitedService service;
  @Inject
  Logger log;
  final static ExecutorService executor = Executors.newFixedThreadPool(10);

  @BeforeEach
  public void reset() {
    this.service.reset();
  }

  @AfterAll
  public static void shutdown() {
    executor.shutdown();
  }

  @Test
  public void testRateLimited() {
    CompletableFuture.allOf(
        IntStream.range(0, 10).mapToObj(i -> CompletableFuture.runAsync(this::increment, executor))
            .toArray(CompletableFuture[]::new))
        .join();
    int counter = this.service.current();
    log.infov("Final value: {0}", counter);
    assertEquals(3, counter);
  }

  private void increment() {
    var end = System.currentTimeMillis() + 1000;
    var name = Thread.currentThread().getName();
    while (System.currentTimeMillis() < end) {
      try {
        var v = this.service.increment();
        log.infov("Incremented: {0} in {1}", v, name);
      } catch (RateLimitException e) {
        log.infov("Rate limited in {0}", name);
        try {
          Thread.sleep(10);
        } catch (InterruptedException i) {
          throw new RuntimeException("Interrupted", i);
        }
      }
    }
  }
}
