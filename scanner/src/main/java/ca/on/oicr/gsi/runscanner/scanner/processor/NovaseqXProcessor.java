package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisStatus;
import ca.on.oicr.gsi.runscanner.dto.type.DRAGENWorkflow;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.BCLConvert;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.DragenAnalysis;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.Samplesheet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaseqXProcessor extends DefaultIllumina {

  private final String NUMERAL = "\\d+";
  static ObjectMapper MAPPER = RunProcessor.createObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(NovaseqXProcessor.class);

  public NovaseqXProcessor(Builder builder, boolean checkOutput) {
    super(builder, checkOutput);
  }

  @Override
  public IlluminaNotificationDto analyse(
      File runDirectory, TimeZone tz, IlluminaNotificationDto dto) throws IOException {
    ObjectNode json = MAPPER.createObjectNode();
    dto.setAnalysisStatus(AnalysisStatus.PENDING);

    File analysisDir = new File(runDirectory, "Analysis");

    // For n in Analysis/n/Data (accommodate reruns, ish. if more reruns appear, they won't be
    // scanned. Someone will need to invalidate the run with the API.)
    if (analysisDir.exists() && analysisDir.isDirectory()) {
      // Null pointer should never actually happen because of above checks
      for (File analysisAttempt : Objects.requireNonNull(analysisDir.listFiles())) {
        if (analysisAttempt.isDirectory() && analysisAttempt.getName().matches(NUMERAL)) {
          String attemptNum = analysisAttempt.getName();
          Samplesheet samplesheet = new Samplesheet(MAPPER, analysisAttempt);
          if (samplesheet.getInfo() == null) { // no info populated if samplesheet doesn't yet exist
            dto.setAnalysisStatus(AnalysisStatus.PENDING);
            return dto;
          }

          if (samplesheet.noWorkflowsExpected()) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
            return dto;
          }

          DragenAnalysis dragenAnalysis = new DragenAnalysis(MAPPER, samplesheet);

          if (samplesheet.isWorkflowExpected(DRAGENWorkflow.BCL_CONVERT)) {
            BCLConvert bclConvert = new BCLConvert(samplesheet, analysisAttempt);
            dragenAnalysis.put("BCLConvert", bclConvert.getResult());

            // TODO: Should we not put() anything if not OK?
            if (bclConvert.isOk()) samplesheet.setWorkflowComplete(DRAGENWorkflow.BCL_CONVERT);
          }

          // Phase 2: more workflows go here

          // TODO (Phase 2): move manifest parsing here so maybe we can avoid looping over it

          if (samplesheet.allWorkflowsCompleted()) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
          }
          json.set(attemptNum, dragenAnalysis.toJson());
        }
      }
    } else { // Analysis dir does not exist - we are not expecting DRAGEN for this run.
      dto.setAnalysisStatus(AnalysisStatus.NONE);
    }
    dto.setAnalysis(json);
    return dto;
  }

  public static NovaseqXProcessor create(Builder builder, ObjectNode parameters) {
    return new NovaseqXProcessor(builder, calculateCheckOutput(parameters));
  }
}
