package com.rxgo;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "prompt", mixinStandardHelpOptions = true)
public class CompletionCommand implements Runnable {

  static final Logger LOG = LoggerFactory.getLogger(CompletionCommand.class);

  static final String DEFAULT_PROMPT = """
      user: Create SEO friendly title tags for "%drugname% coupon" to be used on RxGo.
      The two main search terms the title tag should always start with either
      "%drugname% coupons" or "%drugname% discounts".
      Exclude the words "code", "guarantee" and "best".
      Provide variants
      assistant: %drugname% Discounts and Coupons from RxGo
      %drugname% Coupons | Save Money on Your Prescription - RxGo.com
      %drugname% Discounts and Coupons | RxGo
      %drugname% Discounts and Coupons | shop smart with RxGo
      """;

  static final String DEFAULT_MODEL = "gpt-3.5-turbo";
  static final int MAX_TOKENS = 4000;
  static final int MAX_RESULTS = 100;

  final OpenAiService openAi;
  final PrintStream output;
  final Set<String> result;

  /**
   * @param openAi
   */
  public CompletionCommand(OpenAiService openAi) {
    this.openAi = openAi;
    this.output = System.out;
    this.result = new HashSet<>(MAX_RESULTS);
  }

  @Parameters(paramLabel = "temperature", defaultValue = "0.6", description = "temperature")
  Double temperature;

  @Parameters(paramLabel = "model", defaultValue = DEFAULT_MODEL, description = "model name")
  String model;

  @Override
  public void run() {
    long iterationTokens = MAX_TOKENS / 4;
    long totalTokens = 0;
    List<ChatMessage> originalPrompt = PromptParser.parsePrompts(this.readPromptFromStdinOrDefault());
    List<ChatMessage> prompt = List.copyOf(originalPrompt);
    for (int i = 0, converged = 0; i < MAX_RESULTS && converged < 3; i++) {
      ChatCompletionRequest request = ChatCompletionRequest
          .builder()
          .temperature(this.temperature)
          .model(this.model)
          .messages(prompt)
          .maxTokens((int) (MAX_TOKENS - iterationTokens))
          .build();
      LOG.debug("\nPrompt\n{}", request);
      ChatCompletionResult response = this.openAi.createChatCompletion(request);
      var choices = response.getChoices()
          .stream()
          .map(ChatCompletionChoice::getMessage)
          .filter(c -> c.getRole().equals(ChatMessageRole.ASSISTANT.value()))
          .map(ChatMessage::getContent)
          .flatMap(String::lines)
          .map(String::strip)
          .filter(this::validate)
          .collect(Collectors.toSet());
      iterationTokens = response.getUsage().getTotalTokens();
      totalTokens += iterationTokens;
      choices.removeAll(this.result);
      LOG.debug("\nCollected {} choices\n{}", choices.size(), choices);

      if (!choices.isEmpty()) {
        this.result.addAll(choices);
        converged = 0;
      } else {
        converged++;
      }

      LOG.debug("\nTotal results after {} iterations collected: {}", i + 1, this.result.size());
      if (this.result.size() >= MAX_RESULTS) {
        break;
      }

      if (!choices.isEmpty()) {
        String joinedResponses = this.result.stream().collect(Collectors.joining("\n"));
        prompt = new ArrayList<>(originalPrompt);
        prompt.add(new ChatMessage(ChatMessageRole.ASSISTANT.value(), joinedResponses));
      }
    }
    LOG.info("Total tokens spent: {} total results collected: {}", totalTokens, this.result.size());
    try (var fileToSave = new PrintWriter(new FileWriter("titles.csv", false))) {
      this.result.forEach(fileToSave::println);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private Readable readPromptFromStdinOrDefault() {
    if (System.console() != null) {
      // We have interactive console, use default prompt
      return new StringReader(DEFAULT_PROMPT);
    } else {
      // No interactive console, stdin is likely piped so use stdin
      return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

    }
  }

  private boolean validate(String response) {
    // TODO implement validation logic
    return true;
  }

}
