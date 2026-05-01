package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

// Represents one unit of analysis by a Ultima workflow, uniquely identified by sample name, lane,
// and index. May have many files associated with it (eg, Read 1 and Read 2 for BCLConvert)
public class UltimaAnalysisUnit {
  private String sample, index;
  private List<AnalysisFile> files = new LinkedList<>();

  public String getIndex() {
    return index;
  }

  public String getSample() {
    return sample;
  }

  public List<AnalysisFile> getFiles() {
    return files;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public void setSample(String str) {
    this.sample = str;
  }

  public void addFile(AnalysisFile file) {
    files.add(file);
  }

  @JsonIgnore
  public boolean isEmpty() {
    return sample == null && index == null && files.isEmpty();
  }

  public String toString() {
    return "UltimaAnalysisUnit [files=" + files + ", index=" + index + ", sample=" + sample + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaAnalysisUnit other = (UltimaAnalysisUnit) obj;

    return Objects.equals(this.files, other.files) && Objects.equals(this.sample, other.sample);
  }

  public int hashCode() {
    return Objects.hash(files, index, sample);
  }
}
