/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.solutions.df.video.analytics.common;

import com.google.cloud.videointelligence.v1.Feature;
import java.util.List;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;

public interface VideoAnalyticsPipelineOptions extends PipelineOptions {
  @Description("Subscriber Id to receive message from")
  String getSubscriberId();

  void setSubscriberId(String value);

  @Description("Pub/Sub topic to publish result")
  String getTopicId();

  void setTopicId(String value);

  @Description("Features")
  Feature getFeatures();

  void setFeatures(Feature value);

  @Description("Window interval in seconds")
  @Default.Integer(1)
  Integer getWindowInterval();

  void setWindowInterval(Integer value);

  @Description("key range")
  @Default.Integer(1)
  Integer getKeyRange();

  void setKeyRange(Integer value);

  @Description("Object/Entity List to filter the response")
  List<String> getEntity();

  void setEntity(List<String> value);

  @Description("confidence")
  @Default.Double(0.1)
  Double getConfidence();

  void setConfidence(Double value);

  @Description("BQ Table Spec")
  String getTableSpec();

  void setTableSpec(String value);

  @Description("Chunk Size in Seconds")
  @Default.Integer(3)
  Integer getChunkSize();

  void setChunkSize(Integer value);
}
