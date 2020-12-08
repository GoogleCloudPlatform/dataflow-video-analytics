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
import java.util.List;
import java.util.Objects;
import org.apache.beam.sdk.schemas.transforms.Filter;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;

/**
 * Filters through all the annotated results and outputs to PubSub only the ones that satisfy to the
 * specified entities and confidence threshold.
 */
@AutoValue
public abstract class FilterRelevantAnnotationsTransform
    extends PTransform<PCollection<Row>, PCollection<Row>> {

  public abstract List<String> entityList();

  public abstract Double confidenceThreshold();

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setEntityList(List<String> entityList);

    public abstract Builder setConfidenceThreshold(Double confidence);

    public abstract FilterRelevantAnnotationsTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_FilterRelevantAnnotationsTransform.Builder();
  }

  // [START loadSnippet_4]
  @Override
  public PCollection<Row> expand(PCollection<Row> input) {

    PCollection<Row> filterRows =
        input.apply(
            "FilterByEntityAndConfidence",
            Filter.<Row>create()
                .whereFieldName(
                    "entity", entity -> entityList().stream().anyMatch(obj -> obj.equals(entity)))
                .whereFieldName(
                    "frame_data",
                    (List<Row> fd) ->
                        fd.stream()
                            .anyMatch(
                                detection ->
                                    Objects.requireNonNull(detection.getDouble("confidence"))
                                        >= confidenceThreshold())));
    // [END loadSnippet_4]
    // count the number of entities for metadata

    return filterRows;
  }
}
