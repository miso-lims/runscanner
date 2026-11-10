package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;
import java.util.Objects;

// Represents one CRAM file
public class CramAnalysisFile extends UltimaAnalysisFile {
  private Long readCount;

  public Long getReadCount() {
    return readCount;
  }

  public void setReadCount(Long readCount) {
    this.readCount = readCount;
  }

  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.CRAM;
  }

  @Override
  public String toString() {
    return super.toString() + ", CramAnalysisFile [readCount=" + readCount + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    CramAnalysisFile other = (CramAnalysisFile) obj;

    return Objects.equals(this.readCount, other.readCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.readCount);
  }
}
