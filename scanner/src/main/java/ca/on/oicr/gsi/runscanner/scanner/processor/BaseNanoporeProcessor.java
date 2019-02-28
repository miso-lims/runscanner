package ca.on.oicr.gsi.runscanner.scanner.processor;

import java.io.File;

public abstract class BaseNanoporeProcessor extends RunProcessor {
  protected final String TRACKING_ID = "UniqueGlobalKey/tracking_id";
  protected final String CONTEXT_TAGS = "UniqueGlobalKey/context_tags";
  protected final String SEQUENCER_NAME;

  public BaseNanoporeProcessor(Builder builder, String seqName) {
    super(builder);
    SEQUENCER_NAME = seqName;
  }

  protected boolean isFileFast5(File file) {
    return file.getName().endsWith(".fast5");
  }

  @Override
  public boolean isFilePathValid(File filesystemObject) {
    return filesystemObject.isFile() && filesystemObject.canExecute() && filesystemObject.canRead();
  }
}
