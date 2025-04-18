package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This is a test run scanner for debugging an testing purposes.
 *
 * <p>It scans a directory containing folders with a file named <code>notification.json</code>
 * containing an appropriate subtype of {@link NotificationDto} and returns it. If the directory
 * name ends in "-Xs", then the scanner will wait X seconds pretending to process the run.
 */
public class Testing extends RunProcessor {
  private static final Pattern INTEGER_TAIL = Pattern.compile("-(\\d+)s$");

  public Testing(Builder builder) {
    super(builder);
  }

  /** Determine the right DTO class to parse as based on the platform type. */
  private Class<? extends NotificationDto> classForPlatform() {
    switch (getPlatformType()) {
      case ILLUMINA:
        return IlluminaNotificationDto.class;
      case PACBIO:
        return PacBioNotificationDto.class;
      case OXFORDNANOPORE:
        return OxfordNanoporeNotificationDto.class;
      default:
        return NotificationDto.class;
    }
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    return Arrays.stream(root.listFiles(File::isDirectory));
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    ObjectMapper mapper = createObjectMapper();

    Matcher m = INTEGER_TAIL.matcher(runDirectory.getName());
    if (m.matches()) {
      try {
        wait(Integer.parseInt(m.group(1)) * 1_000L);
      } catch (NumberFormatException | InterruptedException e) {
        // We really don't care if either of these happen.
      }
    }
    return mapper.readValue(new File(runDirectory, "notification.json"), classForPlatform());
  }

  /**
   * Get the type of path (File, Directory) this processor requires for runs.
   *
   * @return PathType File or Directory
   */
  @Override
  public PathType getPathType() {
    return PathType.DIRECTORY;
  }
}
