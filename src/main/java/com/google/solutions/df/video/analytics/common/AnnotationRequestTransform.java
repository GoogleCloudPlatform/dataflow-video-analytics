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

@AutoValue
public abstract class AnnotationRequestTransform
    extends PTransform<PBegin, PCollection<KV<String, ReadableFile>>> {
  public static final Logger LOG = LoggerFactory.getLogger(AnnotationRequestTransform.class);

  public abstract String subscriber();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setSubscriber(String subscriberId);

    public abstract AnnotationRequestTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_AnnotationRequestTransform.Builder();
  }
  @Override
  public PCollection<KV<String, ReadableFile>> expand(PBegin input) {
    return input
        .apply(
            "ReadFileMetadata",
            PubsubIO.readMessagesWithAttributes().fromSubscription(subscriber()))
        .apply("ConvertToGCSUri", ParDo.of(new MapPubSubMessage()))
        .apply("FindFile", FileIO.matchAll().withEmptyMatchTreatment(EmptyMatchTreatment.ALLOW))
        .apply(FileIO.readMatches())
        .apply("AddFileNameAsKey", ParDo.of(new FileSourceDoFn()));
  }

  public class MapPubSubMessage extends DoFn<PubsubMessage, String> {

    @ProcessElement
    public void processElement(ProcessContext c) {
      String bucket = c.element().getAttribute("bucketId");
      String object = c.element().getAttribute("objectId");
      String eventType = c.element().getAttribute("eventType");
      GcsPath uri = GcsPath.fromComponents(bucket, object);

      if (eventType.equalsIgnoreCase(Util.ALLOWED_NOTIFICATION_EVENT_TYPE)) {

        String path = uri.toString();
        LOG.info("File Name {}", path);
        c.output(path);
      } else {
        LOG.info("Event Type Not Supported {}", eventType);
      }
    }
  }
}
