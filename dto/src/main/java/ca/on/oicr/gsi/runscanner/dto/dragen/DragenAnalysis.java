package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.Analysis;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

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

  public String toString() {
    return super.toString() + ", DragenAnalysis [samplesheet=" + samplesheet + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    DragenAnalysis other = (DragenAnalysis) obj;

    return Objects.equals(this.samplesheet, other.samplesheet);
  }

  public int hashCode() {
    return Objects.hash(super.hashCode(), samplesheet);
  }
}
