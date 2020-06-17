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

import com.google.api.gax.rpc.BidiStream;
import com.google.auto.value.AutoValue;
import com.google.cloud.videointelligence.v1.Feature;
import com.google.cloud.videointelligence.v1p3beta1.StreamingAnnotateVideoRequest;
import com.google.cloud.videointelligence.v1p3beta1.StreamingAnnotateVideoResponse;
import com.google.cloud.videointelligence.v1p3beta1.StreamingFeature;
import com.google.cloud.videointelligence.v1p3beta1.StreamingLabelDetectionConfig;
import com.google.cloud.videointelligence.v1p3beta1.StreamingVideoConfig;
import com.google.cloud.videointelligence.v1p3beta1.StreamingVideoIntelligenceServiceClient;
import com.google.protobuf.ByteString;
import java.io.IOException;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
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
    extends PTransform<PCollection<KV<String, ByteString>>, PCollection<Row>> {
  public static final Logger LOG = LoggerFactory.getLogger(VideoApiTransform.class);

  public abstract Feature features();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setFeatures(Feature features);

    public abstract VideoApiTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_VideoApiTransform.Builder();
  }

  @Override
  public PCollection<Row> expand(PCollection<KV<String, ByteString>> input) {

    return input
        .apply("StreamingObjectTracking", ParDo.of(new StreamingObjectTracking()))
        .apply("ProcessResponse", ParDo.of(new ObjectTrackerOutputDoFn()));
  }

  public static class StreamingObjectTracking
      extends DoFn<KV<String, ByteString>, KV<String, StreamingAnnotateVideoResponse>> {
    private final Counter numberOfRequests =
        Metrics.counter(VideoApiTransform.class, "numberOfRequests");

    private StreamingVideoConfig streamingVideoConfig;
    private StreamingVideoIntelligenceServiceClient client;
    BidiStream<StreamingAnnotateVideoRequest, StreamingAnnotateVideoResponse> call;

    @Setup
    public void setup() throws IOException {
      //      StreamingVideoConfig streamingVideoConfig =
      //          StreamingVideoConfig.newBuilder()
      //              .setFeature(StreamingFeature.STREAMING_OBJECT_TRACKING)
      //              .build();
      //      client = StreamingVideoIntelligenceServiceClient.create();
      //      call = client.streamingAnnotateVideoCallable().call();
      //      call.send(
      //
      // StreamingAnnotateVideoRequest.newBuilder().setVideoConfig(streamingVideoConfig).build());
    }

    @ProcessElement
    public void processElement(ProcessContext c) throws IOException {
      String fileName = c.element().getKey();
      ByteString data = c.element().getValue();

      try (StreamingVideoIntelligenceServiceClient client =
          StreamingVideoIntelligenceServiceClient.create()) {
        StreamingLabelDetectionConfig labelConfig =
            StreamingLabelDetectionConfig.newBuilder().setStationaryCamera(false).build();
        StreamingVideoConfig streamingVideoConfig =
            StreamingVideoConfig.newBuilder()
                .setFeature(StreamingFeature.STREAMING_OBJECT_TRACKING)
                .setLabelDetectionConfig(labelConfig)
                .build();
        call = client.streamingAnnotateVideoCallable().call();
        call.send(
            StreamingAnnotateVideoRequest.newBuilder()
                .setVideoConfig(streamingVideoConfig)
                .build());
        call.send(StreamingAnnotateVideoRequest.newBuilder().setInputContent(data).build());
        numberOfRequests.inc();
        call.closeSend();
        for (StreamingAnnotateVideoResponse response : call) {
          c.output(KV.of(fileName, response));
        }
      }
    }
  }
}
