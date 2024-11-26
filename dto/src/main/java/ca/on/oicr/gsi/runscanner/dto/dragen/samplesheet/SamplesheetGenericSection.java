package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

import ca.on.oicr.gsi.Pair;
import java.util.LinkedList;
import java.util.List;

public class SamplesheetGenericSection implements SamplesheetSection {
  private String name;
  private List<Pair<String, String>> entries;

  public SamplesheetGenericSection(String name) {
    this.name = name;
    this.entries = new LinkedList<>();
  }

  public List<Pair<String, String>> getEntries() {
    return entries;
  }

  @Override
  public String getName() {
    return name;
  }

  public void addEntry(Pair<String, String> entry) {
    this.entries.add(entry);
  }
}
