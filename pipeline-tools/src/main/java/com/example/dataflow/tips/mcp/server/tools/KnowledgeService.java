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
package com.example.dataflow.tips.mcp.server.tools;

import com.example.dataflow.tips.common.Utils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** */
@Service
public class KnowledgeService {

  private final Map<String, List<String>> knowledgeBase =
      Map.of(
          "PubSub",
          List.of(
              "A data lag of less than 60 seconds si considered normal in a PubSub reading pipeline."));

  private final List<String> sourceCategories = List.of("PubSub", "GCS", "BigQuery");
  private final List<String> sinkCategories = List.of("PubSub", "GCS", "BigQuery");

  @Tool(
      name = "Best Practices: Sources",
      description =
          "Retrieve the known best practices for the provided Apache Beam source category. "
              + "Use the 'IO Categories' tool to retrieve what best practices are available.")
  public List<String> sourceBestPractices(
      @ToolParam(description = "Source category.") String sourceCategory, ToolContext context) {
    return Utils.execute(
        () -> Optional.ofNullable(knowledgeBase.get(sourceCategory)).orElse(List.of()),
        "Error retrieving source categories best practices",
        sourceCategory);
  }

  @Tool(
      name = "Best Practices: Sinks",
      description =
          "Retrieve the known best practices for the provided Apache Beam sink category. "
              + "Use the 'IO Categories' tool to retrieve what best practices are available.")
  public List<String> sinkBestPractices(
      @ToolParam(description = "Sink category.") String sinkCategory, ToolContext context) {
    return Utils.execute(
        () -> Optional.ofNullable(knowledgeBase.get(sinkCategory)).orElse(List.of()),
        "Error retrieving sink categories best practices",
        sinkCategory);
  }

  @Tool(
      name = "IO Categories",
      description =
          "Retrieves the known IO (sources and sinks) categories covered in this knowledge base.")
  public IoCategories ioCategories(ToolContext context) {
    return Utils.execute(
        () -> new IoCategories(sourceCategories, sinkCategories),
        "Error retrieving the IO categories",
        "");
  }

  public record IoCategories(List<String> sources, List<String> sinks) {}
}
