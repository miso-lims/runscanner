package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.stream.Stream;

public class Configuration {

  private File path;

  private RunProcessor processor;

  private TimeZone timeZone;

  private ArrayList<File> ignoreSubdirectories;

  public File getPath() {
    return path;
  }

  public RunProcessor getProcessor() {
    return processor;
  }

  public Stream<Pair<File, Configuration>> getRuns() {
    return processor.getRunsFromRoot(getPath()).map(directory -> new Pair<>(directory, this));
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public ArrayList<File> getIgnoreSubdirectories() {
    return ignoreSubdirectories;
  }

  public boolean isValid() {
    return path != null
        && path.isDirectory()
        && path.canRead()
        && path.canExecute()
        && processor != null
        && timeZone != null;
  }

  public String validitySummary() {
    String summary = "";
    if (path == null) {
      summary += "Path is null! ";
    } else {
      if (!path.isDirectory()) summary += "Path is not a directory! ";
      if (!path.canRead()) summary += "Path cannot be read! ";
      if (!path.canExecute()) summary += "Path cannot be executed! ";
    }
    if (processor == null) summary += "Processor is null! ";
    if (timeZone == null) summary += "TimeZone is null!";
    return summary;
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

  public void setIgnoreSubdirectories(ArrayList<File> ignoreSubdirectories) {
    if (ignoreSubdirectories == null) {
      this.ignoreSubdirectories = new ArrayList<>();
    } else {
      this.ignoreSubdirectories = ignoreSubdirectories;
    }
  }
}
