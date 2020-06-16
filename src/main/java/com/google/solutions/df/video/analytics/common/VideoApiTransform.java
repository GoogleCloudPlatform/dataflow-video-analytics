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

import com.google.auto.value.AutoValue;
import com.google.cloud.videointelligence.v1.Feature;
import com.google.cloud.videointelligence.v1.VideoContext;
import java.util.Collections;
import org.apache.beam.sdk.extensions.ml.VideoIntelligence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
public abstract class VideoApiTransform
    extends PTransform<PCollection<KV<String, VideoContext>>, PCollection<Row>> {
  public static final Logger LOG = LoggerFactory.getLogger(VideoApiTransform.class);

  public abstract Integer keyRange();

  public abstract Feature features();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setKeyRange(Integer keyRange);

    public abstract Builder setFeatures(Feature features);

    public abstract VideoApiTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_VideoApiTransform.Builder();
  }

  @Override
  public PCollection<Row> expand(PCollection<KV<String, VideoContext>> input) {

    return input
        .apply(
            "RequestValidator",
            ParDo.of(
                new DoFn<KV<String, VideoContext>, KV<String, VideoContext>>() {
                  @ProcessElement
                  public void processElement(ProcessContext c) {
                     String fileName = c.element().getKey().split("\\~")[0];
                     LOG.debug("Request For File Name {}", fileName);
                    c.output(KV.of(fileName, c.element().getValue()));
                  }
                }))
        .apply(
            "AnnotateVideoFiles",
            ParDo.of(
                VideoIntelligence.annotateFromUriWithContext(
                    Collections.singletonList(features()))))
        .apply("ProcessResponse", ParDo.of(new ObjectTrackerOutputDoFn()));
  }
}
