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

import com.google.api.client.json.GenericJson;
import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.videointelligence.v1.AnnotateVideoProgress;
import com.google.cloud.videointelligence.v1.AnnotateVideoRequest;
import com.google.cloud.videointelligence.v1.AnnotateVideoResponse;
import com.google.cloud.videointelligence.v1.VideoIntelligenceServiceClient;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoApiTransform
    extends PTransform<PCollection<AnnotateVideoRequest>, PCollection<String>> {
  public static final Logger LOG = LoggerFactory.getLogger(VideoApiTransform.class);

  @Override
  public PCollection<String> expand(PCollection<AnnotateVideoRequest> input) {
    return input.apply("VideAPIProcessing", ParDo.of(new VideoApiDoFn()));
  }

  public static class VideoApiDoFn extends DoFn<AnnotateVideoRequest, String> {

    private VideoIntelligenceServiceClient client = null;

    @StartBundle
    public void startBundle() {
      try {
        this.client = VideoIntelligenceServiceClient.create();
      } catch (IOException e) {
        this.client.close();
        LOG.error("Can't create VIS API Client");
      }
    }

    @FinishBundle
    public void finishBundle() throws Exception {
      if (this.client != null) {
        this.client.close();
      }
    }

    @ProcessElement
    public void processElement(ProcessContext c) {

      AnnotateVideoRequest request = c.element();
      AnnotateVideoResponse response = null;
      GenericJson json;
      String feature = request.getFeatures(0).name();
      // asynchronously perform video annotate request
      OperationFuture<AnnotateVideoResponse, AnnotateVideoProgress> future =
          this.client.annotateVideoAsync(request);
      LOG.info("Waiting response for a feature {} to complete", feature);
      try {
        response = future.get(Util.timeout, TimeUnit.SECONDS);
        json = Util.convertAnnotateVideoResponseToJson(response);
        LOG.debug("JSON {}", Util.gson.toJson(json));
        c.output(Util.gson.toJson(json));

      } catch (InterruptedException
          | ExecutionException
          | TimeoutException
          | JsonSyntaxException
          | InvalidProtocolBufferException e) {
        LOG.error("ERROR Processing request {}", e.getMessage());
      }
    }
  }
}
