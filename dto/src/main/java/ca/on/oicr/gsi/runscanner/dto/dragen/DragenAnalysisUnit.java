package ca.on.oicr.gsi.runscanner.dto.dragen;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

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
    if (index2 == null) {
      this.setIndex(index1);
    } else {
      this.setIndex(new StringBuilder(index1).append("-").append(index2).toString());
    }
  }

  public void addFile(AnalysisFile file) {
    files.add(file);
  }

  @JsonIgnore
  public boolean isEmpty() {
    return sample == null && lane == 0 && index == null && files.isEmpty();
  }

  public String toString() {
    return "DragenAnalysisUnit [files="
        + files
        + ", index="
        + index
        + ", lane="
        + lane
        + ", sample="
        + sample
        + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DragenAnalysisUnit other = (DragenAnalysisUnit) obj;

    return Objects.equals(
            this.files, other.files) // TODO when we remove() in linkedlist, this might be a problem
        && Objects.equals(this.index, other.index)
        && Objects.equals(this.lane, other.lane)
        && Objects.equals(this.sample, other.sample);
  }

  public int hashCode() {
    return Objects.hash(files, index, lane, sample);
  }
}
