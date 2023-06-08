package com.rxgo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

public class RateLimitedInterceptorTest {

  private Interceptor interceptor;
  final static int LIMIT = 3;
  final static TimeUnit UNIT = TimeUnit.SECONDS;

  @BeforeEach
  void setUp() {

    this.interceptor = new DelayQueueRateLimitingInterceptor(LIMIT, UNIT);

    // Create an OkHttpClient with the interceptor
    @SuppressWarnings("unused")
    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build();
  }

  @Test
  void testRateLimitedInterceptor() throws IOException {
    // Mock the OkHttp response for testing
    Request request = new Request.Builder().url("https://example.com").build();

    Response mockedResponse = new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(HttpURLConnection.HTTP_OK)
        .message("OK")
        .build();

    // Mock the OkHttp chain for testing
    var mockedChain = mock(okhttp3.Interceptor.Chain.class);
    when(mockedChain.request()).thenReturn(request);
    when(mockedChain.proceed(mockedChain.request())).thenReturn(mockedResponse);

    Instant start = Instant.now();
    Instant stop = start.plus(1, UNIT.toChronoUnit());

    while (Instant.now().isBefore(stop)) {
      // Check interceptor
      interceptor.intercept(mockedChain);
      System.out.printf("passed %d ms%n", Duration.between(start, Instant.now()).toMillis());
    }
    verify(mockedChain, times(LIMIT + 1)).proceed(request);
  }
}
