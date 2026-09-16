package ca.on.oicr.gsi.runscanner.dto.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;

// Represents a metadata file (e.g. csv, index, JSON, log)
public class MetadataAnalysisFile extends AnalysisFile {
  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.METADATA;
  }
}
