package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.PipelineRun;
import com.fasterxml.jackson.annotation.JsonProperty;

// Represents an entire attempt at the Ultima analysis pipeline
public class UltimaPipelineRun extends PipelineRun<UltimaWorkflowRun> {

  public UltimaPipelineRun(@JsonProperty("attempt") int attempt) {
    super(attempt);
  }
}
