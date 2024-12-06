package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.Analysis;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import com.fasterxml.jackson.annotation.JsonProperty;

// Represents an entire attempt at a DRAGEN Pipeline
public class DragenAnalysis extends Analysis<DragenWorkflowAnalysis> {
  Samplesheet samplesheet;

  public DragenAnalysis(
      @JsonProperty("samplesheet") Samplesheet samplesheet, @JsonProperty("attempt") int attempt) {
    super(attempt);
    this.samplesheet = samplesheet;
  }

  public Samplesheet getSamplesheet() {
    return samplesheet;
  }
}
