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

import com.google.solutions.df.video.analytics.common.AnnotationRequestTransform;
import com.google.solutions.df.video.analytics.common.BQWriteTransform;
import com.google.solutions.df.video.analytics.common.ResponseWriteTransform;
import com.google.solutions.df.video.analytics.common.Util;
import com.google.solutions.df.video.analytics.common.VideoAnalyticsPipelineOptions;
import com.google.solutions.df.video.analytics.common.VideoApiTransform;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoAnalyticsPipeline {
  public static final Logger LOG = LoggerFactory.getLogger(VideoAnalyticsPipeline.class);

  public static void main(String args[]) {
    VideoAnalyticsPipelineOptions options =
        PipelineOptionsFactory.fromArgs(args)
            .withValidation()
            .as(VideoAnalyticsPipelineOptions.class);

    run(options);
  }

  public static PipelineResult run(VideoAnalyticsPipelineOptions options) {
    Pipeline p = Pipeline.create(options);
    PCollection<String> videoFilesWithContext =
        p.apply(
            "TransformInputRequest",
            AnnotationRequestTransform.newBuilder()
                .setSubscriber(options.getSubscriberId())
                .build());
    PCollection<Row> annotationResult =
        videoFilesWithContext
            .apply(
                "AnnotateVideoRequests",
                VideoApiTransform.newBuilder()
                    .setFeatures(options.getFeatures())
                    .setKeyRange(options.getKeyRange())
                    .setWindowInterval(options.getWindowInterval())
                    .build())
            .setRowSchema(Util.videoMlCustomOutputSchema);

    annotationResult.apply(
        "WriteResponse",
        ResponseWriteTransform.newBuilder()
            .setTopic(options.getTopicId())
            .setEntityList(options.getEntity())
            .setConfidence(options.getConfidence())
            .build());
    annotationResult.apply(
        "StreamFullResponsetoBQ",
        BQWriteTransform.newBuilder()
            .setTableSpec(options.getTableSpec())
            .setMethod(BigQueryIO.Write.Method.STREAMING_INSERTS)
            .build());
    return p.run();
  }
}
