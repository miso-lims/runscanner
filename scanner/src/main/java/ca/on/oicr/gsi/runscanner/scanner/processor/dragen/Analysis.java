package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedList;
import java.util.List;

// Represents one unit of analysis by a DRAGEN workflow, uniquely identified by sample name, lane,
// and index. May have many files associated with it (eg, Read 1 and Read 2 for BCLConvert)
public class Analysis {
  private String sample;
  private int lane;
  private String index;
  private List<AnalysisFile> files = new LinkedList<>();

  public String getSample() {
    return sample;
  }

  public int getLane() {
    return lane;
  }

  public String getIndex() {
    return index;
  }

  public List<AnalysisFile> getFiles() {
    return files;
  }

  public void setSample(String str) {
    this.sample = str;
  }

  public void setLane(int i) {
    this.lane = i;
  }

  public void setIndex(String str) {
    this.index = str;
  }

  public void setIndex(String index1, String index2) {
    this.setIndex(new StringBuilder(index1).append("-").append(index2).toString());
  }

  public void addFile(AnalysisFile file) {
    files.add(file);
  }

  boolean isEmpty() {
    return sample == null && lane == 0 && index == null && files.isEmpty();
  }

  ObjectNode toJson() {
    ObjectNode analysisJson = DragenAnalysis.mapper.createObjectNode();
    analysisJson.put("Sample", sample);
    analysisJson.put("Lane", lane);
    analysisJson.put("Index", index);
    ArrayNode filesArray = DragenAnalysis.mapper.createArrayNode();
    for (AnalysisFile file : files) {
      filesArray.add(file.toJson());
    }
    analysisJson.set("files", filesArray);
    return analysisJson;
  }
}
