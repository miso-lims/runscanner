package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;

// Represents one CRAM file
public class CramAnalysisFile extends UltimaAnalysisFile {
  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.CRAM;
  }
}
