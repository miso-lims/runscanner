package ca.on.oicr.gsi.runscanner.scanner.processor;

import java.io.File;
import java.nio.file.Path;

public abstract class BaseNanoporeProcessor extends RunProcessor {
  protected final String TRACKING_ID = "UniqueGlobalKey/tracking_id";
  protected final String CONTEXT_TAGS = "UniqueGlobalKey/context_tags";
  protected final String SEQUENCER_NAME;

  protected static boolean isFileFast5(String fileName) {
    return fileName.endsWith(".fast5");
  }

  protected static boolean isFileFast5(File file) {
    return isFileFast5(file.getName());
  }

  protected static boolean isFileFast5(Path file) {
    return isFileFast5(file.getFileName().toString());
  }

  public BaseNanoporeProcessor(Builder builder, String seqName) {
    super(builder);
    SEQUENCER_NAME = seqName;
  }

  @Override
  public PathType getPathType() {
    return PathType.DIRECTORY;
  }
}
