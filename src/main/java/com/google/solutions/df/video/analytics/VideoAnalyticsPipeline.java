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

import com.google.protobuf.ByteString;
import com.google.solutions.df.video.analytics.common.*;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.windowing.AfterWatermark;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.joda.time.Duration;

public class VideoAnalyticsPipeline {

  public static void main(String[] args) {
    VideoAnalyticsPipelineOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(VideoAnalyticsPipelineOptions.class);
    run(options);
  }

  private static void run(VideoAnalyticsPipelineOptions options) {
    Pipeline p = Pipeline.create(options);

    // Ingest and validate input GCS notifications
    PCollection<KV<String, ByteString>> videoFilesWithContext =
        p.apply(
            "FilterInputNotifications",
            FilterInputNotificationsTransform.newBuilder()
                .setSubscriptionId(options.getInputNotificationSubscription())
                .build());

    // Call the Video ML API to annotate the ingested video clips
    PCollection<Row> annotationResult =
        videoFilesWithContext
            .apply(
                "AnnotateVideoChunks",
                AnnotateVideoChunksTransform.newBuilder()
                    .setFeatures(options.getFeatures())
                    .setErrorTopic(options.getErrorTopic())
                    .build())
            .setRowSchema(Util.videoMlCustomOutputSchema)
            .apply(
                "FixedWindow",
                Window.<Row>into(
                        FixedWindows.of(Duration.standardSeconds(options.getWindowInterval())))
                    .triggering(AfterWatermark.pastEndOfWindow())
                    .discardingFiredPanes()
                    .withAllowedLateness(Duration.ZERO))
            .apply("GroupAnnotationsResponse", new GroupByAnnotateResponseTransform())
            .setRowSchema(Util.videoMlCustomOutputListSchema);

    // Filter annotations by relevant entities and confidence, then write to Pub/Sub output topic
    annotationResult
        .apply(
            "FilterRelevantAnnotations",
            FilterRelevantAnnotationsTransform.newBuilder()
                .setEntityList(options.getEntities())
                .setConfidenceThreshold(options.getConfidenceThreshold())
                .build())
        .setRowSchema(Util.videoMlCustomOutputListSchema)
        .apply(
            "WriteRelevantAnnotationsToPubSub",
            WriteRelevantAnnotationsToPubSubTransform.newBuilder()
                .setTopicId(options.getOutputTopic())
                .build());

    // Stream insert all annotations to BigQuery
    annotationResult.apply(
        "WriteAllAnnotationsToBigQuery",
        WriteAllAnnotationsToBigQueryTransform.newBuilder()
            .setTableReference(options.getTableReference())
            .setMethod(BigQueryIO.Write.Method.STREAMING_INSERTS)
            .build());
    p.run();
  }
}
