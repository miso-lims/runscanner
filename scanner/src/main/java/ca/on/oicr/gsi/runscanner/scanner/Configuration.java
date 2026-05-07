package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.scanner.processor.PathType;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

public class Configuration {

  private List<File> ignoreSubdirectories;
  private File path;
  private RunProcessor processor;
  private ObjectNode parameters;

  private TimeZone timeZone;

  public List<File> getIgnoreSubdirectories() {
    return ignoreSubdirectories;
  }

  public File getPath() {
    return path;
  }

  public RunProcessor getProcessor() {
    return processor;
  }

  public ObjectNode getParameters() {
    return parameters;
  }

  public Stream<Pair<File, Configuration>> getRuns() {
    return processor.getRunsFromRoot(getPath()).map(directory -> new Pair<>(directory, this));
  }

  public TimeZone getTimeZone() {
    return timeZone;
  }

  public boolean isValid() {

    // If the path is VIRTUAL, we only care that a path string and timezone are provided
    if (processor != null && processor.getPathType() == PathType.VIRTUAL) {
      return path != null && timeZone != null && processor.validateParameters(parameters);
    }

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
    if (timeZone == null) summary += "TimeZone is null! ";
    if (processor != null && processor.getPathType() == PathType.VIRTUAL) {
      if (!processor.validateParameters(parameters))
        summary += "Parameters not configured correctly! ";
    }
    return summary;
  }

  public void setIgnoreSubdirectories(List<File> ignoreSubdirectories) {
    if (ignoreSubdirectories == null) {
      this.ignoreSubdirectories = new ArrayList<>();
    } else {
      this.ignoreSubdirectories = ignoreSubdirectories;
    }
  }

  public void setPath(File path) {
    this.path = path;
  }

  public void setParameters(ObjectNode parameters) {
    this.parameters = parameters;
  }

  public void setProcessor(RunProcessor processor) {
    this.processor = processor;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }
}
