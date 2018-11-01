package ca.on.oicr.gsi.runscanner.scanner;

import java.io.File;
import java.util.TimeZone;
import java.util.stream.Stream;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;

public class Configuration {

  private File path;

  private RunProcessor processor;

  private TimeZone timeZone;

  public File getPath() {
    return path;
  }

  public RunProcessor getProcessor() {
    return processor;
  }

  public Stream<? extends Pair<File, Configuration>> getRuns() {
    return processor.getRunsFromRoot(getPath()).map(directory -> new Pair<>(directory, this));
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public boolean isValid() {
    return path != null && path.isDirectory() && path.canRead() && path.canExecute() && processor != null && timeZone != null;
  }

  public void setPath(File path) {
    this.path = path;
  }

  public void setProcessor(RunProcessor processor) {
    this.processor = processor;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }
}
