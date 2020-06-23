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
import java.util.stream.Stream;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.Schema.FieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Util {

  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

  static final Schema detectionInstanceSchema =
      Stream.of(
              Schema.Field.of("frame", FieldType.INT32).withNullable(true),
              Schema.Field.of("timeOffset", FieldType.STRING).withNullable(true),
              Schema.Field.of("x", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("y", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("w", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("h", FieldType.FLOAT).withNullable(true))
          .collect(toSchema());

  static final Schema detectedEntitySchema =
      Stream.of(
              Schema.Field.of("entity", FieldType.STRING).withNullable(true),
              Schema.Field.of("confidence", FieldType.DOUBLE).withNullable(true),
              Schema.Field.of("transaction_time", FieldType.STRING).withNullable(true))
          .collect(toSchema());

  static final Schema frameSchema =
      Stream.of(
              Schema.Field.of("detections", FieldType.array(FieldType.row(detectionInstanceSchema)))
                  .withNullable(true))
          .collect(toSchema());

  public static final Schema videoMlCustomOutputSchema =
      Stream.of(
              Schema.Field.of("gcsUri", FieldType.STRING).withNullable(true),
              Schema.Field.of("file_data", FieldType.row(detectedEntitySchema)).withNullable(true),
              Schema.Field.of("frame_data", FieldType.row(frameSchema)).withNullable(true))
          .collect(toSchema());

  static String convertDurationToSeconds(Duration offset) {
    return String.valueOf(offset.getSeconds() + offset.getNanos() / 1e9);
  }

  static String getCurrentTimeStamp() {
    return TIMESTAMP_FORMATTER.print(Instant.now().toDateTime(DateTimeZone.UTC));
  }
}
