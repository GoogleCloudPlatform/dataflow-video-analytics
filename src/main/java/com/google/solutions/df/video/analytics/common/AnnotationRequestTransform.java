package com.google.solutions.df.video.analytics.common;

import com.google.auto.value.AutoValue;
import com.google.cloud.videointelligence.v1.AnnotateVideoRequest;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
public abstract class AnnotationRequestTransform
    extends PTransform<PBegin, PCollection<AnnotateVideoRequest>> {
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
  public PCollection<AnnotateVideoRequest> expand(PBegin input) {
    return input
        .apply("ReadFromPubSub", PubsubIO.readStrings().fromSubscription(subscriber()))
        .apply("ConvertJsontoProto", ParDo.of(new JsonValidatorFn()));
  }

  public static class JsonValidatorFn extends DoFn<String, AnnotateVideoRequest> {
    public Gson gson;

    @Setup
    public void setup() {
      gson = new Gson();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
      String input = c.element();
      try {

        try {

          AnnotateVideoRequest.Builder request = AnnotateVideoRequest.newBuilder();
          JsonFormat.parser().ignoringUnknownFields().merge(input, request);
          c.output(request.build());

        } catch (InvalidProtocolBufferException e) {

          LOG.info("Error {}", e.getMessage());
        }

      } catch (JsonSyntaxException e) {
      }
    }
  }
}
