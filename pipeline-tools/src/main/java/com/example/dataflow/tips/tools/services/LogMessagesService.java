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

import com.example.dataflow.tips.tools.common.Utils;
import com.google.dataflow.v1beta3.JobMessage;
import com.google.dataflow.v1beta3.JobMessageImportance;
import com.google.dataflow.v1beta3.ListJobMessagesRequest;
import com.google.dataflow.v1beta3.MessagesV1Beta3Client;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/** */
@Service
public class LogMessagesService {

  private final MessagesV1Beta3Client logClient;

  public LogMessagesService(MessagesV1Beta3Client logClient) {
    this.logClient = logClient;
  }

  @Tool(
      name = "Log Messages Per Level",
      description = "Retrieve the available log messages in the desired level.")
  public List<String> logMessages(
      @ToolParam(description = "Job's GCP project identifier.") String projectId,
      @ToolParam(description = "Job's GCP region identifier.") String regionId,
      @ToolParam(description = "Job's identifier.") String jobId,
      @ToolParam(
              description =
                  "Log level, expected values are: "
                      + "BASIC, DEBUG, DETAILED, ERROR, WARNING, UNKNOWN or null.")
          String logLevel,
      @ToolParam(description = "The amount of seconds ago to retrieve messages from")
          Integer secondsAgo,
      ToolContext context) {
    return Utils.execute(
        () ->
            StreamSupport.stream(
                    logClient
                        .listJobMessages(
                            ListJobMessagesRequest.getDefaultInstance().toBuilder()
                                .setProjectId(projectId)
                                .setJobId(jobId)
                                .setLocation(regionId)
                                .setStartTime(
                                    Timestamp.getDefaultInstance().toBuilder()
                                        .setSeconds(
                                            Instant.now().minusSeconds(secondsAgo).getEpochSecond())
                                        .build())
                                .setMinimumImportance(
                                    JobMessageImportance.valueOf(
                                        Optional.ofNullable(logLevel)
                                            .map(level -> "JOB_MESSAGE_" + level.toUpperCase())
                                            .orElse("UNRECOGNIZED")))
                                .setPageSize(10)
                                .build())
                        .getPage()
                        .iterateAll()
                        .spliterator(),
                    false)
                .map(JobMessage::toString)
                .toList(),
        "Errors while trying to retrieve the job %s logs at %s level (project %s, region %s).",
        jobId,
        logLevel,
        projectId,
        regionId);
  }
}
