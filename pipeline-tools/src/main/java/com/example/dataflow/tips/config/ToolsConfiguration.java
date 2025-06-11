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
package com.example.dataflow.tips.config;

import com.example.dataflow.tips.mcp.server.tools.PipelineTopologyService;
import com.google.dataflow.v1beta3.JobsV1Beta3Client;
import java.io.IOException;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** */
@Configuration
public class ToolsConfiguration {

  @Bean
  public ToolCallbackProvider pipelineTopology(PipelineTopologyService topologyService) {
    return MethodToolCallbackProvider.builder().toolObjects(topologyService).build();
  }

  @Bean
  public JobsV1Beta3Client jobsClient() throws IOException {
    return JobsV1Beta3Client.create();
  }
}
