package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.dto.type.DRAGENWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Samplesheet {
  private final Pattern HEADER = Pattern.compile("(?:\\[)(.*)(?:\\])");
  private List<SamplesheetSection> info;
  private Map<DRAGENWorkflow, Boolean> expectedWorkflows = new HashMap<>();
  static ObjectMapper mapper;
  private static final Logger log = LoggerFactory.getLogger(Samplesheet.class);
  private Instant mtime;

  public Samplesheet(ObjectMapper m, File rootDir) throws IOException {
    mapper = m;
    this.process(rootDir);
  }

  public SamplesheetSection getByName(String name) {
    List<SamplesheetSection> found = info.stream().filter(i -> i.getName().equals(name)).toList();
    if (found.isEmpty()) return null;
    if (found.size() > 1)
      throw new IllegalStateException("More than one section in Samplesheet with the same name.");
    return found.get(0);
  }

  private void addToSamplesheet(SamplesheetSection section) {
    SamplesheetSection potentiallyExtant = getByName(section.getName());
    if (potentiallyExtant != null) {
      info.remove(section);
    }
    info.add(section);
  }

  private void process(File rootDir) throws IOException {
    // Both copies DRAGEN makes of the SampleSheet are within BCLConvert
    // This is probably OK because we can't do any more analysis without it
    File sampleSheet = new File(rootDir, "Data/BCLConvert/SampleSheet.csv");
    if (sampleSheet.exists()) {
      this.mtime = Files.getLastModifiedTime(sampleSheet.toPath()).toInstant();
      List<String[]> lines =
          Files.readAllLines(sampleSheet.toPath())
              .stream()
              .map(line -> line.split(","))
              .filter(line -> !(line.length == 0 || line.length == 1 && line[0].isEmpty()))
              .toList();

      info = new LinkedList<>();
      SamplesheetBCLConvertSection bclConvertSection;
      Matcher headerMatcher;
      String sectionName = "";
      for (String[] line : lines) {
        headerMatcher = HEADER.matcher(line[0]);
        if (headerMatcher.matches()) {
          sectionName = headerMatcher.group(0);
          continue;
        }
        switch (sectionName) {
          case "BCLConvert_Data":
            if (line[0].equals("Lane")) break; // Skip column label line
            bclConvertSection = (SamplesheetBCLConvertSection) getByName("BCLConvert");
            if (bclConvertSection == null) {
              bclConvertSection = new SamplesheetBCLConvertSection();
            }
            bclConvertSection.addDatum(line[0], line[1], line[2], line[3]);
            addToSamplesheet(bclConvertSection);
            break;
          case "BCLConvert_Settings":
            bclConvertSection = (SamplesheetBCLConvertSection) getByName("BCLConvert");
            if (bclConvertSection == null) {
              bclConvertSection = new SamplesheetBCLConvertSection();
            }
            bclConvertSection.addSetting(line[0], line[1]);
            addToSamplesheet(bclConvertSection);
            expectedWorkflows.put(DRAGENWorkflow.BCL_CONVERT, Boolean.FALSE);
          case "Cloud_Settings": // Discard the cloud config
          case "Cloud_Data":
            break;
          default:
            if (line[0].startsWith("[")) continue; // Skip header lines we don't recognize
            SamplesheetGenericSection section = (SamplesheetGenericSection) getByName(sectionName);
            if (section == null) {
              section = new SamplesheetGenericSection(sectionName);
            }
            section.addEntry(new Pair<>(line[0], line[1]));
            addToSamplesheet(section);
        }
      }
    } else {
      // Samplesheet appears several hours after Analysis directory does
      log.info("No samplesheet for {}, will look again later", rootDir);
    }
  }

  public List<SamplesheetSection> getInfo() {
    return info;
  }

  public void setWorkflowComplete(DRAGENWorkflow wf) {
    throwForUnexpectedWorkflow(wf);
    expectedWorkflows.put(wf, Boolean.TRUE);
  }

  public Boolean isWorkflowCompleted(DRAGENWorkflow wf) {
    throwForUnexpectedWorkflow(wf);
    return expectedWorkflows.get(wf);
  }

  public boolean allWorkflowsCompleted() {
    return expectedWorkflows.values().stream().allMatch(b -> b.equals(Boolean.TRUE));
  }

  public boolean isWorkflowExpected(DRAGENWorkflow wf) {
    return expectedWorkflows.containsKey(wf);
  }

  public boolean noWorkflowsExpected() {
    return expectedWorkflows.isEmpty();
  }

  public Instant getMtime() {
    return mtime;
  }

  private void throwForUnexpectedWorkflow(DRAGENWorkflow wf) {
    if (!isWorkflowExpected(wf))
      throw new IllegalStateException(
          "Cannot perform operation for unexpected DRAGEN Workflow " + wf.name());
  }
}
