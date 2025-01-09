package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.WorkflowAnalysis;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DragenWorkflowAnalysis extends WorkflowAnalysis {
  private List<DragenAnalysisUnit> analyses = new LinkedList<>();

  public DragenWorkflowAnalysis(@JsonProperty("workflowName") String workflowName) {
    super(workflowName);
  }

  public List<DragenAnalysisUnit> getAnalyses() {
    return analyses;
  }

  public DragenAnalysisUnit get(String sample, String lane, String index1, String index2) {
    return get(sample, lane, new StringBuilder(index1).append("-").append(index2).toString());
  }

  public DragenAnalysisUnit get(String sample, int lane, String index1, String index2) {
    return get(sample, lane, new StringBuilder(index1).append("-").append(index2).toString());
  }

  public DragenAnalysisUnit get(String sample, String lane, String index) {
    return get(sample, Integer.parseInt(lane), index);
  }

  public DragenAnalysisUnit get(String sample, int lane, String index) {
    return analyses
        .stream()
        .filter(
            a -> a.getSample().equals(sample) && a.getLane() == lane && a.getIndex().equals(index))
        .findFirst()
        .orElse(null);
  }

  public DragenAnalysisUnit get(Path filePath) {
    List<DragenAnalysisUnit> list =
        analyses
            .stream()
            .filter(a -> a.getFiles().stream().anyMatch(f -> f.getPath().equals(filePath)))
            .toList();
    if (list.size() > 1) {
      throw new IllegalStateException(
          "Can't have more than one Analysis unit with same file " + filePath);
    }
    if (list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }

  public void put(DragenAnalysisUnit newDragenAnalysisUnit) {
    for (AnalysisFile af : newDragenAnalysisUnit.getFiles()) {
      get(af.getPath()); // throw out the result, we just want the file path validation
    }
    DragenAnalysisUnit oldDragenAnalysisUnit =
        get(
            newDragenAnalysisUnit.getSample(),
            newDragenAnalysisUnit.getLane(),
            newDragenAnalysisUnit.getIndex());
    if (oldDragenAnalysisUnit != null && !oldDragenAnalysisUnit.isEmpty()) {
      analyses.remove(oldDragenAnalysisUnit);
    }
    analyses.add(newDragenAnalysisUnit);
  }

  public String toString() {
    return super.toString() + ", DragenWorkflowAnalysis [analyses=" + analyses + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    DragenWorkflowAnalysis other = (DragenWorkflowAnalysis) obj;

    return Objects.equals(this.analyses, other.analyses);
  }

  public int hashCode() {
    return Objects.hash(super.hashCode(), this.analyses);
  }
}
