package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection;
import ca.on.oicr.gsi.runscanner.dto.type.DragenWorkflow;
import ca.on.oicr.gsi.runscanner.dto.type.PipelineStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDragen {
  private static Set<DragenWorkflow> expectedWorkflows;
  private static final Pattern HEADER = Pattern.compile("(?:\\[)(.*)(?:\\])");
  private static final String NUMERAL = "\\d+";
  private static final Logger log = LoggerFactory.getLogger(ProcessDragen.class);

  public static IlluminaNotificationDto analyse(
      File runDirectory, TimeZone tz, IlluminaNotificationDto dto) throws IOException {
    DragenPipelineRun dragenPipelineRun = null;

    File analysisDir = new File(runDirectory, "Analysis");

    // For n in Analysis/n/Data (accommodate reruns, ish. if more reruns appear, they won't be
    // scanned. Someone will need to invalidate the run with the API.)
    if (analysisDir.exists() && analysisDir.isDirectory()) {
      dto.setAnalysisExpected(true);
      expectedWorkflows = new HashSet<>();
      // Null pointer should never actually happen because of above checks
      for (File analysisAttempt : Objects.requireNonNull(analysisDir.listFiles())) {
        if (analysisAttempt.isDirectory() && analysisAttempt.getName().matches(NUMERAL)) {
          int attemptNum = Integer.parseInt(analysisAttempt.getName());
          Samplesheet samplesheet = createSamplesheet(analysisAttempt);
          if (samplesheet.getInfo() == null) {
            // no info populated if samplesheet doesn't yet exist
            // however samplesheet doesn't drop immediately. return DTO still pending for another
            // go-around
            return dto;
          }

          // TODO does the new model account for this case?
          //          if (expectedWorkflows.isEmpty()) {
          //            dto.setAnalysisStatus(PipelineStatus.COMPLETED);
          //            return dto;
          //          }

          dragenPipelineRun = new DragenPipelineRun(samplesheet, attemptNum);

          if (expectedWorkflows.contains(DragenWorkflow.BCL_CONVERT)) {
            dragenPipelineRun.put(BCLConvert.process(samplesheet, analysisAttempt));
          }

          // Phase 2: more workflows go here

          // TODO (Phase 2): move manifest parsing here so maybe we can avoid looping over it

          dragenPipelineRun.tryComplete();
          if (dragenPipelineRun.getPipelineStatus().equals(PipelineStatus.COMPLETE)) {
            dto.addPipelineRun(dragenPipelineRun);
          }
        }
      }
    } else { // Analysis dir does not exist - we are not expecting DRAGEN for this run.
      dto.setAnalysisExpected(false);
    }

    return dto;
  }

  private static Samplesheet createSamplesheet(File rootDir) throws IOException {
    // Both copies DRAGEN makes of the SampleSheet are within BCLConvert
    // This is probably OK because we can't do any more analysis without it
    Samplesheet temp = new Samplesheet();
    File sampleSheet = new File(rootDir, "Data/BCLConvert/SampleSheet.csv");
    if (sampleSheet.exists()) {
      temp.setModifiedTime(Files.getLastModifiedTime(sampleSheet.toPath()).toInstant());
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
      for (String[] line : lines) { // TODO Header and Reads sections
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
              continue;
            }
            bclConvertSection = (SamplesheetBCLConvertSection) temp.getByName("BCLConvert");
            if (bclConvertSection == null) {
              bclConvertSection = new SamplesheetBCLConvertSection();
            }
            bclConvertSection.addDatum(
                line[laneIndex], line[sampleIndex], line[indexIndex], line[index2Index]);
            temp.addToSamplesheet(bclConvertSection);
            expectedWorkflows.add(DragenWorkflow.BCL_CONVERT);
            break;
          case "BCLConvert_Settings":
            bclConvertSection = (SamplesheetBCLConvertSection) temp.getByName("BCLConvert");
            if (bclConvertSection == null) {
              bclConvertSection = new SamplesheetBCLConvertSection();
            }
            bclConvertSection.addSetting(line[0], line[1]);
            temp.addToSamplesheet(bclConvertSection);
            expectedWorkflows.add(DragenWorkflow.BCL_CONVERT);
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
}
