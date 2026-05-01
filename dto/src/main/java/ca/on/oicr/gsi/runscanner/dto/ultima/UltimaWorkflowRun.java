package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.WorkflowRun;
import ca.on.oicr.gsi.runscanner.dto.type.UploadStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class UltimaWorkflowRun extends WorkflowRun {
  private UploadStatus uploadStatus;
  private List<UltimaAnalysisUnit> analysisOutputs = new LinkedList<>();

  public UltimaWorkflowRun(@JsonProperty("workflowName") String workflowName) {
    super(workflowName);
    this.uploadStatus = UploadStatus.NOTSTARTED;
  }

  public List<UltimaAnalysisUnit> getAnalysisOutputs() {
    return analysisOutputs;
  }

  public UploadStatus getUploadStatus() {
    return uploadStatus;
  }

  public void uploadComplete() {
    this.uploadStatus = UploadStatus.COMPLETE;
  }

  public void uploadFail() {
    this.uploadStatus = UploadStatus.ERRORED;
  }

  public void uploadStart() {
    this.uploadStatus = UploadStatus.PENDING;
  }

  public UltimaAnalysisUnit get(String sample, String index) {
    return analysisOutputs
        .stream()
        .filter(a -> a.getSample().equals(sample) && a.getIndex().equals(index))
        .findFirst()
        .orElse(null);
  }

  public UltimaAnalysisUnit get(Path filePath) {
    List<UltimaAnalysisUnit> list =
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
  public void put(UltimaAnalysisUnit newUltimaAnalysisUnit) {
    for (AnalysisFile af : newUltimaAnalysisUnit.getFiles()) {
      get(af.getPath()); // throw out the result, we just want the file path validation
    }
    UltimaAnalysisUnit oldUltimaAnalysisUnit =
        get(newUltimaAnalysisUnit.getSample(), newUltimaAnalysisUnit.getIndex());
    if (oldUltimaAnalysisUnit != null && !oldUltimaAnalysisUnit.isEmpty()) {
      analysisOutputs.remove(oldUltimaAnalysisUnit);
    }
    analysisOutputs.add(newUltimaAnalysisUnit);
  }

  public String toString() {
    return super.toString()
        + ", UltimaWorkflowRun [analysisOutputs="
        + analysisOutputs
        + ", uploadStatus="
        + uploadStatus
        + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaWorkflowRun other = (UltimaWorkflowRun) obj;

    return Objects.equals(this.analysisOutputs, other.analysisOutputs)
        && Objects.equals(this.uploadStatus, other.uploadStatus);
  }

  public int hashCode() {
    return Objects.hash(super.hashCode(), this.analysisOutputs, this.uploadStatus);
  }
}
