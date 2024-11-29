package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.Analysis;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;

// Represents an entire attempt at a DRAGEN Pipeline
public class DragenAnalysis extends Analysis<DragenWorkflowAnalysis> {
  Samplesheet samplesheet;

  public DragenAnalysis(Samplesheet s) {
    samplesheet = s;
  }
}
