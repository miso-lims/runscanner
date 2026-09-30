package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;

// Represents one CRAM file
public class CramAnalysisFile extends AnalysisFile {
  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.CRAM;
  }
}
