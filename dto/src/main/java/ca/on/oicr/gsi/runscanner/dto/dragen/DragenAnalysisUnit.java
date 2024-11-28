package ca.on.oicr.gsi.runscanner.dto.dragen;

import java.util.LinkedList;
import java.util.List;

// Represents one unit of analysis by a DRAGEN workflow, uniquely identified by sample name, lane,
// and index. May have many files associated with it (eg, Read 1 and Read 2 for BCLConvert)
public class DragenAnalysisUnit {
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

  public boolean isEmpty() {
    return sample == null && lane == 0 && index == null && files.isEmpty();
  }
}
