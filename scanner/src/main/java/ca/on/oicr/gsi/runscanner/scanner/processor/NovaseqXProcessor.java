package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.dragen.BCLConvert;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysis;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisStatus;
import ca.on.oicr.gsi.runscanner.dto.type.DRAGENWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaseqXProcessor extends DefaultIllumina {
  private Map<DRAGENWorkflow, Boolean> expectedWorkflows = new HashMap<>();
  private final Pattern HEADER = Pattern.compile("(?:\\[)(.*)(?:\\])");
  private final String NUMERAL = "\\d+";
  static ObjectMapper MAPPER = RunProcessor.createObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(NovaseqXProcessor.class);

  public NovaseqXProcessor(Builder builder, boolean checkOutput) {
    super(builder, checkOutput);
  }

  @Override
  public IlluminaNotificationDto analyse(
      File runDirectory, TimeZone tz, IlluminaNotificationDto dto) throws IOException {
    dto.setAnalysisStatus(AnalysisStatus.PENDING);
    DragenAnalysis dragenAnalysis = null;

    File analysisDir = new File(runDirectory, "Analysis");

    // For n in Analysis/n/Data (accommodate reruns, ish. if more reruns appear, they won't be
    // scanned. Someone will need to invalidate the run with the API.)
    if (analysisDir.exists() && analysisDir.isDirectory()) {
      // Null pointer should never actually happen because of above checks
      for (File analysisAttempt : Objects.requireNonNull(analysisDir.listFiles())) {
        if (analysisAttempt.isDirectory() && analysisAttempt.getName().matches(NUMERAL)) {
          Integer attemptNum = Integer.valueOf(analysisAttempt.getName());
          Samplesheet samplesheet = createSamplesheet(analysisAttempt);
          if (samplesheet.getInfo() == null) { // no info populated if samplesheet doesn't yet exist
            dto.setAnalysisStatus(AnalysisStatus.PENDING);
            return dto;
          }

          if (noWorkflowsExpected()) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
            return dto;
          }

          dragenAnalysis = new DragenAnalysis(MAPPER, samplesheet);

          if (isWorkflowExpected(DRAGENWorkflow.BCL_CONVERT)) {
            BCLConvert bclConvert = new BCLConvert(samplesheet, analysisAttempt);
            dragenAnalysis.put("BCLConvert", bclConvert.getResult());

            // TODO: Should we not put() anything if not OK?
            if (bclConvert.isOk()) setWorkflowComplete(DRAGENWorkflow.BCL_CONVERT);
          }

          // Phase 2: more workflows go here

          // TODO (Phase 2): move manifest parsing here so maybe we can avoid looping over it

          if (allWorkflowsCompleted()) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
          }
          dto.addAnalysis(attemptNum, dragenAnalysis);
        }
      }
    } else { // Analysis dir does not exist - we are not expecting DRAGEN for this run.
      dto.setAnalysisStatus(AnalysisStatus.NONE);
    }

    return dto;
  }

  public static NovaseqXProcessor create(Builder builder, ObjectNode parameters) {
    return new NovaseqXProcessor(builder, calculateCheckOutput(parameters));
  }

  private Samplesheet createSamplesheet(File rootDir) throws IOException {
    // Both copies DRAGEN makes of the SampleSheet are within BCLConvert
    // This is probably OK because we can't do any more analysis without it
    Samplesheet temp = new Samplesheet();
    File sampleSheet = new File(rootDir, "Data/BCLConvert/SampleSheet.csv");
    if (sampleSheet.exists()) {
      temp.setMtime(Files.getLastModifiedTime(sampleSheet.toPath()).toInstant());
      List<String[]> lines =
          Files.readAllLines(sampleSheet.toPath())
              .stream()
              .map(line -> line.split(","))
              .filter(line -> !(line.length == 0 || line.length == 1 && line[0].isEmpty()))
              .toList();

      SamplesheetBCLConvertSection bclConvertSection;
      Matcher headerMatcher;
      String sectionName = "";
      boolean bclDataFirstLine = true;
      int sampleIndex = 0, laneIndex = 0, indexIndex = 0, index2Index = 0;
      for (String[] line : lines) {
        headerMatcher = HEADER.matcher(line[0]);
        if (headerMatcher.matches()) {
          // "Capturing groups are indexed from left to right, starting at one.
          // Group zero denotes the entire pattern"
          sectionName = headerMatcher.group(1);
          continue;
        }
        switch (sectionName) {
          case "BCLConvert_Data":
            if (bclDataFirstLine) {
              for (int i = 0; i < line.length; i++) {
                switch (line[i]) {
                  case "Lane":
                    laneIndex = i;
                    break;
                  case "Sample_ID":
                    sampleIndex = i;
                    break;
                  case "Index":
                    indexIndex = i;
                    break;
                  case "Index2":
                    index2Index = i;
                    break;
                }
              }
              bclDataFirstLine = false;
            }
            bclConvertSection = (SamplesheetBCLConvertSection) temp.getByName("BCLConvert");
            if (bclConvertSection == null) {
              bclConvertSection = new SamplesheetBCLConvertSection();
            }
            bclConvertSection.addDatum(
                line[laneIndex], line[sampleIndex], line[indexIndex], line[index2Index]);
            temp.addToSamplesheet(bclConvertSection);
            expectedWorkflows.put(DRAGENWorkflow.BCL_CONVERT, Boolean.FALSE);
            break;
          case "BCLConvert_Settings":
            bclConvertSection = (SamplesheetBCLConvertSection) temp.getByName("BCLConvert");
            if (bclConvertSection == null) {
              bclConvertSection = new SamplesheetBCLConvertSection();
            }
            bclConvertSection.addSetting(line[0], line[1]);
            temp.addToSamplesheet(bclConvertSection);
            expectedWorkflows.put(DRAGENWorkflow.BCL_CONVERT, Boolean.FALSE);
          default:
            break;
        }
      }
    } else {
      // Samplesheet appears several hours after Analysis directory does
      log.info("No samplesheet for {}, will look again later", rootDir);
    }
    return temp;
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

  private void throwForUnexpectedWorkflow(DRAGENWorkflow wf) {
    if (!isWorkflowExpected(wf))
      throw new IllegalStateException(
          "Cannot perform operation for unexpected DRAGEN Workflow " + wf.name());
  }
}
