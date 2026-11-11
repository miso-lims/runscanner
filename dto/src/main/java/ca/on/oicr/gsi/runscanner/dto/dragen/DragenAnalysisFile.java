package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;

// Represents one file output by a DRAGEN workflow
@JsonTypeInfo(
    use = Id.NAME,
    include = As.PROPERTY,
    property = "format",
    visible = true,
    defaultImpl = UnknownAnalysisFile.class)
@JsonSubTypes({ //
  @Type(value = FastqAnalysisFile.class, name = "fastq"), //
  @Type(value = UnknownAnalysisFile.class, name = "unknown"), //
}) //
public abstract class DragenAnalysisFile extends AnalysisFile {
  private String crc32Checksum;

  // Plain CRC32 checksum as hex (from the BCLConvert manifest)
  public String getCrc32Checksum() {
    return crc32Checksum;
  }

  public void setCrc32Checksum(String crc32Checksum) {
    this.crc32Checksum = crc32Checksum;
  }

  @Override
  public String toString() {
    return super.toString() + ", DragenAnalysisFile [crc32Checksum=" + crc32Checksum + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    DragenAnalysisFile other = (DragenAnalysisFile) obj;

    return Objects.equals(this.crc32Checksum, other.crc32Checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), this.crc32Checksum);
  }
}
