package com.rxgo;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "prompt", mixinStandardHelpOptions = true)
public class CompletionCommand implements Runnable {

  static final Logger LOG = LoggerFactory.getLogger(CompletionCommand.class);

  static final String DEFAULT_PROMPT = """
      user: Create SEO friendly title tags for "%drugname% coupon" to be used on RxGo.
      The main search term the title tag should start with is
      "%drugname% coupons" it should not say "%drugname% discounts" or "%drugname% coupons and discounts" or
      "%drugname% discounts and coupons" but it can still contain the word "discounts"
      Exclude the words "code", "guarantee" "best", "healthcare" and "therapy".
      Provide variants
      assistant: %drugname% Coupons from RxGo
      %drugname% Coupons | Save Money on Your Prescription - RxGo.com
      %drugname% Coupons | RxGo prescription discounts
      %drugname% Coupons | shop smart with RxGo's prescription discounts
      RxGo: Your Prescription Savings Hub for %drugname% Coupons
      RxGo: Your Destination for %drugname% Coupons
      %drugname% Coupons | RxGo - Your Prescription Savings Partner
      RxGo: %drugname% Coupons - Save with our discounts
      %drugname% Coupons Use RxGo for Cost-Effective Medication Discounts
      RxGo: Your Prescription Savings Destination for %drugname% Coupons
      """;

  static final String DEFAULT_MODEL = "gpt-3.5-turbo";
  static final String DEFAULT_TEMP = "0.6";
  static final int CONTEXT_SIZE = 4000;
  static final int ENOUGH_RESULTS = 100;
  static final int MAX_ITERATIONS = 100;
  static final int MAX_NORESULT_ITERATIONS = 3;
  static final Pattern STOP_WORDS_PATTERN = Pattern.compile(
      ".*\\b(code|best|guarantee|healthcare|therapy)\\b.*",
      Pattern.CASE_INSENSITIVE);

  @Inject
  @RestClient
  OpenAiChatClient openAi;

  @Parameters(paramLabel = "temperature", defaultValue = DEFAULT_TEMP, description = "temperature")
  Double temperature;

  @Parameters(paramLabel = "model", defaultValue = DEFAULT_MODEL, description = "model name")
  String model;

  @Override
  public void run() {

    final List<ChatMessage> prompt = PromptParser.parsePrompts(this.readPromptFromStdinOrDefault());
    final int firstResponseIndex = prompt.size();
    final Set<String> results = new HashSet<>(ENOUGH_RESULTS);
    long totalTokensSpent = 0;
    int noResultsIterations = 0;

    for (int i = 0; i < MAX_ITERATIONS && noResultsIterations < MAX_NORESULT_ITERATIONS; i++) {
      final ChatCompletionRequest request = prepareRequest(prompt);
      LOG.trace("\nPrompt\n{}", request);
      try {
        ChatCompletionResult response = this.openAi.createChatCompletion(request);
        var choices = responseToLines(response);
        final long iterationTokensSpent = response.getUsage().getTotalTokens();
        totalTokensSpent += iterationTokensSpent;
        // We need to remove the choices that we have already seen
        choices.removeAll(results);
        LOG.trace("\nCollected {} choices\n{}", choices.size(), choices);

        if (!choices.isEmpty()) {
          results.addAll(choices);
          noResultsIterations = 0;
        } else {
          noResultsIterations++;
        }

        LOG.debug("\nTotal results after {} iterations collected: {}", i + 1, results.size());
        if (results.size() >= ENOUGH_RESULTS) {
          break;
        }

        // feed the results back into the prompt
        if (!choices.isEmpty()) {
          String joinedResponses = choices.stream().collect(Collectors.joining("\n"));
          prompt.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), joinedResponses));
          if (iterationTokensSpent > CONTEXT_SIZE / 2) {
            // we need to remove the oldest results
            prompt.remove(firstResponseIndex);
          }
        }
      } catch (OpenAiHttpException e) {
        LOG.error("Error calling OpenAI API endpoint", e);
        noResultsIterations++;
      }
    }

    LOG.debug(
        "Total tokens spent: {} total results collected: {}",
        totalTokensSpent,
        results.size());

    saveResultsToFile(results);

  }

  ChatCompletionRequest prepareRequest(List<ChatMessage> prompt) {
    return ChatCompletionRequest
        .builder()
        .temperature(this.temperature)
        .model(this.model)
        .messages(prompt)
        .build();
  }

  Set<String> responseToLines(ChatCompletionResult response) {
    return response.getChoices()
        .stream()
        .map(ChatCompletionChoice::getMessage)
        .filter(c -> c.getRole().equals(ChatMessageRole.ASSISTANT.value()))
        .map(ChatMessage::getContent)
        .flatMap(String::lines)
        .map(String::strip)
        .filter(this::validateLine)
        .collect(Collectors.toSet());
  }

  void saveResultsToFile(Iterable<String> results) {
    try (var fileToSave = new PrintWriter(new FileWriter("titles.csv", false))) {
      results.forEach(fileToSave::println);
    } catch (IOException e) {
      LOG.error("Failed to save results", e);
    }
  }

  Readable readPromptFromStdinOrDefault() {
    if (System.console() != null) {
      // We have interactive console, use default prompt
      return new StringReader(DEFAULT_PROMPT);
    } else {
      // No interactive console, stdin is likely piped so use stdin
      return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    }
  }

  boolean validateLine(String response) {
    // use regexp to verify that response does not contain one of stop words
    return response.contains("%drugname%") &&
        !STOP_WORDS_PATTERN.matcher(response).matches();
  }

}
