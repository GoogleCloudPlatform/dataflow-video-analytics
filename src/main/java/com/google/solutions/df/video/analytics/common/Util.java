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

import com.google.api.client.json.GenericJson;
import com.google.cloud.videointelligence.v1.AnnotateVideoResponse;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.Duration;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.stream.Stream;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.Schema.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
  public static final Logger LOG = LoggerFactory.getLogger(Util.class);
  public static Gson gson = new Gson();
  public static long timeout = 120;
  public static final String ALLOWED_NOTIFICATION_EVENT_TYPE = String.valueOf("OBJECT_FINALIZE");
  /** Allowed image extension supported by Vision API */
  public static final String FILE_PATTERN = "(^.*\\.(MOV|mov|MPEG4|mpeg4|MP4|mp4|AVI|avi)$)";
  /** Error message if no valid extension found */
  public static final String NO_VALID_EXT_FOUND_ERROR_MESSAGE =
      "File {} does not contain a valid extension";

  public static final Schema videoMlDetectionSchema =
      Stream.of(
              Schema.Field.of("frame", FieldType.INT32).withNullable(true),
              Schema.Field.of("timeOffset", FieldType.STRING).withNullable(true),
              Schema.Field.of("x", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("y", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("w", FieldType.FLOAT).withNullable(true),
              Schema.Field.of("h", FieldType.FLOAT).withNullable(true))
          .collect(toSchema());
  public static final Schema videoMlCustomFileDataSchema =
      Stream.of(
              Schema.Field.of("entity", FieldType.STRING).withNullable(true),
              Schema.Field.of("confidence", FieldType.DOUBLE).withNullable(true),
              Schema.Field.of("startTimeOffset", FieldType.STRING).withNullable(true),
              Schema.Field.of("endTimeOffset", FieldType.STRING).withNullable(true))
          .collect(toSchema());

  public static final Schema videoMlCustomFrameDataSchema =
      Stream.of(
              Schema.Field.of("detections", FieldType.array(FieldType.row(videoMlDetectionSchema)))
                  .withNullable(true))
          .collect(toSchema());
  public static final Schema videoMlCustomOutputSchema =
      Stream.of(
              Schema.Field.of("gcsUri", FieldType.STRING).withNullable(true),
              Schema.Field.of("file_data", FieldType.row(videoMlCustomFileDataSchema))
                  .withNullable(true),
              Schema.Field.of("frame_data", FieldType.row(videoMlCustomFrameDataSchema))
                  .withNullable(true))
          .collect(toSchema());

  public static GenericJson convertAnnotateVideoResponseToJson(AnnotateVideoResponse response)
      throws JsonSyntaxException, InvalidProtocolBufferException {
    return gson.fromJson(
        JsonFormat.printer().print(response), new TypeToken<GenericJson>() {}.getType());
  }

  public static String convertToSec(Duration offset) {
    return String.valueOf(offset.getSeconds() + offset.getNanos() / 1e9);
  }
}
