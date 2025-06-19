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
package com.example.dataflow.tips.tools.services;

import static com.example.dataflow.tips.tools.common.Utils.execute;

import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.dataflow.v1beta3.GetJobMetricsRequest;
import com.google.dataflow.v1beta3.MetricsV1Beta3Client;
import com.google.monitoring.v3.Aggregation;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Timestamps;
import java.time.Instant;
import java.util.List;
import java.util.stream.StreamSupport;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** */
@Service
public class PipelineMetricsService {
  private final MetricsV1Beta3Client metricsClient;
  private final MetricServiceClient gcpMetricsClient;

  public PipelineMetricsService(
      MetricsV1Beta3Client metricsClient, MetricServiceClient gcpMetricsClient) {
    this.metricsClient = metricsClient;
    this.gcpMetricsClient = gcpMetricsClient;
  }

  @Tool(
      name = "Job metrics",
      description =
          "Get the metrics for the Dataflow's job. "
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

  @Tool(
      name = "Job Workers CPU metrics",
      description =
          "Retrieves the CPU utilization metrics for all the current workers for the job.")
  public List<WorkerCpuUtilization> workerCpuUtilizationInternal(
      @ToolParam(description = "Job's GCP project identifier.") String projectId,
      @ToolParam(description = "Job's identifier.") String dataflowJobId,
      ToolContext context) {
    var timeInSecs = 300;
    return execute(
        () ->
            StreamSupport.stream(
                    gcpMetricsClient
                        .listTimeSeries(
                            ListTimeSeriesRequest.newBuilder()
                                .setName(ProjectName.of(projectId).toString())
                                .setFilter(
                                    String.format(
                                        "metric.type = \"compute.googleapis.com/instance/cpu/utilization\" AND "
                                            + "metadata.user_labels.dataflow_job_id = \"%s\"",
                                        dataflowJobId))
                                .setInterval(
                                    TimeInterval.newBuilder()
                                        .setStartTime(
                                            Timestamps.fromMillis(
                                                Instant.now()
                                                    .minusSeconds(timeInSecs)
                                                    .toEpochMilli()))
                                        .setEndTime(
                                            Timestamps.fromMillis(Instant.now().toEpochMilli()))
                                        .build())
                                .setAggregation(
                                    Aggregation.newBuilder()
                                        .setAlignmentPeriod(
                                            Duration.newBuilder().setSeconds(timeInSecs).build())
                                        .setPerSeriesAligner(Aggregation.Aligner.ALIGN_MEAN)
                                        .build())
                                .setView(ListTimeSeriesRequest.TimeSeriesView.FULL)
                                .build())
                        .iterateAll()
                        .spliterator(),
                    false)
                .flatMap(
                    ts ->
                        ts.getPointsList().stream()
                            .map(
                                point ->
                                    new WorkerCpuUtilization(
                                        ts.getMetric().getLabelsMap().get("instance_name"),
                                        point.getValue().getDoubleValue() * 100,
                                        Instant.ofEpochSecond(
                                            point.getInterval().getStartTime().getSeconds()))))
                .toList(),
        "Errors while trying to retrieve CPU metrics for jobid %s",
        dataflowJobId);
  }

  public record WorkerCpuUtilization(String name, Double utilization, Instant timestamp) {}
}
