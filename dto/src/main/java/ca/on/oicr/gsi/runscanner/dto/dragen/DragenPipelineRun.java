package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.PipelineRun;
import com.fasterxml.jackson.annotation.JsonProperty;

// Represents an entire attempt at a DRAGEN Pipeline
public class DragenPipelineRun extends PipelineRun<DragenWorkflowRun> {

  public DragenPipelineRun(@JsonProperty("attempt") int attempt) {
    super(attempt);
  }
}
