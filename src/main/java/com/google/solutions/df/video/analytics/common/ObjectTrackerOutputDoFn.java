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

import com.google.cloud.videointelligence.v1.NormalizedBoundingBox;
import com.google.cloud.videointelligence.v1.VideoAnnotationResults;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectTrackerOutputDoFn extends DoFn<List<VideoAnnotationResults>, Row> {
  public static final Logger LOG = LoggerFactory.getLogger(AnnotationRequestTransform.class);

  @ProcessElement
  public void processElement(ProcessContext c) {
    List<VideoAnnotationResults> results = c.element();

    results.forEach(
        result -> {
          String gcsUri = result.getInputUri();

          result
              .getObjectAnnotationsList()
              .forEach(
                  annotation -> {
                    double confidence = annotation.getConfidence();
                    String entityDescription =
                        annotation.hasEntity()
                            ? annotation.getEntity().getDescription()
                            : "NOT_FOUND";

                    String startTimeOffset =
                        annotation.hasSegment()
                            ? Util.convertToSec(annotation.getSegment().getStartTimeOffset())
                            : "NOT_FOUND";

                    String endTimeOffset =
                        annotation.hasSegment()
                            ? Util.convertToSec(annotation.getSegment().getEndTimeOffset())
                            : "NOT_FOUND";

                    List<Row> frameDataList = new ArrayList<Row>();
                    AtomicInteger frameCounter = new AtomicInteger(0);
                    annotation
                        .getFramesList()
                        .forEach(
                            frame -> {
                              NormalizedBoundingBox normalizedBoundingBox =
                                  frame.getNormalizedBoundingBox();
                              String timeOffset = Util.convertToSec(frame.getTimeOffset());

                              frameDataList.add(
                                  Row.withSchema(Util.videoMlDetectionSchema)
                                      .addValues(
                                          frameCounter.incrementAndGet(),
                                          timeOffset,
                                          normalizedBoundingBox.getLeft(),
                                          normalizedBoundingBox.getTop(),
                                          normalizedBoundingBox.getRight(),
                                          normalizedBoundingBox.getBottom())
                                      .build());
                            }); // end of frame loop
                    Row outputRow =
                        Row.withSchema(Util.videoMlCustomOutputSchema)
                            .addValues(
                                gcsUri,
                                Row.withSchema(Util.videoMlCustomFileDataSchema)
                                    .addValues(
                                        entityDescription,
                                        confidence,
                                        startTimeOffset,
                                        endTimeOffset)
                                    .build(),
                                Row.withSchema(Util.videoMlCustomFrameDataSchema)
                                    .addArray(frameDataList)
                                    .build())
                            .build();
                    LOG.info("Row {}", outputRow);
                    c.output(outputRow);
                  });
        });
  }
}
