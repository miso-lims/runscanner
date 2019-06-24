package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Attempts to process run directories, provided on the command line, through getRunsFromRoot() and
 * display the results. This is for debugging purposes.
 *
 * <p>Suppress warnings: "squid:S4823" warns whenever command line arguments are used. "squid:S106"
 * warns whenever System.out or System.err is used (requests a logging engine is used instead).
 * "squid:S1148" warns whenever Throwable.printStackTrace() is called.
 */
@SuppressWarnings({"squid:S4823", "squid:S106", "squid:S1148"})
public final class FindRuns {

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println(
          "Usage: java -DplatformType=ILLUMINA -Dname=default -Dtz=America/Toronto -Dparameters={} ca.on.oicr.gsi.runscanner.scanner.FindRuns /path/to/run/folder");
    }
    String platformName = System.getProperty("platformType");
    if (platformName == null) {
      System.err.println("Please set -DplatformType=X where X is one of:");
      Arrays.stream(Platform.values()).map(Platform::name).forEach(System.err::println);
      System.exit(1);
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

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
    List<File> results = new ArrayList<>();
    boolean success = true;
    for (String path : args) {
      File directory = new File(path);
      if (!(directory.isDirectory() && directory.canExecute() && directory.canRead())) {
        System.err.println("Target is not of usable type: " + path);
        System.err.println("!isDirectory: " + !directory.isDirectory());
        System.err.println("canExecute: " + directory.canExecute());
        System.err.println("canRead: " + directory.canRead());
        success = false;
        continue;
      }
      try {
        rp.getRunsFromRoot(directory).forEach(results::add);
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
}
