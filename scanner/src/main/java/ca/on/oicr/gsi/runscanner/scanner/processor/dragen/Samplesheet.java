package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import ca.on.oicr.gsi.runscanner.dto.type.DRAGENWorkflow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Samplesheet {
  private ObjectNode info;
  private Map<DRAGENWorkflow, Boolean> expectedWorkflows = new HashMap<>();
  static ObjectMapper mapper;
  private static final Logger log = LoggerFactory.getLogger(Samplesheet.class);
  private Instant mtime;

  public Samplesheet(ObjectMapper m, File rootDir) throws IOException {
    mapper = m;
    this.process(rootDir);
  }

  private void process(File rootDir) throws IOException {
    // Both copies DRAGEN makes of the SampleSheet are within BCLConvert
    // This is probably OK because we can't do any more analysis without it
    File sampleSheet = new File(rootDir, "Data/BCLConvert/SampleSheet.csv");
    this.mtime = Files.getLastModifiedTime(sampleSheet.toPath()).toInstant();
    if (sampleSheet.exists()) {
      List<String[]> lines =
          Files.readAllLines(sampleSheet.toPath())
              .stream()
              .map(line -> line.split(","))
              .filter(line -> line.length != 0)
              .toList();

      info = mapper.createObjectNode();
      ObjectNode sampleSheetBCLConvertSection = mapper.createObjectNode(),
          sampleSheetBCLConvertData = mapper.createObjectNode(),
          sampleSheetBCLConvertSettings = mapper.createObjectNode();
      int s = 0;
      String sectionName = "";
      for (String[] line : lines) {
        if (line[0].startsWith("[")) {
          sectionName = line[0];
          s = 0;
          continue;
        }
        switch (sectionName) {
          case "[BCLConvert_Data]":
            if (line[0].equals("Lane")) break; // Skip column label line
            s++;
            ObjectNode nested = mapper.createObjectNode();
            nested.put("Lane", line[0]);
            nested.put("Sample_ID", line[1]);
            nested.put("Index", line[2]);
            nested.put("Index2", line[3]); // Some have a line[4] like "Y27;I10;I10;Y27"
            sampleSheetBCLConvertData.set(Integer.toString(s), nested);
            break;
          case "[BCLConvert_Settings]":
            sampleSheetBCLConvertSettings.put(line[0], line[1]);
            expectedWorkflows.put(DRAGENWorkflow.BCL_CONVERT, Boolean.FALSE);
          case "[Cloud_Settings]": // Discard the cloud config
          case "[Cloud_Data]":
            break;
          default:
            info.put(line[0], line[1]);
        }
      }
      sampleSheetBCLConvertSection.set("Data", sampleSheetBCLConvertData);
      sampleSheetBCLConvertSection.set("Settings", sampleSheetBCLConvertSettings);
      info.set("BCLConvert", sampleSheetBCLConvertSection);
    } else {
      // Samplesheet appears several hours after Analysis directory does
      log.info("No samplesheet for {}, will look again later", rootDir);
    }
  }

  public JsonNode getInfo() {
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
