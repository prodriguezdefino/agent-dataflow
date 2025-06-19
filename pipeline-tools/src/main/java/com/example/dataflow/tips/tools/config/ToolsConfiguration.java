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
package com.example.dataflow.tips.tools.config;

import com.example.dataflow.tips.tools.services.KnowledgeService;
import com.example.dataflow.tips.tools.services.LogMessagesService;
import com.example.dataflow.tips.tools.services.PipelineMetricsService;
import com.example.dataflow.tips.tools.services.PipelineTopologyService;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.dataflow.v1beta3.JobsV1Beta3Client;
import com.google.dataflow.v1beta3.MessagesV1Beta3Client;
import com.google.dataflow.v1beta3.MetricsV1Beta3Client;
import java.io.IOException;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** */
@Configuration
@EnableConfigurationProperties({KnowledgeProperties.class})
public class ToolsConfiguration {

  @Bean
  public ToolCallbackProvider pipelineTopology(
      PipelineTopologyService topologyService,
      PipelineMetricsService metricsService,
      KnowledgeService knowService,
      LogMessagesService logsService) {
    return MethodToolCallbackProvider.builder()
        .toolObjects(topologyService, metricsService, knowService, logsService)
        .build();
  }

  @Bean
  public JobsV1Beta3Client jobsClient() throws IOException {
    return JobsV1Beta3Client.create();
  }

  @Bean
  public MetricsV1Beta3Client metricsClient() throws IOException {
    return MetricsV1Beta3Client.create();
  }

  @Bean
  public MessagesV1Beta3Client logsClient() throws IOException {
    return MessagesV1Beta3Client.create();
  }

  @Bean
  public MetricServiceClient gcpMetricsClient() throws IOException {
    return MetricServiceClient.create();
  }
}
