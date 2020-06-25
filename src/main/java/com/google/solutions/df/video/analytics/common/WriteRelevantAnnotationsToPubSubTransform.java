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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.schemas.transforms.Filter;
import org.apache.beam.sdk.schemas.transforms.Group;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.transforms.ToJson;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters through all the annotated results and outputs to PubSub only the ones that satisfy to the
 * specified entities and confidence threshold.
 */
@AutoValue
public abstract class WriteRelevantAnnotationsToPubSubTransform
    extends PTransform<PCollection<Row>, PDone> {

  private static final Logger LOG =
      LoggerFactory.getLogger(WriteRelevantAnnotationsToPubSubTransform.class);

  public abstract String topicId();

  @Nullable
  public abstract List<String> entityList();

  @Nullable
  public abstract Double confidenceThreshold();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setTopicId(String topic);

    public abstract Builder setEntityList(List<String> entityLst);

    public abstract Builder setConfidenceThreshold(Double confidence);

    public abstract WriteRelevantAnnotationsToPubSubTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_WriteRelevantAnnotationsToPubSubTransform.Builder();
  }

  // [START loadSnippet_4]
  @Override
  public PDone expand(PCollection<Row> input) {

    return input
        .apply(
            "FilterByEntityAndConfidence",
            Filter.<Row>create()
                .whereFieldName(
                    "entity", entity -> entityList().stream().anyMatch(obj -> obj.equals(entity)))
                .whereFieldName(
                    "detection.confidence",
                    (Double confidence) -> confidence >= confidenceThreshold()))
        .apply("GroupByFileName", Group.<Row>byFieldNames("gcsUri", "entity"))
        .apply("MergeRow", MapElements.via(new MergeFilterResponse()))
        .setRowSchema(Util.videoMlCustomOutputSchema)
        .apply("ConvertToJson", ToJson.of())
        // [END loadSnippet_4]
        .apply("PublishToPubSub", PubsubIO.writeStrings().to(topicId()));
  }

  public class MergeFilterResponse extends SimpleFunction<Row, Row> {
    @Override
    public Row apply(Row input) {

      String gcsUri = input.getRow("key").getString("gcsUri");
      String entity = input.getRow("key").getString("entity");
      List<Row> detections = new ArrayList<Row>();
      Iterable<Row> values = input.getIterable("value");
      values.forEach(
          v -> {
            detections.add(v.getRow("detection"));
          });
      Row aggrRow =
          Row.withSchema(Util.videoMlCustomOutputSchema)
              .addValues(gcsUri, entity, detections)
              .build();
      LOG.info("Output Row {}", aggrRow.toString());

      return aggrRow;
    }
  }
}
