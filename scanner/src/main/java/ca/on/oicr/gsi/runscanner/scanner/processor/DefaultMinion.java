package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import java.util.stream.Stream;

public class DefaultMinion extends RunProcessor {

  public DefaultMinion(Builder builder) {
    super(builder);
  }
  /**
   * Provide the directories containing runs given a user-specified configuration directory.
   *
   * <p>For most sequencer, the runs are the directories immediately under the sequencer's output
   * directory. In other platforms, they may be a subset of those directories or nested further
   * down. This method is to return the appropriate directories that are worth processing.
   *
   * @param root The directory as specified by the user.
   * @return a stream of directories to process
   */
  @Override
  public Stream<File> getRunsFromRoot(File root) {
    return null;
  }

  /**
   * Read a run directory and compute a result that can be sent to MISO.
   *
   * @param runDirectory the directory to scan (which will be output from {@link
   *     #getRunsFromRoot(File)}
   * @param tz the user-specified timezone that the sequencer exists in
   * @return the DTO result for consumption by MISO; if {@link Platform#isDone} is false, this
   *     directory may be processed again.
   */
  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    return null;
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new DefaultMinion(builder);
  }
}
