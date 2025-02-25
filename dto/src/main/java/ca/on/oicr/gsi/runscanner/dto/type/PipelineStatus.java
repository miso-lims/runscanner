package ca.on.oicr.gsi.runscanner.dto.type;

public enum PipelineStatus {
  /** Not all files found yet; queue for recheck */
  INCOMPLETE,

  /** All files found. No recheck */
  COMPLETE,

  /**
   * The only workflows queued by the samplesheet are workflows runscanner does not yet parse, so we
   * will not try and will not recheck
   */
  UNSUPPORTED,

  /** The samplesheet is present but has something wrong with it. Developer attention needed. */
  SCAN_ERROR
}
