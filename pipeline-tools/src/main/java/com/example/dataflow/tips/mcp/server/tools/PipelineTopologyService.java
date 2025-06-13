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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.dataflow.v1beta3.DisplayData;
import com.google.dataflow.v1beta3.GetJobRequest;
import com.google.dataflow.v1beta3.JobView;
import com.google.dataflow.v1beta3.JobsV1Beta3Client;
import com.google.dataflow.v1beta3.ListJobsRequest;
import com.google.dataflow.v1beta3.TransformSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** */
@Service
public class PipelineTopologyService {
  private final JobsV1Beta3Client jobsClient;

  public PipelineTopologyService(JobsV1Beta3Client jobsClient) {
    this.jobsClient = jobsClient;
  }

  @Tool(
      name = "Job Details",
      description =
          "Get Dataflow's job detailed information, including topology structure, stages, "
              + "types, metadata, experiments in use.")
  public Pipeline jobDetails(
      @ToolParam(description = "Job's GCP project identifier.") String projectId,
      @ToolParam(description = "Job's GCP region identifier.") String regionId,
      @ToolParam(description = "Job's identifier.") String jobId,
      ToolContext context) {
    return execute(
        () -> {
          var job =
              jobsClient.getJob(
                  GetJobRequest.getDefaultInstance().toBuilder()
                      .setJobId(jobId.trim())
                      .setProjectId(projectId.trim())
                      .setLocation(regionId.trim())
                      .setView(JobView.JOB_VIEW_ALL)
                      .build());
          return new Pipeline(
              job.getName(),
              job.getProjectId(),
              job.getLocation(),
              job.getId(),
              job.getType().toString(),
              job.getCurrentState().toString(),
              Instant.ofEpochSecond(job.getStartTime().getSeconds()),
              job.getEnvironment().getExperimentsList(),
              job.getEnvironment().getWorkerPoolsList().stream()
                  .findFirst()
                  .map(wp -> wp.getMachineType())
                  .orElse("NA"),
              toTransform(job.getPipelineDescription().getOriginalPipelineTransformList()),
              toDisplayData(job.getPipelineDescription().getDisplayDataList()));
        },
        "Error while retrieving information for job id: %s, project: %s, region: %s",
        jobId,
        projectId,
        regionId);
  }

  @Tool(
      name = "Job List For Project",
      description = "Get Dataflow's jobs executed in a GCP project.")
  public List<Pipeline> pipelines(
      @ToolParam(description = "Pipelines GCP project identifier.") String projectId,
      ToolContext context) {
    return execute(
        () ->
            StreamSupport.stream(
                    jobsClient
                        .listJobs(
                            ListJobsRequest.getDefaultInstance().toBuilder()
                                .setProjectId(projectId.trim())
                                .setPageSize(10)
                                .build())
                        .getPage()
                        .iterateAll()
                        .spliterator(),
                    false)
                .map(
                    job ->
                        new Pipeline(
                            job.getName(),
                            job.getProjectId(),
                            job.getLocation(),
                            job.getId(),
                            job.getType().toString(),
                            job.getCurrentState().toString(),
                            Instant.ofEpochSecond(job.getStartTime().getSeconds())))
                .toList(),
        "Error while retrieving pipelines for project: %s",
        projectId);
  }

  @Tool(
      name = "Job List For Project and Region",
      description = "Get Dataflow's jobs executed in a GCP project and region.")
  public List<Pipeline> pipelines(
      @ToolParam(description = "Job's GCP project identifier.") String projectId,
      @ToolParam(description = "Job's GCP region identifier.") String regionId,
      ToolContext context) {
    return execute(
        () ->
            StreamSupport.stream(
                    jobsClient
                        .listJobs(
                            ListJobsRequest.getDefaultInstance().toBuilder()
                                .setProjectId(projectId.trim())
                                .setLocation(regionId.trim())
                                .setPageSize(10)
                                .build())
                        .getPage()
                        .iterateAll()
                        .spliterator(),
                    false)
                .map(
                    job ->
                        new Pipeline(
                            job.getName(),
                            job.getProjectId(),
                            job.getLocation(),
                            job.getId(),
                            job.getType().toString(),
                            job.getCurrentState().toString(),
                            Instant.ofEpochSecond(job.getStartTime().getSeconds())))
                .toList(),
        "Error while retrieving pipelines for project: %s on region %s",
        projectId,
        regionId);
  }

  @Tool(
      name = "Job List For Project, Region and Name",
      description =
          "Get Dataflow's jobs executed in a GCP project, region and with the provided name.")
  public List<Pipeline> pipelines(
      @ToolParam(description = "Job's GCP project identifier.") String projectId,
      @ToolParam(description = "Job's GCP region identifier.") String regionId,
      @ToolParam(description = "Job's exact name.") String name,
      ToolContext context) {
    return execute(
        () ->
            StreamSupport.stream(
                    jobsClient
                        .listJobs(
                            ListJobsRequest.getDefaultInstance().toBuilder()
                                .setProjectId(projectId.trim())
                                .setLocation(regionId.trim())
                                .setName(name.trim())
                                .setPageSize(10)
                                .build())
                        .getPage()
                        .iterateAll()
                        .spliterator(),
                    false)
                .map(
                    job ->
                        new Pipeline(
                            job.getName(),
                            job.getProjectId(),
                            job.getLocation(),
                            job.getId(),
                            job.getType().toString(),
                            job.getCurrentState().toString(),
                            Instant.ofEpochSecond(job.getStartTime().getSeconds())))
                .toList(),
        "Error while retrieving pipelines for project: %s on region %s with name %s",
        projectId,
        regionId,
        name);
  }

  List<Transform> toTransform(List<TransformSummary> summaries) {
    return summaries.stream()
        .map(
            t ->
                new Transform(
                    t.getKind().name(),
                    t.getId(),
                    t.getName(),
                    toDisplayData(t.getDisplayDataList()),
                    t.getOutputCollectionNameList(),
                    t.getInputCollectionNameList()))
        .toList();
  }

  List<Map<String, String>> toDisplayData(List<DisplayData> displayData) {
    return displayData.stream()
        .map(
            d ->
                d.getAllFields().entrySet().stream()
                    .collect(
                        Collectors.toMap(
                            entry -> entry.getKey().getFullName(),
                            entry -> entry.getValue().toString())))
        .toList();
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Transform(
      String kind,
      String id,
      String name,
      List<Map<String, String>> displayData,
      List<String> outputCollectionNames,
      List<String> inputCollectionNames) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Pipeline(
      String name,
      String projectId,
      String region,
      String id,
      String type,
      String state,
      Instant startTime,
      List<String> experiments,
      String machineType,
      List<Transform> transforms,
      List<Map<String, String>> displayData) {

    Pipeline(
        String name,
        String project,
        String region,
        String id,
        String type,
        String state,
        Instant startTime) {
      this(name, project, region, id, type, state, startTime, null, null, null, null);
    }
  }
}
