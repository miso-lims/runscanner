package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import java.util.Objects;

public class UltimaAnalysisFile extends AnalysisFile {
  private Integer readCount, readNumber;

  public Integer getReadCount() {
    return readCount;
  }

  public Integer getReadNumber() {
    return readNumber;
  }

  public void setReadCount(int readCount) {
    this.readCount = readCount;
  }

  public void setReadNumber(int readNumber) {
    this.readNumber = readNumber;
  }

  public void setCrc32Checksum() {
    super.setCrc32Checksum("unimplemented");
  }

  @Override
  public void setCrc32Checksum(String s) {
    super.setCrc32Checksum("unimplemented");
  }

  @Override
  public String toString() {
    return super.toString()
        + ", UltimaAnalysisFile [readCount="
        + readCount
        + ", readNumber="
        + readNumber
        + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaAnalysisFile other = (UltimaAnalysisFile) obj;

    return Objects.equals(this.readCount, other.readCount)
        && Objects.equals(this.readNumber, other.readNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.readCount, this.readNumber);
  }
}
