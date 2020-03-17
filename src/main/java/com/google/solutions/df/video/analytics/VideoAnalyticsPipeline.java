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
import com.google.solutions.df.video.analytics.common.ResponseWriteTransform;
import com.google.solutions.df.video.analytics.common.VideoAnalyticsPipelineOptions;
import com.google.solutions.df.video.analytics.common.VideoApiTransform;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
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
    p.apply(
            "TransformInputRequest",
            AnnotationRequestTransform.newBuilder()
                .setSubscriber(options.getSubscriberId())
                .build())
        .apply("ProcessAnnotateRequest", new VideoApiTransform())
        .apply(
            "WriteResponse",
            ResponseWriteTransform.newBuilder().setTopic(options.getTopicId()).build());
    return p.run();
  }
}
