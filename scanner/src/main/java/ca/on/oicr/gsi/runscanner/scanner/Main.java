package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

/**
 * Attempts to process run directories, provided on the command line, through a particular processor
 * and display the results. This is for debugging purposes.
 */
public final class Main {

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println(
          "Usage: java -DplatformType=ILLUMINA -Dname=default -Dtz=America/Toronto -Dparameters={} ca.on.oicr.gsi.runscanner.scanner.Main /path/to/run/folder");
    }
    String platformName = System.getProperty("platformType");
    if (platformName == null) {
      System.err.println("Please set -DplatformType=X where X is one of:");
      Arrays.stream(Platform.values()).map(Platform::name).forEach(System.err::println);
      System.exit(1);
    }

    String tzId = System.getProperty("tz");
    TimeZone tz;
    if (tzId == null) {
      tz = TimeZone.getDefault();
    } else {
      tz = TimeZone.getTimeZone(tzId);
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule()).setDateFormat(new ISO8601DateFormat());

    Platform pt = Platform.valueOf(platformName);
    String name = System.getProperty("name", "default");
    RunProcessor rp =
        RunProcessor.processorFor(
                pt,
                name,
                mapper.readValue(System.getProperty("parameters", "{}"), ObjectNode.class))
            .orElseGet(
                () -> {
                  System.err.println("Cannot find a run processor that matches.");
                  System.exit(1);
                  return null;
                });
    List<NotificationDto> results = new ArrayList<>();
    boolean success = true;
    for (String path : args) {
      File directory = new File(path);
      if (!validPathForPlatform(pt, directory)) {
        System.err.println("Target is not of usable type: " + path);
        success = false;
        continue;
      }
      try {
        results.add(rp.process(directory, tz));
      } catch (Exception e) {
        System.err.println("Cannot process directory: " + path);
        e.printStackTrace();
        success = false;
      }
    }

    try {
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.writeValue(System.out, results);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(success ? 0 : 2);
  }

  /**
   * Check whether a given File represents a useable path for the given Platform.
   *
   * <p>This check is necessary because Oxford Nanopore output has many files per directory, so
   * individual files are passed as runs rather than directories. PacBio and Illumina output is
   * taken as a path to a directory.
   *
   * @param platform Platform type of the sequencer we are testing.
   * @param fs_obj File object representing the location of the sequencer output.
   * @return true if file/directory is accessible and of the correct type (directory for
   *     PacBio/Illumina, file for Oxford Nanopore)
   */
  private static boolean validPathForPlatform(Platform platform, File fs_obj) {
    switch (platform) {
      case PACBIO:
      case ILLUMINA:
        return fs_obj.isDirectory() && fs_obj.canExecute() && fs_obj.canRead();
      case PROMETHION:
      case MINION:
        return fs_obj.isFile() && fs_obj.canExecute() && fs_obj.canRead();
      default:
        return false;
    }
  }
}
