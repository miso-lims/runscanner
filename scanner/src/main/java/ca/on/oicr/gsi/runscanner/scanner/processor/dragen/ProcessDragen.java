package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.WorkflowRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection.SamplesheetBCLConvertDataEntry;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetReadsSection;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Semver;
import ca.on.oicr.gsi.runscanner.dto.type.DragenWorkflow;
import ca.on.oicr.gsi.runscanner.dto.type.PipelineStatus;
import ca.on.oicr.gsi.runscanner.dto.type.WorkflowRunStatus;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
          dragenPipelineRun = new DragenPipelineRun(attemptNum);

          // Exit early if we didn't find any workflows we support
          if (expectedWorkflows.isEmpty()) {
            dragenPipelineRun.setPipelineStatus(PipelineStatus.UNSUPPORTED);
            dto.addPipelineRun(dragenPipelineRun);
            continue;
          }

          // Exit early if Samplesheet finds a SoftwareVersion < 4.1.7, our minimum supported
          // version
          SamplesheetBCLConvertSection bclConvertSection =
              (SamplesheetBCLConvertSection) samplesheet.getByName("BCLConvert");
          if (bclConvertSection.getSoftwareVersion().compareTo(new Semver(4, 1, 7)) < 0) {
            dragenPipelineRun.setPipelineStatus(PipelineStatus.UNSUPPORTED);
            dto.addPipelineRun(dragenPipelineRun);
            continue;
          }

          if (expectedWorkflows.contains(DragenWorkflow.BCL_CONVERT)) {
            dragenPipelineRun.put(BCLConvert.process(samplesheet, analysisAttempt));
          }

          // Phase 2: more workflows go here

          // TODO (Phase 2): move manifest parsing here so maybe we can avoid looping over it

          // WorkflowRuns that appear to be still pending when Secondary_Analysis_Complete
          // is created are actually failed.
          // Truly pending WorkflowRuns keep a PipelineRun from being complete, but
          // failed jobs do not - downstream consumers must check WorkflowRunStatus
          File secondaryAnalysisComplete =
              new File(analysisAttempt, "Data/Secondary_Analysis_Complete.txt");
          boolean secondaryAnalysisCompletePresent =
              (secondaryAnalysisComplete.exists() && secondaryAnalysisComplete.isFile());
          boolean allComplete = true;

          for (WorkflowRun wr : dragenPipelineRun.getWorkflowRuns()) {
            if (wr.getWorkflowRunStatus() == WorkflowRunStatus.PENDING) {
              if (secondaryAnalysisCompletePresent) {
                wr.fail();
              } else {
                allComplete = false;
              }
            }
          }
          if (allComplete) {
            dragenPipelineRun.setPipelineStatus(PipelineStatus.COMPLETE);
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

      SamplesheetBCLConvertSection bclConvertSection = null;
      SamplesheetReadsSection readsSection = null;
      Matcher headerMatcher;
      String sectionName = "";
      boolean bclDataFirstLine = true;
      Map<String, Integer> lineIndices = new HashMap<>();
      for (String[] line : lines) {
        headerMatcher = HEADER.matcher(line[0]);
        if (headerMatcher.matches()) {
          // "Capturing groups are indexed from left to right, starting at one.
          // Group zero denotes the entire pattern"
          sectionName = headerMatcher.group(1);
          continue;
        }
        switch (sectionName) {
          case "Reads":
            readsSection = (SamplesheetReadsSection) temp.getByName("Reads");
            if (readsSection == null) {
              readsSection = new SamplesheetReadsSection();
            }
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
                // TODO a couple samplesheets only have Lane and Sample_ID, this needs support
                switch (line[i]) {
                  case "Lane":
                    lineIndices.put("lane", i);
                    break;
                  case "Sample_ID":
                    lineIndices.put("sample_id", i);
                    break;
                  case "Index":
                    lineIndices.put("index", i);
                    break;
                  case "Index2":
                    lineIndices.put("index2", i);
                    break;
                  case "OverrideCycles":
                    lineIndices.put("overrideCycles", i);
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

            if (lineIndices.get("overrideCycles") == null) {
              bclConvertSection.addDatum(
                  line[lineIndices.get("lane")],
                  line[lineIndices.get("sample_id")],
                  line[lineIndices.get("index")],
                  line[lineIndices.get("index2")],
                  null);
            } else {
              bclConvertSection.addDatum(
                  line[lineIndices.get("lane")],
                  line[lineIndices.get("sample_id")],
                  line[lineIndices.get("index")],
                  line[lineIndices.get("index2")],
                  line[lineIndices.get("overrideCycles")]);
            }
            temp.addToSamplesheet(bclConvertSection);
            expectedWorkflows.add(DragenWorkflow.BCL_CONVERT);
            break;
          case "BCLConvert_Settings":
            bclConvertSection = (SamplesheetBCLConvertSection) temp.getByName("BCLConvert");
            if (bclConvertSection == null) {
              bclConvertSection = new SamplesheetBCLConvertSection();
            }
            switch (line[0]) {
              case "SoftwareVersion":
                bclConvertSection.setSoftwareVersion(line[1]);
                break;
              case "OverrideCycles":
                bclConvertSection.setOverrideCyclesSetting(line[1]);
                break;
            }
            temp.addToSamplesheet(bclConvertSection);
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
      // TODO BCLConvert.process() also needs this logic, see if we can refactor
      if (bclConvertSection != null && !lineIndices.containsKey("overrideCycles")) {
        if (bclConvertSection.getOverrideCyclesSetting() != null) {
          for (SamplesheetBCLConvertDataEntry entry : bclConvertSection.getData()) {
            entry.setOverrideCycles(bclConvertSection.getOverrideCyclesSetting());
          }
        } else {
          try {
            String defaultOverrideCycles =
                new StringBuilder("Y")
                    .append(readsSection.getRead1Cycles())
                    .append("I")
                    .append(readsSection.getIndex1Cycles())
                    .append("I")
                    .append(readsSection.getIndex2Cycles())
                    .append("Y")
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
    } else {
      // Samplesheet appears several hours after Analysis directory does
      log.info("No samplesheet for {}, will look again later", rootDir);
    }
    return temp;
  }
}
