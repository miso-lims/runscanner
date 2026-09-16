package ca.on.oicr.gsi.runscanner.dto.type;

/**
 * Enum representing the formats of file that a sequencing workflow can output.
 *
 * <p>Each constant's {@link #getFormat()} value must match the name under which the corresponding
 * subclass is registered in {@code AnalysisFile}'s {@code @JsonSubTypes}, since that is the
 * discriminator written when an AnalysisFile is serialized.
 */
public enum AnalysisFileFormat {
  FASTQ("fastq"), //
  CRAM("cram"), //
  METADATA("metadata");

  private final String format;

  AnalysisFileFormat(String format) {
    this.format = format;
  }

  /** The value used as the "format" property when an AnalysisFile of this type is serialized. */
  public String getFormat() {
    return format;
  }
}
