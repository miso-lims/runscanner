package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.dragen.FastqAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;
import ca.on.oicr.gsi.runscanner.dto.ultima.CramAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.MetadataAnalysisFile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;

// Represents one file output by a sequencing workflow
@JsonTypeInfo(
    use = Id.NAME,
    include = As.PROPERTY,
    property = "format",
    visible = true,
    defaultImpl = UnknownAnalysisFile.class)
@JsonSubTypes({ //
  @Type(value = FastqAnalysisFile.class, name = "fastq"), //
  @Type(value = CramAnalysisFile.class, name = "cram"), //
  @Type(value = MetadataAnalysisFile.class, name = "metadata"), //
  @Type(value = UnknownAnalysisFile.class, name = "unknown"), //
}) //
public abstract class AnalysisFile {
  private URI path;
  private String crc32Checksum;
  private long size;
  private Instant createdTime;
  private Instant modifiedTime;
  private String rawFormat;

  /**
   * The format of this file. Jackson consumes the serialized "format" property as the type
   * discriminator, so this accessor is what lets consumers read the format back off a deserialized
   * AnalysisFile. It is ignored on the wire to avoid writing the format twice.
   */
  @JsonIgnore
  public abstract AnalysisFileFormat getFormatType();

  /**
   * The "format" discriminator exactly as it appeared in the JSON this object was deserialized
   * from, or null if this object was built in code rather than read off the wire.
   *
   * <p>For a recognised format this is just {@code getFormatType().getFormat()}. It earns its keep
   * on an {@link UnknownAnalysisFile}, where it is the only record of what the producer actually
   * called the format; the enum can say no more than {@link AnalysisFileFormat#UNKNOWN}.
   *
   * <p>Deliberately excluded from equals/hashCode: it describes how this object was obtained, not
   * which file it denotes, and including it would make a file built by the scanner unequal to the
   * deserialized copy of itself.
   */
  @JsonIgnore
  public String getRawFormat() {
    return rawFormat;
  }

  /**
   * Populated by Jackson from the type discriminator, thanks to {@code visible = true}. Not
   * serialized — Jackson's type serializer is solely responsible for writing "format".
   */
  @JsonProperty("format")
  public void setRawFormat(String rawFormat) {
    this.rawFormat = rawFormat;
  }

  public Instant getCreatedTime() {
    return createdTime;
  }

  public Instant getModifiedTime() {
    return modifiedTime;
  }

  public URI getPath() {
    return path;
  }

  public void setCreatedTime(Instant createdTime) {
    this.createdTime = createdTime;
  }

  public void setModifiedTime(Instant modifiedTime) {
    this.modifiedTime = modifiedTime;
  }

  public void setPath(URI p) {
    this.path = p;
  }

  public String getCrc32Checksum() {
    return crc32Checksum;
  }

  public void setCrc32Checksum(String s) {
    this.crc32Checksum = s;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long l) {
    this.size = l;
  }

  public String toString() {
    return "AnalysisFile [format="
        + getFormatType()
        + ", rawFormat="
        + rawFormat
        + ", path="
        + path
        + ", crc32Checksum="
        + crc32Checksum
        + ", size="
        + size
        + ", createdTime="
        + createdTime
        + ", modifiedTime="
        + modifiedTime
        + "]";
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    AnalysisFile ao = (AnalysisFile) o;

    return Objects.equals(path, ao.getPath())
        && Objects.equals(crc32Checksum, ao.getCrc32Checksum())
        && Objects.equals(size, ao.getSize())
        && Objects.equals(createdTime, ao.getCreatedTime())
        && Objects.equals(modifiedTime, ao.getModifiedTime());
  }

  public int hashCode() {
    return Objects.hash(path, crc32Checksum, size, createdTime, modifiedTime);
  }
}
