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
import java.util.Collections;
import java.util.Random;
import org.apache.beam.sdk.extensions.ml.VideoIntelligence;
import org.apache.beam.sdk.state.BagState;
import org.apache.beam.sdk.state.StateSpec;
import org.apache.beam.sdk.state.StateSpecs;
import org.apache.beam.sdk.state.TimeDomain;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.state.TimerSpec;
import org.apache.beam.sdk.state.TimerSpecs;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.DoFn.OnTimer;
import org.apache.beam.sdk.transforms.DoFn.OutputReceiver;
import org.apache.beam.sdk.transforms.DoFn.StateId;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.WithKeys;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
public abstract class VideoApiTransform extends PTransform<PCollection<String>, PCollection<Row>> {
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
  public PCollection<Row> expand(PCollection<String> input) {

    return input
        //.apply("AddRandomKey", WithKeys.of(new Random().nextInt(keyRange())))
        //.apply("ProcessingTimeDelay", ParDo.of(new BatchRequest()))
        .apply(
            "AnnotateVideoFiles",
            ParDo.of(
                VideoIntelligence.annotateFromURI(Collections.singletonList(features()), null)))
        .apply("ProcessResponse", ParDo.of(new ObjectTrackerOutputDoFn()));
  }

  public static class BatchRequest extends DoFn<KV<Integer, String>, String> {

    @StateId("elementsBag")
    private final StateSpec<BagState<String>> elementsBag = StateSpecs.bag();

    @ProcessElement
    public void process(
        @Element KV<Integer, String> element,
        @StateId("elementsBag") BagState<String> elementsBag) {
      elementsBag.add(element.getValue());
    }

    @OnWindowExpiration 
    public void onWindowExpiration (
        @StateId("elementsBag") BagState<String> elementsBag, OutputReceiver<String> output) {
      elementsBag
          .read()
          .forEach(
              file -> {
                output.output(file);
              });
      elementsBag.clear();
    }
  }
}
