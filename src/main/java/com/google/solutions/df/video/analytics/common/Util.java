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

import static org.apache.beam.sdk.schemas.Schema.toSchema;

import com.google.protobuf.Duration;
import java.net.URI;
import java.util.stream.Stream;
import org.apache.beam.sdk.extensions.gcp.util.gcsfs.GcsPath;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.Schema.FieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
  private static final Logger LOG = LoggerFactory.getLogger(Util.class);

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
  private static final Long SPLIT_SECONDS_PARAM = 5L;

  public static final Schema metadataSchema =
      Stream.of(
              Schema.Field.of("file_name", FieldType.STRING).withNullable(true),
              Schema.Field.of("num_of_entities", FieldType.INT64).withNullable(true))
          .collect(toSchema());
  static final Schema detectionInstanceSchema =
      Stream.of(
              Schema.Field.of("processing_timestamp", FieldType.STRING).withNullable(true),
              Schema.Field.of("timeOffset", FieldType.STRING).withNullable(true),
              Schema.Field.of("confidence", FieldType.DOUBLE).withNullable(true),
              Schema.Field.of("left", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("top", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("right", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("bottom", FieldType.FLOAT).withNullable(true))
          .collect(toSchema());

  public static final Schema videoMlCustomOutputListSchema =
      Stream.of(
              Schema.Field.of("file_name", FieldType.STRING).withNullable(true),
              Schema.Field.of("entity", FieldType.STRING).withNullable(true),
              Schema.Field.of(
                  "frame_data", FieldType.array(FieldType.row(detectionInstanceSchema))))
          .collect(toSchema());

  public static final Schema videoMlCustomOutputSchema =
      Stream.of(
              Schema.Field.of("file_name", FieldType.STRING).withNullable(true),
              Schema.Field.of("entity", FieldType.STRING).withNullable(true),
              Schema.Field.of("detection", FieldType.row(detectionInstanceSchema))
                  .withNullable(true))
          .collect(toSchema());

  public static final Schema errorSchema =
      Stream.of(
              Schema.Field.of("file_name", FieldType.STRING).withNullable(true),
              Schema.Field.of("transaction_timestamp", FieldType.STRING).withNullable(true),
              Schema.Field.of("error_code", FieldType.INT32).withNullable(true),
              Schema.Field.of("error_message", FieldType.STRING).withNullable(true))
          .collect(toSchema());

  static String convertDurationToSeconds(Duration offset, Long splitSeconds) {
    long offsetValue = SPLIT_SECONDS_PARAM * splitSeconds;
    return String.valueOf((offset.getSeconds() + offsetValue) + offset.getNanos() / 1e9);
  }

  static String getCurrentTimeStamp() {
    return TIMESTAMP_FORMATTER.print(Instant.now().toDateTime(DateTimeZone.UTC));
  }

  static String getFileName(String gcsPath) {
    GcsPath path = GcsPath.fromUri(URI.create(gcsPath));
    return path.getObject();
  }
}
