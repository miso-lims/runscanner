package ca.on.oicr.gsi.runscanner.dto;

import java.time.Instant;

public abstract class WorkflowAnalysis {
  private Instant completionTime;
  private Instant startTime;
  private final String workflowName;

  public WorkflowAnalysis(String name) {
    this.workflowName = name;
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
