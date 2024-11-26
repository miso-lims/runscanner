package ca.on.oicr.gsi.runscanner.dto.dragen;

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

public class DragenWorkflowAnalysis {
  List<DragenAnalysisUnit> analyses = new LinkedList<>();
  Instant completionTime;
  Instant startTime;

  DragenAnalysisUnit get(String sample, String lane, String index1, String index2) {
    return get(sample, lane, new StringBuilder(index1).append("-").append(index2).toString());
  }

  DragenAnalysisUnit get(String sample, int lane, String index1, String index2) {
    return get(sample, lane, new StringBuilder(index1).append("-").append(index2).toString());
  }

  DragenAnalysisUnit get(String sample, String lane, String index) {
    return get(sample, Integer.parseInt(lane), index);
  }

  DragenAnalysisUnit get(String sample, int lane, String index) {
    return analyses
        .stream()
        .filter(a -> a.getSample().equals(sample) && a.getLane() == lane)
        .findFirst()
        .orElse(new DragenAnalysisUnit());
  }

  DragenAnalysisUnit get(Path filePath) {
    List<DragenAnalysisUnit> list =
        analyses
            .stream()
            .filter(a -> a.getFiles().stream().anyMatch(f -> f.getPath().equals(filePath)))
            .toList();
    if (list.size() > 1) {
      throw new IllegalStateException(
          "Can't have more than one Analysis with same file " + filePath);
    }
    if (list.isEmpty()) {
      return null;
    }
    return list.get(0);
  }

  void put(DragenAnalysisUnit newDragenAnalysisUnit) {
    DragenAnalysisUnit oldDragenAnalysisUnit =
        get(
            newDragenAnalysisUnit.getSample(),
            newDragenAnalysisUnit.getLane(),
            newDragenAnalysisUnit.getIndex());
    if (!oldDragenAnalysisUnit.isEmpty()) {
      analyses.remove(oldDragenAnalysisUnit);
    }
    analyses.add(newDragenAnalysisUnit);
  }

  public Instant getCompletionTime() {
    return completionTime;
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
