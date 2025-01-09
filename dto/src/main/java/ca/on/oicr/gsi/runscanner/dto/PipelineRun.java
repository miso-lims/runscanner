package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.dragen.DragenPipelineRun;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

// Represents one attempt at an analysis suite.
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "suite")
@JsonSubTypes({ //
  @Type(value = DragenPipelineRun.class, name = "DRAGEN") //
}) //
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class PipelineRun<T extends WorkflowRun> {
  private List<T> workflowRuns = new LinkedList<>();
  private final int attempt;

  protected PipelineRun(int attempt) {
    this.attempt = attempt;
  }

  public T get(String workflowName) {
    return workflowRuns
        .stream()
        .filter(a -> a.getWorkflowName().equals(workflowName))
        .findFirst()
        .orElse(null);
  }

  public void put(T t) {
    workflowRuns.add(t);
  }

  public int getAttempt() {
    return attempt;
  }

  public List<T> getWorkflowRuns() {
    return workflowRuns;
  }

  public String toString() {
    return "PipelineRun [workflowRuns=" + workflowRuns + ", attempt=" + attempt + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    PipelineRun<T> other = (PipelineRun<T>) obj;

    return Objects.equals(this.workflowRuns, other.workflowRuns)
        && Objects.equals(this.attempt, other.attempt);
  }

  public int hashCode() {
    return Objects.hash(this.workflowRuns, this.attempt);
  }
}
