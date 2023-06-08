package com.rxgo;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class OpenAiServiceProvider {

  @Produces
  @ApplicationScoped
  public OpenAiService openAiService(@ConfigProperty(name = "openai.apikey") final String token,
      @ConfigProperty(name = "openai.timeout.seconds", defaultValue = "180") final long timeoutSeconds) {

    // 3 requests per minute
    final Interceptor rateLimiter = new DelayQueueRateLimitingInterceptor(3, TimeUnit.MINUTES);

    final OkHttpClient client = OpenAiService.defaultClient(token, Duration.ofSeconds(timeoutSeconds))
        .newBuilder()
        .addInterceptor(rateLimiter)
        .build();
    final ObjectMapper mapper = OpenAiService.defaultObjectMapper();
    final Retrofit retrofit = OpenAiService.defaultRetrofit(client, mapper);

    final OpenAiApi api = retrofit.create(OpenAiApi.class);
    return new OpenAiService(api);
  }

}
