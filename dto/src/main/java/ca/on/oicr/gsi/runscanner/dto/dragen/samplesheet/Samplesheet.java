package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Samplesheet {
  private List<SamplesheetSection> info;
  private Instant modifiedTime;

  public Samplesheet() {
    info = new LinkedList<>();
  }

  public SamplesheetSection getByName(String name) {
    List<SamplesheetSection> found = info.stream().filter(i -> i.getName().equals(name)).toList();
    if (found.isEmpty()) return null;
    if (found.size() > 1)
      throw new IllegalStateException("More than one section in Samplesheet with the same name.");
    return found.get(0);
  }

  public void addToSamplesheet(SamplesheetSection section) {
    SamplesheetSection potentiallyExtant = getByName(section.getName());
    if (potentiallyExtant != null) {
      info.remove(section);
    }
    info.add(section);
  }

  public List<SamplesheetSection> getInfo() {
    return info;
  }

  public Instant getModifiedTime() {
    return modifiedTime;
  }

  public void setModifiedTime(Instant modifiedTime) {
    this.modifiedTime = modifiedTime;
  }

  public String toString() {
    return "Samplesheet [info=" + info + ", modifiedTime=" + modifiedTime + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Samplesheet other = (Samplesheet) obj;

    return Objects.equals(this.info, other.info)
        && Objects.equals(this.modifiedTime, other.modifiedTime);
  }

  public int hashCode() {
    return Objects.hash(this.info, this.modifiedTime);
  }
}
