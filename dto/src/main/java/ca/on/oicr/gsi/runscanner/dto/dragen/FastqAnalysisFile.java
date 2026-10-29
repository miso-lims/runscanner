package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;
import java.util.Objects;

public class FastqAnalysisFile extends AnalysisFile {
  private Integer readCount, readNumber;
  private String crc32Checksum;

  // Plain CRC32 checksum as hex (from the BCLConvert manifest)
  public String getCrc32Checksum() {
    return crc32Checksum;
  }

  public void setCrc32Checksum(String crc32Checksum) {
    this.crc32Checksum = crc32Checksum;
  }

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

  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.FASTQ;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", FastqAnalysisFile [crc32Checksum="
        + crc32Checksum
        + ", readCount="
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
    FastqAnalysisFile other = (FastqAnalysisFile) obj;

    return Objects.equals(this.crc32Checksum, other.crc32Checksum)
        && Objects.equals(this.readCount, other.readCount)
        && Objects.equals(this.readNumber, other.readNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.crc32Checksum, this.readCount, this.readNumber);
  }
}
