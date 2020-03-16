package com.google.solutions.df.video.analytics.common;

import com.google.cloud.videointelligence.v1.AnnotateVideoRequest;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationRequestTransform
    extends PTransform<PBegin, PCollection<AnnotateVideoRequest>> {
	public static final Logger LOG = LoggerFactory.getLogger(AnnotationRequestTransform.class);

  @Override
  public PCollection<AnnotateVideoRequest> expand(PBegin input) {
    return null;
  }
}
