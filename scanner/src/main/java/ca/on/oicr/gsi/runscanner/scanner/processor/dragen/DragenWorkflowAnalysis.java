package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

public class DragenWorkflowAnalysis {
  List<Analysis> analyses = new LinkedList<>();

  Analysis get(String sample, String lane, String index1, String index2) {
    return get(sample, lane, new StringBuilder(index1).append("-").append(index2).toString());
  }

  Analysis get(String sample, int lane, String index1, String index2) {
    return get(sample, lane, new StringBuilder(index1).append("-").append(index2).toString());
  }

  Analysis get(String sample, String lane, String index) {
    return get(sample, Integer.parseInt(lane), index);
  }

  Analysis get(String sample, int lane, String index) {
    return analyses
        .stream()
        .filter(a -> a.getSample().equals(sample) && a.getLane() == lane)
        .findFirst()
        .orElse(new Analysis());
  }

  Analysis get(Path filePath) {
    List<Analysis> list =
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

  void put(Analysis newAnalysis) {
    Analysis oldAnalysis =
        get(newAnalysis.getSample(), newAnalysis.getLane(), newAnalysis.getIndex());
    if (!oldAnalysis.isEmpty()) {
      analyses.remove(oldAnalysis);
    }
    analyses.add(newAnalysis);
  }

  ArrayNode toJson() {
    ArrayNode ret = DragenAnalysis.mapper.createArrayNode();
    analyses
        .stream()
        .sorted(
            (a1, a2) -> {
              int sampleComp = a1.getSample().compareTo(a2.getSample());
              if (sampleComp == 0) { // same sample
                int laneComp = Integer.compare(a1.getLane(), a2.getLane());
                if (laneComp == 0) { // same lane
                  return a1.getIndex().compareTo(a2.getIndex());
                }
                return laneComp;
              }
              return sampleComp;
            })
        .forEach(a -> ret.add(a.toJson()));
    return ret;
  }
}
