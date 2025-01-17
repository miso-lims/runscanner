package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.WorkflowRun;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DragenWorkflowRun extends WorkflowRun {
  private List<DragenAnalysisUnit> analysisOutputs = new LinkedList<>();

  public DragenWorkflowRun(@JsonProperty("workflowName") String workflowName) {
    super(workflowName);
  }

  public List<DragenAnalysisUnit> getAnalysisOutputs() {
    return analysisOutputs;
  }

  public DragenAnalysisUnit get(String sample, String lane, String index1, String index2) {
    return get(sample, Integer.parseInt(lane), index1, index2);
  }

  public DragenAnalysisUnit get(String sample, int lane, String index1, String index2) {
    return analysisOutputs
        .stream()
        .filter(
            a ->
                a.getSample().equals(sample)
                    && a.getLane() == lane
                    && a.getIndex1().equals(index1)
                    && a.getIndex2().equals(index2))
        .findFirst()
        .orElse(null);
  }

  public DragenAnalysisUnit get(Path filePath) {
    List<DragenAnalysisUnit> list =
        analysisOutputs
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
  // TODO all this might be unneccessary? Memory addresses seem consistent when getting
  public void put(DragenAnalysisUnit newDragenAnalysisUnit) {
    for (AnalysisFile af : newDragenAnalysisUnit.getFiles()) {
      get(af.getPath()); // throw out the result, we just want the file path validation
    }
    DragenAnalysisUnit oldDragenAnalysisUnit =
        get(
            newDragenAnalysisUnit.getSample(),
            newDragenAnalysisUnit.getLane(),
            newDragenAnalysisUnit.getIndex1(),
            newDragenAnalysisUnit.getIndex2());
    if (oldDragenAnalysisUnit != null && !oldDragenAnalysisUnit.isEmpty()) {
      analysisOutputs.remove(oldDragenAnalysisUnit);
    }
    analysisOutputs.add(newDragenAnalysisUnit);
  }

  public String toString() {
    return super.toString() + ", DragenWorkflowRun [analysisOutputs=" + analysisOutputs + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    DragenWorkflowRun other = (DragenWorkflowRun) obj;

    return Objects.equals(this.analysisOutputs, other.analysisOutputs);
  }

  public int hashCode() {
    return Objects.hash(super.hashCode(), this.analysisOutputs);
  }
}
