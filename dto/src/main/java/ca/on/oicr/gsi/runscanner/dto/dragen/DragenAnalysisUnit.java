package ca.on.oicr.gsi.runscanner.dto.dragen;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

// Represents one unit of analysis by a DRAGEN workflow, uniquely identified by sample name, lane,
// and index. May have many files associated with it (eg, Read 1 and Read 2 for BCLConvert)
public class DragenAnalysisUnit {
  private String sample, index1, index2, overrideCycles;
  private int lane;
  private List<AnalysisFile> files = new LinkedList<>();

  public String getIndex1() {
    return index1;
  }

  public String getIndex2() {
    return index2;
  }

  public String getSample() {
    return sample;
  }

  public int getLane() {
    return lane;
  }

  public String getOverrideCycles() {
    return overrideCycles;
  }

  public List<AnalysisFile> getFiles() {
    return files;
  }

  public void setIndex1(String index1) {
    this.index1 = index1;
  }

  public void setIndex2(String index2) {
    this.index2 = index2;
  }

  public void setSample(String str) {
    this.sample = str;
  }

  public void setLane(int i) {
    this.lane = i;
  }

  public void setOverrideCycles(String overrideCycles) {
    this.overrideCycles = overrideCycles;
  }

  public void addFile(AnalysisFile file) {
    files.add(file);
  }

  @JsonIgnore
  public boolean isEmpty() {
    return sample == null && lane == 0 && index1 == null && index2 == null && files.isEmpty();
  }

  public String toString() {
    return "DragenAnalysisUnit [files="
        + files
        + ", index1="
        + index1
        + ", index2="
        + index2
        + ", lane="
        + lane
        + ", sample="
        + sample
        + ", overrideCycles="
        + overrideCycles
        + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    DragenAnalysisUnit other = (DragenAnalysisUnit) obj;

    return Objects.equals(this.files, other.files)
        && Objects.equals(this.index1, other.index1)
        && Objects.equals(this.index2, other.index2)
        && Objects.equals(this.lane, other.lane)
        && Objects.equals(this.sample, other.sample)
        && Objects.equals(this.overrideCycles, other.overrideCycles);
  }

  public int hashCode() {
    return Objects.hash(files, index1, index2, lane, sample, overrideCycles);
  }
}
