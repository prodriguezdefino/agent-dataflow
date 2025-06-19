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

import static com.example.dataflow.tips.common.Utils.execute;

import com.example.dataflow.tips.config.KnowledgeProperties;
import com.example.dataflow.tips.config.KnowledgeProperties.IoCategories;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** */
@Service
public class KnowledgeService {

  private final KnowledgeProperties knowledge;

  public KnowledgeService(KnowledgeProperties knowledge) {
    this.knowledge = knowledge;
  }

  @Tool(
      name = "Best Practices: Sources",
      description =
          "Retrieve the known best practices for the provided Apache Beam source category. "
              + "Use the 'IO Categories' tool to retrieve what best practices are available.")
  public List<String> sourceBestPractices(
      @ToolParam(description = "Source category.") String sourceCategory, ToolContext context) {
    return execute(
        () -> knowledge.bestPractice(sourceCategory),
        "Error retrieving source %s categories best practices.",
        sourceCategory);
  }

  @Tool(
      name = "Best Practices: Sinks",
      description =
          "Retrieve the known best practices for the provided Apache Beam sink category. "
              + "Use the 'IO Categories' tool to retrieve what best practices are available.")
  public List<String> sinkBestPractices(
      @ToolParam(description = "Sink category.") String sinkCategory, ToolContext context) {
    return execute(
        () -> knowledge.bestPractice(sinkCategory),
        "Error retrieving sink %s categories best practices.",
        sinkCategory);
  }

  @Tool(
      name = "IO Categories",
      description =
          "Retrieves the known IO (sources and sinks) categories covered in this knowledge base.")
  public IoCategories ioCategories(ToolContext context) {
    return execute(() -> knowledge.ioCategories(), "Error retrieving the IO categories.", "");
  }
}
