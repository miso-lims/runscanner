package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.WorkflowRun;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class UltimaWorkflowRun extends WorkflowRun {

  private List<UltimaAnalysisUnit> analysisOutputs = new LinkedList<>();

  public UltimaWorkflowRun(@JsonProperty("workflowName") String workflowName) {
    super(workflowName);
  }

  public List<UltimaAnalysisUnit> getAnalysisOutputs() {
    return analysisOutputs;
  }

  public void setAnalysisOutputs(List<UltimaAnalysisUnit> analysisOutputs) {
    this.analysisOutputs = analysisOutputs;
  }

  @Override
  public String toString() {
    return super.toString() + ", UltimaWorkflowRun [analysisOutputs=" + analysisOutputs + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaWorkflowRun other = (UltimaWorkflowRun) obj;
    return Objects.equals(this.analysisOutputs, other.analysisOutputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), analysisOutputs);
  }
}
