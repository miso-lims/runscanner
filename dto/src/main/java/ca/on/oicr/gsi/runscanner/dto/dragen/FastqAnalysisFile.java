package ca.on.oicr.gsi.runscanner.dto.dragen;

import java.util.Objects;

public class FastqAnalysisFile extends AnalysisFile {
  private Integer readCount, readNumber;
  private String overrideCycles;

  public String getOverrideCycles() {
    return overrideCycles;
  }

  public Integer getReadCount() {
    return readCount;
  }

  public Integer getReadNumber() {
    return readNumber;
  }

  public void setOverrideCycles(String overrideCycles) {
    this.overrideCycles = overrideCycles;
  }

  public void setReadCount(int readCount) {
    this.readCount = readCount;
  }

  public void setReadNumber(int readNumber) {
    this.readNumber = readNumber;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", FastqAnalysisFile [readCount="
        + readCount
        + ", readNumber="
        + readNumber
        + ", overrideCycles="
        + overrideCycles
        + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    FastqAnalysisFile other = (FastqAnalysisFile) obj;

    return Objects.equals(this.readCount, other.readCount)
        && Objects.equals(this.readNumber, other.readNumber)
        && Objects.equals(this.overrideCycles, other.overrideCycles);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.overrideCycles, this.readCount, this.readNumber);
  }
}
