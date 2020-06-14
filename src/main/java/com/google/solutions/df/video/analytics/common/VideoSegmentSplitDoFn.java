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

import com.google.cloud.videointelligence.v1.VideoContext;
import com.google.cloud.videointelligence.v1.VideoSegment;
import com.google.protobuf.Duration;
import java.io.IOException;
import org.apache.beam.sdk.io.range.OffsetRange;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.splittabledofn.OffsetRangeTracker;
import org.apache.beam.sdk.transforms.splittabledofn.RestrictionTracker;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VideoSegmentSplitDoFn extends DoFn<KV<String, String>, KV<String, VideoContext>> {
  public static final Logger LOG = LoggerFactory.getLogger(VideoSegmentSplitDoFn.class);
  private Integer chunkSize;
  private Integer keyRange;
  private final Counter numberOfRequests =
      Metrics.counter(VideoSegmentSplitDoFn.class, "numberOfRequests");

  public VideoSegmentSplitDoFn(Integer chunkSize, Integer keyRange) {
    this.chunkSize = chunkSize;
    this.keyRange = keyRange;
  }

  @ProcessElement
  public void processElement(ProcessContext c, RestrictionTracker<OffsetRange, Long> tracker) {
    Double duration = Double.valueOf(c.element().getValue());
    for (long i = tracker.currentRestriction().getFrom(); tracker.tryClaim(i); ++i) {
      Double startOffset = Double.valueOf((i * chunkSize) - chunkSize);
      if (startOffset < duration) {
        Double endOffset = startOffset + chunkSize;
        if (endOffset > duration) {
          endOffset = Math.rint(duration);
        }
        if (startOffset < endOffset) {
          VideoSegment videoSegment =
              VideoSegment.newBuilder()
                  .setStartTimeOffset(
                      Duration.newBuilder().setSeconds(startOffset.longValue()).build())
                  .setEndTimeOffset(Duration.newBuilder().setSeconds(endOffset.longValue()).build())
                  .build();
          VideoContext context = VideoContext.newBuilder().addSegments(videoSegment).build();
          LOG.debug(
              "File Name {} Clip Length {} Video Context {}",
              c.element().getKey(),
              c.element().getValue(),
              context.toString());
          numberOfRequests.inc();
          // String key = String.format("%s~%d", c.element().getKey(), new
          // Random().nextInt(keyRange));
          c.output(KV.of(c.element().getKey(), context));
        }
      }
    }
  }

  @GetInitialRestriction
  public OffsetRange getInitialRestriction(@Element KV<String, String> videoFile)
      throws IOException {
    Double clipLength = Double.valueOf(videoFile.getValue());
    long duration = (long) Math.rint(clipLength);
    long totalSplit = 0;
    if (duration < chunkSize) {
      totalSplit = 2;
    } else {
      totalSplit = totalSplit + (duration / chunkSize);
      double remaining = clipLength % chunkSize;
      if (remaining > 0.0) {
        totalSplit = totalSplit + 2;
      }
    }

    LOG.info(
        "Total clip Duration {} seconds for File {} -Initial Restriction range from 1 to: {}",
        clipLength,
        videoFile.getKey(),
        totalSplit);
    return new OffsetRange(1, totalSplit);
  }

  @SplitRestriction
  public void splitRestriction(
      @Element KV<String, String> videoFile,
      @Restriction OffsetRange range,
      OutputReceiver<OffsetRange> out) {
    for (final OffsetRange p : range.split(1, 1)) {
      out.output(p);
    }
  }

  @NewTracker
  public OffsetRangeTracker newTracker(@Restriction OffsetRange range) {
    return new OffsetRangeTracker(new OffsetRange(range.getFrom(), range.getTo()));
  }
}
