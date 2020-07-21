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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.beam.sdk.schemas.transforms.Group;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class GroupByAnnotateResponseTransform
    extends PTransform<PCollection<Row>, PCollection<Row>> {
  private static final Logger LOG = LoggerFactory.getLogger(GroupByAnnotateResponseTransform.class);
  // [START loadSnippet_5]
  @Override
  public PCollection<Row> expand(PCollection<Row> input) {
    return input
        .apply("GroupByFields", Group.byFieldNames("file_name", "entity"))
        .apply("MergeRow", MapElements.via(new MergeFilterResponse()))
        .setRowSchema(Util.videoMlCustomOutputListSchema);
  }

  public static class MergeFilterResponse extends SimpleFunction<Row, Row> {
    @Override
    public Row apply(Row input) {
      String gcsUri = input.getRow("key").getString("file_name");
      String entity = input.getRow("key").getString("entity");
      List<Row> detections = new ArrayList<>();
      Iterable<Row> values = input.getIterable("value");
      Objects.requireNonNull(values)
          .forEach(
              v -> {
                detections.add(v.getRow("detection"));
              });
      Row row =
          Row.withSchema(Util.videoMlCustomOutputListSchema)
              .addValues(Util.getFileName(gcsUri), entity, detections)
              .build();
      // [END loadSnippet_5]

      LOG.debug("Output Row {}", row.toString());
      return row;
    }
  }
}
