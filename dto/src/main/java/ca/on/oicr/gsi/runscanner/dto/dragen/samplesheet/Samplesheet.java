package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;

public class Samplesheet {
  private List<SamplesheetSection> info;
  private Instant mtime;

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

  public Instant getMtime() {
    return mtime;
  }

  public void setMtime(Instant mtime) {
    this.mtime = mtime;
  }
}
