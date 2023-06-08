package com.rxgo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

public class PromptParserTest {

  @Test
  public void testPromptParser() {
    String input = "user: Hello\nsystem: Hi there!\nuser: How are you?\n";

    var prompts = PromptParser.parsePrompts(new StringReader(input));

    assertEquals(3, prompts.size());

    prompts.forEach(System.out::println);
  }

  @Test
  public void testImplicitRolePrompt() {
    var prompts = PromptParser.parsePrompts(new StringReader(CompletionCommand.DEFAULT_PROMPT));

    assertEquals(2, prompts.size());
    assertEquals("user", prompts.get(0).getRole());

    prompts.forEach(System.out::println);
  }

}
