package ca.on.oicr.gsi.runscanner.dto.dragen;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

// Represents one file output by DRAGEN
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "format")
@JsonSubTypes({ //
  @Type(value = FastqAnalysisFile.class, name = "fastq"), //
}) //
public abstract class AnalysisFile {
  private Path path;
  private String crc32Checksum;
  private long size;
  private Instant createdTime;
  private Instant modifiedTime;

  public Instant getCreatedTime() {
    return createdTime;
  }

  public Instant getModifiedTime() {
    return modifiedTime;
  }

  public Path getPath() {
    return path;
  }

  public void setCreatedTime(Instant createdTime) {
    this.createdTime = createdTime;
  }

  public void setModifiedTime(Instant modifiedTime) {
    this.modifiedTime = modifiedTime;
  }

  public void setPath(Path p) {
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
    return "AnalysisFile [path="
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
