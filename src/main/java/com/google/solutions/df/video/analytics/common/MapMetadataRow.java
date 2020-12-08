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

import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapMetadataRow extends SimpleFunction<Row, Row> {
  private static final Logger LOG = LoggerFactory.getLogger(MapMetadataRow.class);

  @Override
  public Row apply(Row input) {
    Row metadataRow =
        Row.withSchema(Util.metadataSchema)
            .addValues(
                input.getRow("key").getString("file_name"),
                input.getRow("value").getInt64("num_of_entities"))
            .build();
    LOG.info("Metadata Row {}", metadataRow.toString());
    return metadataRow;
  }
}
