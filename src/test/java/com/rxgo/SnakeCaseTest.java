package com.rxgo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class SnakeCaseTest {

  @Inject
  ObjectMapper mapper;
  @Inject
  Logger log;

  @Test
  public void testSnakeCase() throws JsonProcessingException {
    var request = ChatCompletionRequest
        .builder()
        .model("model")
        .temperature(1.0)
        .frequencyPenalty(2.0)
        .maxTokens(100)
        .build();
    var json = mapper.writeValueAsString(request);
    log.info(json);
    assertTrue(json.contains("frequency_penalty"));
  }

}
