package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;

// Represents one index file
public class IndexAnalysisFile extends UltimaAnalysisFile {
  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.INDEX;
  }
}
