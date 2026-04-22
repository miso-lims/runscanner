package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

public class Configuration {

  private List<File> ignoreSubdirectories;
  private String nexusApiAddress;
  private File nexusApiTokenFile;
  private File path;
  private RunProcessor processor;
  private String sampleDBApiAddress;
  private File sampleDBApiTokenFile;

  private TimeZone timeZone;

  public List<File> getIgnoreSubdirectories() {
    return ignoreSubdirectories;
  }

  public String getNexusApiAddress() {
    return nexusApiAddress;
  }

  public File getNexusApiTokenFile() {
    return nexusApiTokenFile;
  }

  public File getPath() {
    return path;
  }

  public RunProcessor getProcessor() {
    return processor;
  }

  public Stream<Pair<File, Configuration>> getRuns() {
    return processor.getRunsFromRoot(getPath()).map(directory -> new Pair<>(directory, this));
  }

  public String getSampleDBApiAddress() {
    return sampleDBApiAddress;
  }

  public File getSampleDBApiTokenFile() {
    return sampleDBApiTokenFile;
  }

  public TimeZone getTimeZone() {
    return timeZone;
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

  public void setIgnoreSubdirectories(List<File> ignoreSubdirectories) {
    if (ignoreSubdirectories == null) {
      this.ignoreSubdirectories = new ArrayList<>();
    } else {
      this.ignoreSubdirectories = ignoreSubdirectories;
    }
  }

  public void setNexusApiAddress(String nexusApiAddress) {
    this.nexusApiAddress = nexusApiAddress;
  }

  public void setNexusApiTokenFile(File nexusApiTokenFile) {
    this.nexusApiTokenFile = nexusApiTokenFile;
  }

  public void setPath(File path) {
    this.path = path;
  }

  public void setProcessor(RunProcessor processor) {
    this.processor = processor;
  }

  public void setSampleDBApiAddress(String sampleDBApiAddress) {
    this.sampleDBApiAddress = nexusApiAddress;
  }

  public void setSampleDBApiTokenFile(File sampleDBApiTokenFile) {
    this.sampleDBApiTokenFile = sampleDBApiTokenFile;
  }

  public void setTimeZone(TimeZone timeZone) {
    this.timeZone = timeZone;
  }
}
