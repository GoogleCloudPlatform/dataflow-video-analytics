package com.google.solutions.df.video.analytics.common;

import com.google.api.client.json.GenericJson;
import com.google.cloud.videointelligence.v1.AnnotateVideoResponse;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
  public static final Logger LOG = LoggerFactory.getLogger(Util.class);
  public static Gson gson = new Gson();

  public static GenericJson convertAnnotateVideoResponseToJson(AnnotateVideoResponse response)
      throws JsonSyntaxException, InvalidProtocolBufferException {
    return gson.fromJson(
        JsonFormat.printer().print(response), new TypeToken<GenericJson>() {}.getType());
  }
}
