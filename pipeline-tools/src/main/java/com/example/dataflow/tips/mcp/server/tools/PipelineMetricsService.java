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

import com.google.dataflow.v1beta3.GetJobMetricsRequest;
import com.google.dataflow.v1beta3.MetricsV1Beta3Client;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import java.time.Instant;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** */
@Service
public class PipelineMetricsService {
  private final MetricsV1Beta3Client metricsClient;

  public PipelineMetricsService(MetricsV1Beta3Client metricsClient) {
    this.metricsClient = metricsClient;
  }

  @Tool(
      name = "All Job metrics",
      description =
          "Get all the metrics for the  Dataflow's job, this is a costly operation. "
              + "If the job is not running it may come back with empty results.")
  public String allJobMetrics(
      @ToolParam(description = "Job's GCP project identifier.") String projectId,
      @ToolParam(description = "Job's GCP region identifier.") String regionId,
      @ToolParam(description = "Job's identifier.") String jobId,
      ToolContext context) {

    return execute(
        () ->
            JsonFormat.printer()
                .sortingMapKeys()
                .omittingInsignificantWhitespace()
                .print(
                    metricsClient.getJobMetrics(
                        GetJobMetricsRequest.getDefaultInstance().toBuilder()
                            .setJobId(jobId)
                            .setProjectId(projectId)
                            .setLocation(regionId)
                            .setStartTime(
                                Timestamp.getDefaultInstance().toBuilder()
                                    .setSeconds(Instant.now().minusSeconds(3600).getEpochSecond())
                                    .build())
                            .build())),
        "Error while retrieving metrics for job id %s, project %s, region %s.",
        jobId,
        projectId,
        regionId);
  }
}
