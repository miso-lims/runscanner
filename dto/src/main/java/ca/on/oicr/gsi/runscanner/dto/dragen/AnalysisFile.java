package ca.on.oicr.gsi.runscanner.dto.dragen;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Represents one file output by DRAGEN
public class AnalysisFile {
  private Path path;
  private String crc32Checksum;
  private long size;
  private Instant created;
  private Instant modified;

  // What will be in this map will be unique to the particular DRAGEN Workflow
  private Map<String, Object> info = new HashMap<>();

  public Instant getCreated() {
    return created;
  }

  public Instant getModified() {
    return modified;
  }

  public Path getPath() {
    return path;
  }

  public void setCreated(Instant created) {
    this.created = created;
  }

  public void setModified(Instant modified) {
    this.modified = modified;
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

  public Map<String, Object> getInfo() {
    return info;
  }

  public void addInfoItem(String k, Object v) {
    info.put(k, v);
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (getClass() != o.getClass()) return false;
    AnalysisFile ao = (AnalysisFile) o;

    return Objects.equals(path, ao.getPath())
        && Objects.equals(crc32Checksum, ao.getCrc32Checksum())
        && Objects.equals(size, ao.getSize())
        && Objects.equals(info, ao.getInfo())
        && Objects.equals(created, ao.getCreated())
        && Objects.equals(modified, ao.getModified());
  }

  public int hashCode() {
    return Objects.hash(path, crc32Checksum, size, info, created, modified);
  }
}
