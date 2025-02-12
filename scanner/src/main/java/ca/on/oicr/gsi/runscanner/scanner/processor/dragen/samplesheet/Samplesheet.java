package ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet;

import ca.on.oicr.gsi.runscanner.dto.type.DragenWorkflow;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.SamplesheetBCLConvertSection.SamplesheetBCLConvertDataEntry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Samplesheet {
  private enum BCL_DATA_COL {
    LANE("lane"),
    SAMPLE_ID("sample_id"),
    INDEX("index"),
    INDEX2("index2"),
    OVERRIDE_CYCLES("overrideCycles");

    private final String name;

    BCL_DATA_COL(String s) {
      name = s;
    }

    public String toString() {
      return this.name;
    }
  }

  public static class SamplesheetException extends Exception {
    public SamplesheetException(String s) {
      super(s);
    }
  }

  private List<SamplesheetSection> info;
  private Instant modifiedTime;
  private static final Pattern HEADER = Pattern.compile("(?:\\[)(.*)(?:\\])");
  private Set<DragenWorkflow> expectedWorkflows;
  private static final Logger log = LoggerFactory.getLogger(Samplesheet.class);

  public Samplesheet(File rootDir) throws SamplesheetException, IOException {
    info = new LinkedList<>();
    expectedWorkflows = new HashSet<>();
    // Both copies DRAGEN makes of the SampleSheet are within BCLConvert
    // This is probably OK because we can't do any more analysis without it
    File sampleSheet = new File(rootDir, "Data/BCLConvert/SampleSheet.csv");
    if (!sampleSheet.exists()) {
      // Samplesheet appears several hours after Analysis directory does
      log.info("No samplesheet for {}, will look again later", rootDir);
      return;
    }
    setModifiedTime(Files.getLastModifiedTime(sampleSheet.toPath()).toInstant());
    List<String[]> lines =
        Files.readAllLines(sampleSheet.toPath())
            .stream()
            .map(line -> line.split(","))
            .filter(line -> !(line.length == 0 || Arrays.stream(line).allMatch(String::isBlank)))
            .toList();
    SamplesheetReadsSection readsSection = (SamplesheetReadsSection) getByName("Reads");
    if (readsSection == null) {
      readsSection = new SamplesheetReadsSection();
    }
    SamplesheetBCLConvertSection bclConvertSection =
        (SamplesheetBCLConvertSection) getByName("BCLConvert");
    if (bclConvertSection == null) {
      bclConvertSection = new SamplesheetBCLConvertSection();
    }
    String sectionName = "";
    boolean bclDataFirstLine = true;
    Map<BCL_DATA_COL, Integer> lineIndices = new HashMap<>();

    for (String[] line : lines) {
      Matcher headerMatcher = HEADER.matcher(line[0]);
      if (headerMatcher.matches()) {
        // "Capturing groups are indexed from left to right, starting at one.
        // Group zero denotes the entire pattern"
        sectionName = headerMatcher.group(1);
        continue;
      }
      switch (sectionName) {
        case "Reads":
          int value = Integer.parseInt(line[1]);
          switch (line[0]) {
            case "Read1Cycles":
              readsSection.setRead1Cycles(value);
              break;
            case "Read2Cycles":
              readsSection.setRead2Cycles(value);
              break;
            case "Index1Cycles":
              readsSection.setIndex1Cycles(value);
              break;
            case "Index2Cycles":
              readsSection.setIndex2Cycles(value);
          }
          break;
        case "BCLConvert_Data":
          if (bclDataFirstLine) {
            for (int i = 0; i < line.length; i++) {
              switch (line[i]) {
                case "Lane":
                  lineIndices.put(BCL_DATA_COL.LANE, i);
                  break;
                case "Sample_ID":
                  lineIndices.put(BCL_DATA_COL.SAMPLE_ID, i);
                  break;
                case "Index":
                  lineIndices.put(BCL_DATA_COL.INDEX, i);
                  break;
                case "Index2":
                  lineIndices.put(BCL_DATA_COL.INDEX2, i);
                  break;
                case "OverrideCycles":
                  lineIndices.put(BCL_DATA_COL.OVERRIDE_CYCLES, i);
                  break;
              }
            }
            if (!(lineIndices.containsKey(BCL_DATA_COL.LANE)
                && lineIndices.containsKey(BCL_DATA_COL.SAMPLE_ID)
                && lineIndices.containsKey(BCL_DATA_COL.INDEX)
                && lineIndices.containsKey(BCL_DATA_COL.INDEX2))) {
              throw new SamplesheetException(
                  "Expected Lane, Sample_ID, Index, and Index2 in BCLConvert_Data, got "
                      + lineIndices.keySet());
            }
            bclDataFirstLine = false;
            continue;
          }
          if (lineIndices.get(BCL_DATA_COL.OVERRIDE_CYCLES) == null) {
            bclConvertSection.addDatum(
                line[lineIndices.get(BCL_DATA_COL.LANE)],
                line[lineIndices.get(BCL_DATA_COL.SAMPLE_ID)],
                line[lineIndices.get(BCL_DATA_COL.INDEX)],
                line[lineIndices.get(BCL_DATA_COL.INDEX2)],
                null);
          } else {
            bclConvertSection.addDatum(
                line[lineIndices.get(BCL_DATA_COL.LANE)],
                line[lineIndices.get(BCL_DATA_COL.SAMPLE_ID)],
                line[lineIndices.get(BCL_DATA_COL.INDEX)],
                line[lineIndices.get(BCL_DATA_COL.INDEX2)],
                line[lineIndices.get(BCL_DATA_COL.OVERRIDE_CYCLES)]);
          }
          addToSamplesheet(bclConvertSection);
          expectedWorkflows.add(DragenWorkflow.BCL_CONVERT);
          break;
        case "BCLConvert_Settings":
          switch (line[0]) {
            case "SoftwareVersion":
              bclConvertSection.setSoftwareVersion(line[1]);
              break;
            case "OverrideCycles":
              bclConvertSection.setOverrideCyclesSetting(line[1]);
              break;
          }
          this.addToSamplesheet(bclConvertSection);
          expectedWorkflows.add(DragenWorkflow.BCL_CONVERT);
        default:
          break;
      }
    }

    // If there is a BCLConvert section but no OverrideCycles column in BCLConvert_Data,
    // use the OverrideCycles from BCLConvert_Settings
    // If neither is available, Illumina says:
    // "the 'OverrideCycles' defaults [...] to match the run setup as applicable:
    // Y(Read 1); I(Index 1); I(Index 2): Y(Read 2)"
    if (!lineIndices.containsKey(BCL_DATA_COL.OVERRIDE_CYCLES)) {
      if (bclConvertSection.getOverrideCyclesSetting() != null) {
        for (SamplesheetBCLConvertDataEntry entry : bclConvertSection.getData()) {
          entry.setOverrideCycles(bclConvertSection.getOverrideCyclesSetting());
        }
      } else {
        try {
          String defaultOverrideCycles =
              new StringBuilder("Y")
                  .append(readsSection.getRead1Cycles())
                  .append(";I")
                  .append(readsSection.getIndex1Cycles())
                  .append(";I")
                  .append(readsSection.getIndex2Cycles())
                  .append(";Y")
                  .append(readsSection.getRead2Cycles())
                  .toString();
          for (SamplesheetBCLConvertDataEntry entry : bclConvertSection.getData()) {
            entry.setOverrideCycles(defaultOverrideCycles);
          }
        } catch (NullPointerException npe) {
          throw new IllegalStateException("Missing Reads Section field for " + rootDir, npe);
        }
      }
    }
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

  public boolean noneExpected() {
    return expectedWorkflows.isEmpty();
  }

  public boolean isExpected(DragenWorkflow dw) {
    return expectedWorkflows.contains(dw);
  }

  public String toString() {
    return "Samplesheet [info="
        + info
        + ", modifiedTime="
        + modifiedTime
        + ", expectedWorkflows="
        + expectedWorkflows
        + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Samplesheet other = (Samplesheet) obj;

    return Objects.equals(this.info, other.info)
        && Objects.equals(this.modifiedTime, other.modifiedTime)
        && Objects.equals(this.expectedWorkflows, other.expectedWorkflows);
  }

  public int hashCode() {
    return Objects.hash(this.info, this.modifiedTime, this.expectedWorkflows);
  }
}
