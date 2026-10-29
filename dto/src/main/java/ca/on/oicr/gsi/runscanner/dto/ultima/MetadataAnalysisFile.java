package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;
import java.util.Objects;

// Represents a metadata file (e.g. csv, index, JSON, log)
public class MetadataAnalysisFile extends AnalysisFile {
  private String crc32cBase64Checksum;

  // CRC32C checksum as base64 of its 4 big-endian bytes (as reported by Google Cloud Storage)
  public String getCrc32cBase64Checksum() {
    return crc32cBase64Checksum;
  }

  public void setCrc32cBase64Checksum(String crc32cBase64Checksum) {
    this.crc32cBase64Checksum = crc32cBase64Checksum;
  }

  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.METADATA;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", MetadataAnalysisFile [crc32cBase64Checksum="
        + crc32cBase64Checksum
        + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    MetadataAnalysisFile other = (MetadataAnalysisFile) obj;

    return Objects.equals(this.crc32cBase64Checksum, other.crc32cBase64Checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.crc32cBase64Checksum);
  }
}
