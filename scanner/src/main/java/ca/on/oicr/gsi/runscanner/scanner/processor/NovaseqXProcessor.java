package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaDragenNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
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
  static ObjectMapper MAPPER;
  private static final Logger log = LoggerFactory.getLogger(NovaseqXProcessor.class);

  public NovaseqXProcessor(Builder builder, boolean checkOutput) {
    super(builder, checkOutput);
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    MAPPER = new ObjectMapper().registerModule(super.setUpCustomModule(tz));
    ObjectNode json = MAPPER.createObjectNode();
    IlluminaDragenNotificationDto dto = new IlluminaDragenNotificationDto();
    dto.clone((IlluminaNotificationDto) super.process(runDirectory, tz));
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

          if (samplesheet.noWorkflowsExpected()) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
            return dto;
          }

          DragenAnalysis dragenAnalysis = new DragenAnalysis(MAPPER, samplesheet);

          if (samplesheet.isWorkflowExpected(DRAGENWorkflow.BCL_CONVERT)) {
            dragenAnalysis.put("BCLConvert", BCLConvert.process(analysisAttempt));

            // TODO there's more to this than that
            samplesheet.setWorkflowComplete(DRAGENWorkflow.BCL_CONVERT);
          }

          // Phase 2: more workflows go here

          if (samplesheet.allWorkflowsCompleted()) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
          }
          json.set(attemptNum, dragenAnalysis.toJson());
        }
      }
    } else { // TODO: Analysis dir does not exist. Does it spawn immediately or later?
      dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
    }
    dto.setAnalysis(json);
    return dto;
  }

  public static NovaseqXProcessor create(Builder builder, ObjectNode parameters) {
    return new NovaseqXProcessor(builder, calculateCheckOutput(parameters));
  }
}
