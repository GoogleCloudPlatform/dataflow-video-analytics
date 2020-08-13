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
import com.google.common.collect.ImmutableMap;

import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.ToJson;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.beam.sdk.values.Row;

@SuppressWarnings("serial")
@AutoValue
public abstract class WriteRelevantAnnotationsToPubSubTransform
    extends PTransform<PCollection<Row>, PDone> {

  public abstract String topicId();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setTopicId(String topic);

    public abstract WriteRelevantAnnotationsToPubSubTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_WriteRelevantAnnotationsToPubSubTransform.Builder();
  }

  @Override
  public PDone expand(PCollection<Row> input) {

    return input
        .apply("ConvertToJson", ToJson.of())
        .apply(
            "ConvertToPubSubMessage",
            ParDo.of(
                new DoFn<String, PubsubMessage>() {

                  @ProcessElement
                  public void processContext(ProcessContext c) {
                    c.output(new PubsubMessage(c.element().getBytes(), 
                    		ImmutableMap.of("entity","object_tracking")
                    ));
                  }
                }))
        .apply("PublishToPubSub", PubsubIO.writeMessages().to(topicId()));
  }
}
