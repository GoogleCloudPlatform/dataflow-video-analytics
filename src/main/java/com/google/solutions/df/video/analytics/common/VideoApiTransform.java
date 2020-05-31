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
import com.google.cloud.videointelligence.v1.VideoAnnotationResults;
import com.google.common.base.MoreObjects;
import java.util.Collections;
import java.util.List;
import org.apache.beam.sdk.extensions.ml.VideoIntelligence;
import org.apache.beam.sdk.state.BagState;
import org.apache.beam.sdk.state.StateSpec;
import org.apache.beam.sdk.state.StateSpecs;
import org.apache.beam.sdk.state.TimeDomain;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.state.TimerSpec;
import org.apache.beam.sdk.state.TimerSpecs;
import org.apache.beam.sdk.state.ValueState;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.WithKeys;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
public abstract class VideoApiTransform
    extends PTransform<PCollection<String>, PCollection<String>> {
  public static final Logger LOG = LoggerFactory.getLogger(VideoApiTransform.class);

  public abstract Integer keyRange();

  public abstract Integer windowInterval();

  public abstract Feature features();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setKeyRange(Integer keyRange);

    public abstract Builder setWindowInterval(Integer windowInterval);

    public abstract Builder setFeatures(Feature features);

    public abstract VideoApiTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_VideoApiTransform.Builder();
  }

  @Override
  public PCollection<String> expand(PCollection<String> input) {

    return input
        .apply("AddRandomKey", WithKeys.of(keyRange()))
        .apply("ProcessingTimeDelay", ParDo.of(new BatchRequest(windowInterval())))
        .apply(
            "AnnotateVideoFiles",
            ParDo.of(
                VideoIntelligence.annotateFromURI(Collections.singletonList(features()), null)))
        .apply(
            "ProcessResponse",
            ParDo.of(
                new DoFn<List<VideoAnnotationResults>, String>() {
                  @ProcessElement
                  public void processElement(ProcessContext c) {
                    c.element()
                        .forEach(
                            output -> {
                              c.output(output.toString());
                            });
                  }
                }));
  }

  public static class BatchRequest extends DoFn<KV<Integer, String>, String> {
    private Integer windowInterval;

    public BatchRequest(Integer windowInterval) {
      this.windowInterval = windowInterval;
    }

    @StateId("elementsBag")
    private final StateSpec<BagState<KV<Integer, String>>> elementsBag = StateSpecs.bag();

    @TimerId("eventTimer")
    private final TimerSpec timer = TimerSpecs.timer(TimeDomain.PROCESSING_TIME);

    @StateId("isTimerSet")
    private final StateSpec<ValueState<Boolean>> isTimerSet = StateSpecs.value();

    @ProcessElement
    public void process(
        @Element KV<Integer, String> element,
        @StateId("elementsBag") BagState<String> elementsBag,
        @StateId("isTimerSet") ValueState<Boolean> isTimerSetState,
        @TimerId("eventTimer") Timer eventTimer) {
      elementsBag.add(element.getValue());
      if (!MoreObjects.firstNonNull(isTimerSetState.read(), false)) {
        eventTimer.offset(Duration.standardSeconds(windowInterval)).setRelative();
        isTimerSetState.write(true);
      }
    }

    @OnTimer("eventTimer")
    public void onTimer(
        @StateId("elementsBag") BagState<String> elementsBag,
        OutputReceiver<String> output,
        @StateId("isTimerSet") ValueState<Boolean> isTimerSetState) {

      elementsBag
          .read()
          .forEach(
              file -> {
                output.output(file);
              });

      elementsBag.clear();
      isTimerSetState.clear();
    }
  }
}
