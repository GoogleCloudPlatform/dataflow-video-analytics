package com.google.solutions.df.video.analytics.common;

import com.google.auto.value.AutoValue;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ToJson;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PDone;
import org.apache.beam.sdk.values.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoValue
public abstract class WriteRelevantAnnotationsToPubSubTransform
    extends PTransform<PCollection<Row>, PDone> {
  private static final Logger LOG =
      LoggerFactory.getLogger(WriteRelevantAnnotationsToPubSubTransform.class);

  public abstract String topicId();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setTopicId(String topic);

    public abstract WriteRelevantAnnotationsToPubSubTransform build();
  }

  public static Builder newBuilder() {
    return new AutoValue_WriteRelevantAnnotationsToPubSubTransform.Builder();
  }

  @Override
  public PDone expand(PCollection<Row> input) {

    return input
        .apply("ConvertToJson", ToJson.of())
        .apply("PublishToPubSub", PubsubIO.writeStrings().to(topicId()));
  }
}
