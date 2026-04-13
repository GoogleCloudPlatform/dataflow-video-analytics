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

import com.google.cloud.videointelligence.v1p3beta1.NormalizedBoundingBox;
import com.google.cloud.videointelligence.v1p3beta1.StreamingAnnotateVideoResponse;
import com.google.cloud.videointelligence.v1p3beta1.StreamingVideoAnnotationResults;
import java.util.Objects;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formats the annotation results from the Streaming Video Intelligence API into a schema that can
 * be consumed by downstream pipelines.
 */
public class FormatAnnotationSchemaDoFn
    extends DoFn<KV<String, StreamingAnnotateVideoResponse>, Row> {

  private static final Logger LOG = LoggerFactory.getLogger(FormatAnnotationSchemaDoFn.class);
  private final Counter numberOfObjectAnnotations =
      Metrics.counter(FormatAnnotationSchemaDoFn.class, "numberOfObjectAnnotations");

  static class VideoClipSplitMetadata {
    String baseName;
    String splitIndex;
  }

  /**
   * Extracts some metadata from the provided video clip's file name. It is assumed that the video
   * clip is a time segment created by ffmpeg. e.g. for a file named "myfile~1.mp4", the returned
   * metadata is: baseName="myfile.mp4" splitIndex="1".
   */
  private static VideoClipSplitMetadata extractMetadata(String fileName) {
    VideoClipSplitMetadata metadata = new VideoClipSplitMetadata();
    if (fileName.contains("~")) {
      String extension = fileName.split("\\.")[1];
      String fileNameWithSplitCharacter = fileName.split("\\.")[0];
      String newFileName = fileNameWithSplitCharacter.split("~")[0];
      String fileNameWithExtension = String.format("%s%s%s", newFileName, ".", extension);
      String splitCounter = fileNameWithSplitCharacter.split("~")[1];
      metadata.baseName = fileNameWithExtension;
      metadata.splitIndex = splitCounter;
    } else {
      metadata.baseName = fileName;
      metadata.splitIndex = "0";
    }
    return metadata;
  }

  @ProcessElement
  public void processElement(ProcessContext c) {
    StreamingAnnotateVideoResponse response = c.element().getValue();
    StreamingVideoAnnotationResults results = response.getAnnotationResults();
    numberOfObjectAnnotations.inc(results.getObjectAnnotationsCount());
    VideoClipSplitMetadata metadata = extractMetadata(Objects.requireNonNull(c.element().getKey()));
    results
        .getObjectAnnotationsList()
        .forEach(
            annotation -> {
              double confidence = annotation.getConfidence();
              String entityDescription =
                  annotation.hasEntity() ? annotation.getEntity().getDescription() : "NOT_FOUND";
              // [START loadSnippet_3]
              annotation
                  .getFramesList()
                  .forEach(
                      frame -> {
                        NormalizedBoundingBox normalizedBoundingBox =
                            frame.getNormalizedBoundingBox();

                        Row frameDataOutput =
                            Row.withSchema(Util.detectionInstanceSchema)
                                .addValues(
                                    Util.getCurrentTimeStamp(),
                                    Util.convertDurationToSeconds(
                                        frame.getTimeOffset(), Long.valueOf(metadata.splitIndex)),
                                    confidence,
                                    normalizedBoundingBox.getLeft(),
                                    normalizedBoundingBox.getTop(),
                                    normalizedBoundingBox.getRight(),
                                    normalizedBoundingBox.getBottom())
                                .build();
                        Row outputRow =
                            Row.withSchema(Util.videoMlCustomOutputSchema)
                                .addValues(metadata.baseName, entityDescription, frameDataOutput)
                                .build();
                        LOG.info("Formatted row {}", outputRow.toString());
                        c.output(outputRow);
                      });
              // [END loadSnippet_3]
            });
  }
}
