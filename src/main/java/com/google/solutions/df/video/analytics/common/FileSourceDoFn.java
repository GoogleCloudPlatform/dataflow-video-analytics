package com.google.solutions.df.video.analytics.common;

import org.apache.beam.sdk.io.FileIO.ReadableFile;
import org.apache.beam.sdk.metrics.Counter;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSourceDoFn extends DoFn<ReadableFile, KV<String, ReadableFile>> {
  public static final Logger LOG = LoggerFactory.getLogger(FileSourceDoFn.class);
  private static final String FILE_PATTERN = "([^\\s]+(\\.(?i)(mp4))$)";
  private final Counter numberOfFiles =
      Metrics.counter(VideoSegmentSplitDoFn.class, "numberOfFiles");

  @ProcessElement
  public void processElement(ProcessContext c) {

    ReadableFile file = c.element();
    String fileName = file.getMetadata().resourceId().toString();
    if (fileName.matches(FILE_PATTERN)) {
      numberOfFiles.inc();
      c.output(KV.of(fileName, file));
    } else {
      LOG.info("Extension Not Supported {}", fileName);
    }
  }
}
