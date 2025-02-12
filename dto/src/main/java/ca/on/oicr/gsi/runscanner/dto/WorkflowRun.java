package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.dragen.DragenWorkflowRun;
import ca.on.oicr.gsi.runscanner.dto.type.WorkflowRunStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import java.util.Objects;

// Represents one attempt at a workflow within a suite
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "suite")
@JsonSubTypes({ //
  @Type(value = DragenWorkflowRun.class, name = "DRAGEN") //
}) //
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class WorkflowRun {
  private Instant completionTime;
  private Instant startTime;
  private final String workflowName;
  private WorkflowRunStatus workflowRunStatus;
  private String softwareVersion;

  public WorkflowRun(String workflowName) {
    this.workflowName = workflowName;
    this.workflowRunStatus = WorkflowRunStatus.PENDING;
  }

  public Instant getCompletionTime() {
    return completionTime;
  }

  public String getSoftwareVersion() {
    return softwareVersion;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public WorkflowRunStatus getWorkflowRunStatus() {
    return workflowRunStatus;
  }

  public void setCompletionTime(Instant i) {
    this.completionTime = i;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public void setSoftwareVersion(String softwareVersion) {
    this.softwareVersion = softwareVersion;
  }

  public void setStartTime(Instant i) {
    this.startTime = i;
  }

  public void complete() {
    this.workflowRunStatus = WorkflowRunStatus.COMPLETE;
  }

  public void fail() {
    this.workflowRunStatus = WorkflowRunStatus.FAILED;
  }

  public String toString() {
    return "WorkflowRun [completionTime="
        + completionTime
        + ", startTime="
        + startTime
        + ", workflowName="
        + workflowName
        + ", workflowRunStatus="
        + workflowRunStatus
        + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    WorkflowRun other = (WorkflowRun) obj;

    return Objects.equals(this.completionTime, other.completionTime)
        && Objects.equals(this.workflowName, other.workflowName)
        && Objects.equals(this.startTime, other.startTime)
        && Objects.equals(this.workflowRunStatus, other.workflowRunStatus);
  }

  public int hashCode() {
    return Objects.hash(completionTime, workflowName, startTime, workflowRunStatus);
  }
}
