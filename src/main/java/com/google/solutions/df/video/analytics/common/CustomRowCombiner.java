package com.google.solutions.df.video.analytics.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.beam.sdk.transforms.Combine.CombineFn;
import org.apache.beam.sdk.values.Row;

public class CustomRowCombiner extends CombineFn<Row, CustomRowCombiner.Accum, List<Row>> {

  public static class Accum implements Serializable {
    Row inputRow;
    List<Row> mergedRows = new ArrayList<Row>();
  }

  @Override
  public Accum createAccumulator() {
    return new Accum();
  }

  @Override
  public Accum addInput(Accum mutableAccumulator, Row input) {
    mutableAccumulator.inputRow =
        Row.withSchema(Util.detectionInstanceSchema)
            .addValue(input.getString("timeOffset"))
            .addValue(input.getFloat("left"))
            .addValue(input.getFloat("top"))
            .addValue(input.getFloat("right"))
            .addValue(input.getFloat("bottom"))
            .build();
    mutableAccumulator.mergedRows.add(mutableAccumulator.inputRow);
    return mutableAccumulator;
  }

  @Override
  public Accum mergeAccumulators(Iterable<Accum> accumulators) {
    Accum merged = createAccumulator();
    accumulators.forEach(
        accum -> {
          accum.mergedRows.forEach(
              row -> {
                merged.mergedRows.add(row);
              });
        });
    return merged;
  }

  @Override
  public List<Row> extractOutput(Accum accumulator) {
    return accumulator.mergedRows;
  }
}
