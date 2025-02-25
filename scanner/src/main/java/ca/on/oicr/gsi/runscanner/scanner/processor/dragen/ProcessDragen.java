package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.WorkflowRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.type.DragenWorkflow;
import ca.on.oicr.gsi.runscanner.dto.type.PipelineStatus;
import ca.on.oicr.gsi.runscanner.dto.type.WorkflowRunStatus;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.Samplesheet;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.Samplesheet.SamplesheetException;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.SamplesheetBCLConvertSection;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.Semver;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDragen {

  private static final String NUMERAL = "\\d+";
  private static final Logger log = LoggerFactory.getLogger(ProcessDragen.class);

  public static IlluminaNotificationDto analyse(File runDirectory, IlluminaNotificationDto dto)
      throws IOException {
    DragenPipelineRun dragenPipelineRun = null;

    File analysisDir = new File(runDirectory, "Analysis");

    // For n in Analysis/n/Data (accommodate reruns, ish. if more reruns appear, they won't be
    // scanned. Someone will need to invalidate the run with the API.)
    if (analysisDir.exists() && analysisDir.isDirectory()) {
      dto.setAnalysisExpected(true);
      // Null pointer should never actually happen because of above checks
      for (File analysisAttempt : Objects.requireNonNull(analysisDir.listFiles())) {
        if (analysisAttempt.isDirectory() && analysisAttempt.getName().matches(NUMERAL)) {
          int attemptNum = Integer.parseInt(analysisAttempt.getName());
          dragenPipelineRun = new DragenPipelineRun(attemptNum);
          Samplesheet samplesheet;
          try {
            samplesheet = new Samplesheet(analysisAttempt);
          } catch (SamplesheetException ise) {
            log.error("SCAN_ERROR in Samplesheet.csv for {}", analysisAttempt);
            dragenPipelineRun.setPipelineStatus(PipelineStatus.SCAN_ERROR);
            dto.addPipelineRun(dragenPipelineRun);
            return dto;
          }
          if (samplesheet.getInfo() == null) {
            // no info populated if samplesheet doesn't yet exist
            // however samplesheet doesn't drop immediately. return DTO still pending for another
            // go-around
            return dto;
          }

          // Exit early if we didn't find any workflows we support
          if (samplesheet.noneExpected()) {
            dragenPipelineRun.setPipelineStatus(PipelineStatus.UNSUPPORTED);
            dto.addPipelineRun(dragenPipelineRun);
            continue;
          }

          if (samplesheet.isExpected(DragenWorkflow.BCL_CONVERT)) {
            // Exit early if Samplesheet finds a SoftwareVersion < 4.1.7, our minimum supported
            // version
            SamplesheetBCLConvertSection bclConvertSection =
                (SamplesheetBCLConvertSection) samplesheet.getByName("BCLConvert");
            if (bclConvertSection.getSoftwareVersion().compareTo(new Semver(4, 1, 7)) < 0) {
              dragenPipelineRun.setPipelineStatus(PipelineStatus.UNSUPPORTED);
              dto.addPipelineRun(dragenPipelineRun);
              continue;
            }

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
}
