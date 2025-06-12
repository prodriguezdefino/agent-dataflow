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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.dataflow.v1beta3.GetJobRequest;
import com.google.dataflow.v1beta3.JobView;
import com.google.dataflow.v1beta3.JobsV1Beta3Client;
import com.google.dataflow.v1beta3.ListJobsRequest;
import com.google.protobuf.util.JsonFormat;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** */
@Service
public class PipelineTopologyService {

  private static final Logger LOG = LoggerFactory.getLogger(PipelineTopologyService.class);

  private final JobsV1Beta3Client jobsClient;

  public PipelineTopologyService(JobsV1Beta3Client jobsClient) {
    this.jobsClient = jobsClient;
  }

  @FunctionalInterface
  interface CheckedSupplier<T> {
    T apply() throws Exception;
  }

  static <T> T execute(CheckedSupplier<T> toExecute, String errorMessage, Object... errorArgs) {
    try {
      var result = toExecute.apply();
      LOG.debug("Completed tool execution: " + result.toString());
      return result;
    } catch (Exception ex) {
      String msg = String.format(errorMessage, errorArgs);
      LOG.error(msg, ex);
      throw new RuntimeException(msg, ex);
    }
  }

  @Tool(
      name = "Job Details",
      description = "Get Dataflow's job detailed information, including all the composite states.")
  public String jobDetails(
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
                    jobsClient.getJob(
                        GetJobRequest.getDefaultInstance().toBuilder()
                            .setJobId(jobId.trim())
                            .setProjectId(projectId.trim())
                            .setLocation(regionId.trim())
                            .setView(JobView.JOB_VIEW_ALL)
                            .build())),
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
                                .build())
                        .iterateAll()
                        .spliterator(),
                    false)
                .map(
                    job ->
                        new Pipeline(
                            job.getName(),
                            job.getId(),
                            job.getProjectId(),
                            job.getLocation(),
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
                                .build())
                        .iterateAll()
                        .spliterator(),
                    false)
                .map(
                    job ->
                        new Pipeline(
                            job.getName(),
                            job.getId(),
                            job.getProjectId(),
                            job.getLocation(),
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
                                .build())
                        .iterateAll()
                        .spliterator(),
                    false)
                .map(
                    job ->
                        new Pipeline(
                            job.getName(),
                            job.getId(),
                            job.getProjectId(),
                            job.getLocation(),
                            job.getType().toString(),
                            job.getCurrentState().toString(),
                            Instant.ofEpochSecond(job.getStartTime().getSeconds())))
                .toList(),
        "Error while retrieving pipelines for project: %s on region %s with name %s",
        projectId,
        regionId,
        name);
  }

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
      List<String> options,
      Map<String, Object> stages) {

    Pipeline(
        String name,
        String id,
        String project,
        String region,
        String type,
        String state,
        Instant startTime) {
      this(name, id, project, region, type, state, startTime, null, null, null);
    }
  }
}
