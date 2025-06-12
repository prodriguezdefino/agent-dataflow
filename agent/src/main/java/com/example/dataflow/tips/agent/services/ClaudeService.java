/*
 * Copyright (C) 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.dataflow.tips.agent.services;

import io.modelcontextprotocol.client.McpAsyncClient;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Handles interactions with the Claude LLM. This class is responsible for managing the lifecycle of
 * MCP (Model Context Protocol) clients, preparing the chat prompts, and streaming the AI-generated
 * responses.
 */
@Service
public class ClaudeService {

  private final ChatClient.Builder chatClientBuilder;
  private final ObjectProvider<List<McpAsyncClient>> mcpClientListProvider;
  private final SystemPromptTemplate systemPrompt;

  public ClaudeService(
      ChatClient.Builder chatClientBuilder,
      ObjectProvider<List<McpAsyncClient>> mcpClientListProvider,
      SystemPromptTemplate systemPrompt) {
    this.chatClientBuilder = chatClientBuilder;
    this.mcpClientListProvider = mcpClientListProvider;
    this.systemPrompt = systemPrompt;
  }

  Mono<List<McpAsyncClient>> prepareClients() {
    return Mono.fromCallable(mcpClientListProvider::getObject)
        .subscribeOn(Schedulers.boundedElastic());
  }

  Mono<Boolean> cleanup(List<McpAsyncClient> clients) {
    return Mono.fromCallable(
        () -> {
          clients.stream().map(client -> client.closeGracefully()).toList();
          return true;
        });
  }

  /**
   * Generates a response from the Claude LLM based on the given message and message history.
   *
   * <p>Manages the lifecycle of MCP clients for this specific generation request, ensuring they are
   * initialized before use and cleaned up afterwards.
   *
   * @param message The current user message to send to the AI.
   * @param history A list of previous messages in the conversation history.
   * @return A Flux<String> that streams the AI-generated response content.
   */
  public Flux<String> generate(String message, List<Message> history) {
    return Flux.usingWhen(
        // McpClients Initialization (resourceAsync)
        prepareClients(),
        // MacpClients usage for Chat client as tools (resourceClosure)
        mcpAsyncClients ->
            this.chatClientBuilder
                .clone()
                .defaultToolCallbacks(new AsyncMcpToolCallbackProvider(mcpAsyncClients))
                .build()
                .prompt(
                    new Prompt(
                        Stream.of(
                                List.<Message>of(new UserMessage(message)),
                                history,
                                List.of(systemPrompt.createMessage()))
                            .flatMap(List::stream)
                            .toList()))
                .toolContext(Map.of())
                .stream()
                .content(),
        // McpClients cleanup (asyncCleanup)
        mcpAsyncClients -> cleanup(mcpAsyncClients));
  }
}
