package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;

// Represents one CRAM file produced by a CRAM generation workflow.
public class CramAnalysisFile extends AnalysisFile {
  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.CRAM;
  }
}
