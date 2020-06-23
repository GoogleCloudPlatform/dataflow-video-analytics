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
import org.apache.beam.sdk.extensions.gcp.util.gcsfs.GcsPath;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.io.FileIO.ReadableFile;
import org.apache.beam.sdk.io.fs.EmptyMatchTreatment;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter input PubSub messages coming from GCS upload notifications to only process the files that
 * have a valid video file extension.
 */
@AutoValue
public abstract class FilterInputNotificationsTransform
    extends PTransform<PBegin, PCollection<KV<String, ReadableFile>>> {

  private static final Logger LOG =
      LoggerFactory.getLogger(FilterInputNotificationsTransform.class);

  public abstract String subscriptionId();
  // Event type sent when a new object (or a new generation of an existing object)
  // is *successfully* created in the bucket
  private static final String OBJECT_FINALIZE = "OBJECT_FINALIZE";
  // Allowed image extensions supported by the Vision API
  private static final String FILE_PATTERN = "(^.*\\.(?i)(mov|mpeg4|mp4|avi)$)";

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSubscriptionId(String subscriptionId);

    public abstract FilterInputNotificationsTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_FilterInputNotificationsTransform.Builder();
  }

  @Override
  public PCollection<KV<String, ReadableFile>> expand(PBegin input) {
    return input
        .apply(
            "ReadFileMetadataFromPubSubMessage",
            PubsubIO.readMessagesWithAttributes().fromSubscription(subscriptionId()))
        .apply("ValidateEventType", ParDo.of(new ValidateEventType()))
        .apply("FindFile", FileIO.matchAll().withEmptyMatchTreatment(EmptyMatchTreatment.ALLOW))
        .apply(FileIO.readMatches())
        .apply("ValidateFileExtension", ParDo.of(new ValidateFileExtension()));
  }

  public class ValidateEventType extends DoFn<PubsubMessage, String> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      String bucket = c.element().getAttribute("bucketId");
      String object = c.element().getAttribute("objectId");
      String eventType = c.element().getAttribute("eventType");
      GcsPath uri = GcsPath.fromComponents(bucket, object);
      if (eventType != null && eventType.equalsIgnoreCase(OBJECT_FINALIZE)) {
        String path = uri.toString();
        LOG.info("File Name {}", path);
        c.output(path);
      } else {
        LOG.info("Event Type Not Supported {}", eventType);
      }
    }
  }

  public static class ValidateFileExtension extends DoFn<ReadableFile, KV<String, ReadableFile>> {
    @ProcessElement
    public void processElement(ProcessContext c) {
      ReadableFile file = c.element();
      String fileName = file.getMetadata().resourceId().toString();
      if (fileName.matches(FILE_PATTERN)) {
        c.output(KV.of(fileName, file));
        LOG.info("File Name {}", fileName);
      } else {
        LOG.warn("File {} does not contain a valid extension", fileName);
      }
    }
  }
}
