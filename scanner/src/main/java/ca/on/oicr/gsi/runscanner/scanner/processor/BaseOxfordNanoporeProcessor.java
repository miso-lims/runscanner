package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseOxfordNanoporeProcessor extends RunProcessor {
  /** Used for error and debug logging */
  private final Logger log = LoggerFactory.getLogger(BaseOxfordNanoporeProcessor.class);

  /** Used for reporting non-fast5 files encountered while looking for fast5s */
  private final Logger mysteryFiles = LoggerFactory.getLogger("mysteryLogger");

  protected static String trackingId;
  protected static String contextTags;
  protected final String SEQUENCER_NAME;
  protected static final int LANE_COUNT = 1;

  /** Skip processing anything older than 2017 since they typically fail */
  protected static final FileTime CUTOFF_DATE =
      FileTime.from(Instant.parse("2017-01-01T00:00:00.00Z"));

  protected static boolean isFileFast5(String fileName) {
    return fileName.endsWith(".fast5");
  }

  protected static boolean isFileFast5(File file) {
    return isFileFast5(file.getName());
  }

  protected static boolean isFileFast5(Path file) {
    return isFileFast5(file.getFileName().toString());
  }

  public BaseOxfordNanoporeProcessor(Builder builder, String seqName) {
    super(builder);
    SEQUENCER_NAME = seqName;
  }

  @Override
  public PathType getPathType() {
    return PathType.DIRECTORY;
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    final List<File> runDirectories = new ArrayList<>();
    try {
      Files.walkFileTree(
          root.toPath(),
          new FileVisitor<Path>() {

            /**
             * Actions to take before visiting each individual file within the directory, mostly
             * dealing with directory path.
             */
            @Override
            public FileVisitResult preVisitDirectory(
                Path path, BasicFileAttributes basicFileAttributes) throws IOException {
              log.debug("Pre-visit: " + path);

              /**
               * If directory matches criteria we know exclude the directory from consideration,
               * don't go into the directory (SKIP_SUBTREE)
               */
              if (excludedDirectoryFormat(path) || olderThanCutoff(path)) {
                log.debug("Skipping " + path + " because we found an excluded directory in it.");
                return FileVisitResult.SKIP_SUBTREE;
              }

              /**
               * If directory matches criteria we know include the directory, add without looking
               * inside directory (add, SKIP_SUBTREE). Checking if result of readsDirectoryForRun
               * 'isDirectory' is an existence check.
               */
              if (readsDirectoryForRun(path).anyMatch(Files::isDirectory)) {
                log.debug("Adding " + path);
                runDirectories.add(path.toFile());
                return FileVisitResult.SKIP_SUBTREE;
              }

              // If directory has directories or fast5s in, keep going through directory (CONTINUE)
              if (Arrays.asList(path.toFile().listFiles())
                      .stream()
                      .filter(f -> f.isDirectory() || isFileFast5(f))
                      .count()
                  > 0) {
                return FileVisitResult.CONTINUE;
              }

              // There's nothing good in this directory, so don't go within (SKIP_SUBTREE)
              return FileVisitResult.SKIP_SUBTREE;
            }

            /** Actions to take on file(s) within directory. */
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
              // If file is a fast5 file, add directory path to runDirectories
              if (isFileFast5(path)) {
                runDirectories.add(path.getParent().toFile());
                return FileVisitResult.SKIP_SIBLINGS;
              } else {
                /**
                 * If file isn't fast5, log because it probably shouldn't be in sequencer output,
                 * but keep going (CONTINUE) because we know from preVisitDirectory that there's
                 * something worth looking at in here
                 */
                mysteryFiles.debug(path.toString());
                return FileVisitResult.CONTINUE;
              }
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
              log.error("Failed to visit " + path);
              log.error(e.getMessage());
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
              log.debug("Done visiting " + path);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return runDirectories.stream();
  }

  /**
   * Returns true if provided Path points to a directory in a format we are skipping for
   * compatibility reasons.
   *
   * @param path Path to file
   * @return true if path matches known-bad directory name format
   */
  protected abstract boolean excludedDirectoryFormat(Path path);

  protected abstract Stream<Path> readsDirectoryForRun(Path path);

  /**
   * Unlike the other process() implementations, this one is 'synchronized'. This is because JHDF5
   * is *NOT THREADSAFE* and will start using read names from other files if not controlled
   *
   * @param runDirectory the directory to scan (which will be output from {@link
   *     #getRunsFromRoot(File)}
   * @param tz the user-specified timezone that the sequencer exists in
   * @return NotificationDto containing run information
   * @throws IOException
   */
  public synchronized NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    final File firstFile =
        readsDirectoryForRun(runDirectory.toPath())
            .filter(Files::isDirectory)
            .flatMap(
                p -> {
                  // Using walk() rather than list() prevents overlooking the case where /0 is empty
                  // but /37 has fast5s
                  try (Stream<Path> files = Files.walk(p)) {
                    return files
                        .filter(BaseOxfordNanoporeProcessor::isFileFast5)
                        .findFirst()
                        .map(Path::toFile)
                        .map(Stream::of)
                        .orElseGet(Stream::empty);
                  } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    return Stream.empty();
                  }
                })
            .findFirst()
            .orElseThrow(
                // This can be thrown in cases of extremely large numbers of fast5s. This is OK
                () -> new IOException("Cannot find FAST5 file in run directory: " + runDirectory));

    log.debug("For runDirectory = " + runDirectory + " we will be considering file: " + firstFile);
    OxfordNanoporeNotificationDto onnd = new OxfordNanoporeNotificationDto();
    try (IHDF5Reader genericReader = HDF5FactoryProvider.get().openForReading(firstFile)) {

      // Unless we have UniqueGlobalKey,
      // Get the name of a read so we can access the metadata. getAllGroupMembers() doesn't return
      // names in any
      // particular order so this is arbitrary.
      String read_name =
          genericReader.object().exists("/UniqueGlobalKey")
              ? "UniqueGlobalKey"
              : genericReader.object().getAllGroupMembers("/").get(0);

      log.debug("Selected read name " + read_name + " from " + firstFile);

      Path p = runDirectory.toPath();
      // nameCount - 1 is the position of the name furthest from the root
      onnd.setRunAlias(p.getName(p.getNameCount() - 1).toString());

      onnd.setSequencerFolderPath(runDirectory.toString());
      onnd.setSequencerName(SEQUENCER_NAME);

      trackingId = read_name + "/tracking_id";
      contextTags = read_name + "/context_tags";

      onnd.setContainerSerialNumber(genericReader.string().getAttr(trackingId, "flow_cell_id"));

      onnd.setLaneCount(LANE_COUNT);
      onnd.setHealthType(HealthType.UNKNOWN);

      onnd.setStartDate(
          ZonedDateTime.parse(genericReader.string().getAttr(trackingId, "exp_start_time"))
              .toLocalDateTime());

      onnd.setSoftware(genericReader.string().getAttr(trackingId, "version"));
      onnd.setProtocolVersion(genericReader.string().getAttr(trackingId, "protocols_version"));

      onnd.setRunType(genericReader.string().getAttr(trackingId, "exp_script_purpose"));

      additionalProcess(onnd, genericReader);
      return onnd;
    }
  }

  /**
   * Tests whether file at path is older than Jan 1 2017.
   *
   * @param path path leading to file to test
   * @return true if file is older than Jan 1 2017 OR IOException occurs, false otherwise
   */
  protected boolean olderThanCutoff(Path path) {
    try {
      return Files.readAttributes(path, BasicFileAttributes.class)
              .creationTime()
              .compareTo(CUTOFF_DATE)
          <= 0;
    } catch (IOException e) {
      return true;
    }
  }

  protected abstract void additionalProcess(OxfordNanoporeNotificationDto nnd, IHDF5Reader reader);
}
