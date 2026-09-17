package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;

/**
 * Stand-in for an AnalysisFile whose "format" discriminator this version of the DTOs does not
 * recognise, either because it is absent or because it names a format added after this version was
 * built.
 *
 * <p>This is the {@code defaultImpl} of {@link AnalysisFile}. Without it, a single unrecognised
 * format aborts the deserialization of the entire enclosing notification, taking every well-formed
 * file with it; with it, the unrecognised file arrives as an UnknownAnalysisFile carrying its
 * common fields and its siblings deserialize normally. The discriminator that was actually read is
 * available from {@link AnalysisFile#getRawFormat()}.
 */
public class UnknownAnalysisFile extends AnalysisFile {
  @Override
  public AnalysisFileFormat getFormatType() {
    return AnalysisFileFormat.UNKNOWN;
  }
}
