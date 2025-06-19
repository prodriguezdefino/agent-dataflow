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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** */
@ConfigurationProperties(prefix = "knowledge")
public class KnowledgeProperties {
  enum Categories {
    Sources,
    Sinks
  }

  private final Map<String, List<String>> categories;
  private final Map<String, List<String>> entries;

  public KnowledgeProperties(
      Map<String, List<String>> categories, Map<String, List<String>> entries) {
    this.categories = categories;
    this.entries = entries;
  }

  public List<String> bestPractice(String category) {
    return Optional.ofNullable(entries.get(category)).orElse(List.of());
  }

  public IoCategories ioCategories() {
    return new IoCategories(
        categories.get(Categories.Sources.name().toLowerCase()),
        categories.get(Categories.Sinks.name().toLowerCase()));
  }

  public record IoCategories(List<String> sources, List<String> sinks) {}
}
