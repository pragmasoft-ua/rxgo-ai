package com.rxgo;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.theokanning.openai.completion.chat.ChatMessage;

/**
 * Parses the prompts from the character stream.
 */
public class PromptParser {

  static final Pattern ROLE_PATTERN = Pattern.compile("^(user|system|assistant):\s*");

  private PromptParser() {
  }

  public static List<ChatMessage> parsePrompts(Readable charStream) {

    try (Scanner scanner = new Scanner(charStream);) {

      List<ChatMessage> prompts = new ArrayList<>();
      String role = "user";
      StringBuilder prompt = new StringBuilder(256);

      while (scanner.hasNextLine()) {
        var line = scanner.nextLine();
        var matcher = ROLE_PATTERN.matcher(line);
        if (matcher.find()) {
          if (!prompt.isEmpty()) {
            prompts.add(new ChatMessage(role, prompt.toString()));
          }
          role = matcher.group(1);
          prompt.setLength(0);
          line = line.substring(matcher.end()).trim();
        }
        if (!prompt.isEmpty()) {
          prompt.append('\n');
        }
        prompt.append(line);
      }
      if (!prompt.isEmpty()) {
        prompts.add(new ChatMessage(role, prompt.toString()));
      }
      return prompts;
    }
  }

}
