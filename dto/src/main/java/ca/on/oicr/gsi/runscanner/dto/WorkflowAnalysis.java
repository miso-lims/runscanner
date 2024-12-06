package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.dragen.DragenWorkflowAnalysis;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;

// Represents one attempt at a workflow within a suite
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "suite")
@JsonSubTypes({ //
  @Type(value = DragenWorkflowAnalysis.class, name = "DRAGEN") //
}) //
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class WorkflowAnalysis {
  private Instant completionTime;
  private Instant startTime;
  private final String workflowName;

  public WorkflowAnalysis(String workflowName) {
    this.workflowName = workflowName;
  }

  public Instant getCompletionTime() {
    return completionTime;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setCompletionTime(Instant i) {
    this.completionTime = i;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant i) {
    this.startTime = i;
  }
}
