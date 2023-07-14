package com.rxgo;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("v1")
@RegisterRestClient(configKey = "openai-api")
@RegisterProvider(RateLimitingFilter.class)
@ClientHeaderParam(name = "Authorization", value = "Bearer ${openai.apikey}")
public interface OpenAiChatClient {

  @POST
  @Path("chat/completions")
  ChatCompletionResult createChatCompletion(ChatCompletionRequest request);

}