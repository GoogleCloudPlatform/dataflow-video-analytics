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
package com.google.solutions.df.video.analytics;

import com.google.cloud.videointelligence.v1.Feature;
import java.util.List;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;

public interface VideoAnalyticsPipelineOptions extends PipelineOptions {

  @Description("Pub/Sub subscription ID to receive input Cloud Storage notifications from")
  String getInputNotificationSubscription();

  void setInputNotificationSubscription(String value);

  @Description("Pub/Sub topic ID to publish the results to")
  String getOutputTopic();

  void setOutputTopic(String value);

  @Description("Features")
  Feature getFeatures();

  void setFeatures(Feature value);

  @Description("Window time interval (in seconds) for outputing results")
  @Default.Integer(1)
  Integer getWindowInterval();

  void setWindowInterval(Integer value);

  @Description(
      "Comma-separated list of entity labels. Object tracking annotations must contain at least one of those labels to be considered significant and be published the output Pub/Sub topic")
  List<String> getEntities();

  void setEntities(List<String> value);

  @Description(
      "Minimum confidence level that the object tracking annotations must meet to be considered significant and be published the output Pub/Sub topic")
  @Default.Double(0.8)
  Double getConfidenceThreshold();

  void setConfidenceThreshold(Double value);

  @Description("Reference of the output BigQuery table")
  String getTableReference();

  void setTableReference(String value);
}
