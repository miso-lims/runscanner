package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;

// Represents one file output by an Ultima workflow
@JsonTypeInfo(
    use = Id.NAME,
    include = As.PROPERTY,
    property = "format",
    visible = true,
    defaultImpl = UnknownAnalysisFile.class)
@JsonSubTypes({ //
  @Type(value = CramAnalysisFile.class, name = "cram"), //
  @Type(value = IndexAnalysisFile.class, name = "index"), //
  @Type(value = MetadataAnalysisFile.class, name = "metadata"), //
  @Type(value = UnknownAnalysisFile.class, name = "unknown"), //
}) //
public abstract class UltimaAnalysisFile extends AnalysisFile {
  private String crc32cBase64Checksum;

  // CRC32C checksum as base64 of its 4 big-endian bytes (the format reported by Google Cloud
  // Storage)
  public String getCrc32cBase64Checksum() {
    return crc32cBase64Checksum;
  }

  public void setCrc32cBase64Checksum(String crc32cBase64Checksum) {
    this.crc32cBase64Checksum = crc32cBase64Checksum;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", UltimaAnalysisFile [crc32cBase64Checksum="
        + crc32cBase64Checksum
        + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaAnalysisFile other = (UltimaAnalysisFile) obj;

    return Objects.equals(this.crc32cBase64Checksum, other.crc32cBase64Checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.crc32cBase64Checksum);
  }
}
