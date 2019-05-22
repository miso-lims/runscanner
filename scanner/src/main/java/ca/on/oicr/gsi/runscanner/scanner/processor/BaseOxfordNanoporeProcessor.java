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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseOxfordNanoporeProcessor extends RunProcessor {
  private static final Logger log = LoggerFactory.getLogger(BaseOxfordNanoporeProcessor.class);

  protected static String trackingId;
  protected static String contextTags;
  protected final String SEQUENCER_NAME;
  protected static final int LANE_COUNT = 1;
  /**
   * Used for filtering out directories named after sequencer positions, a hallmark of the old
   * directory format which we need to ignore for performance.
   */
  protected static final Pattern POSITION = Pattern.compile("/[0-9]-[A-Z][0-9]+-[A-Z][0-9]+");

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
            @Override
            public FileVisitResult preVisitDirectory(
                Path path, BasicFileAttributes basicFileAttributes) throws IOException {
              // Immediately check if we're dealing with the old format, move on if so.
              if (POSITION.matcher(path.toString()).find()) {
                log.debug("Skipping " + path + " because we found a Sequencer Position in it.");
                return FileVisitResult.SKIP_SUBTREE;
              }
              if (readsDirectoryForRun(path).anyMatch(Files::isDirectory)) {
                log.debug("Adding " + path);
                runDirectories.add(path.toFile());
                return FileVisitResult.SKIP_SUBTREE;
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
                throws IOException {
              log.debug("Visiting " + path + " and moving on");
              return FileVisitResult.CONTINUE;
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

  protected abstract Stream<Path> readsDirectoryForRun(Path path);

  /**
   * Unlike the other process() implementations, this one is 'synchronized'. This is because JHDF5
   * is *NOT THREADSAFE* and will start using read names from other files if not controlled
   *
   * @param runDirectory the directory to scan (which will be output from {@link
   *     #getRunsFromRoot(File)}
   * @param tz the user-specified timezone that the sequencer exists in
   * @return
   * @throws IOException
   */
  public synchronized NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    final File firstFile =
        readsDirectoryForRun(runDirectory.toPath())
            .filter(Files::isDirectory)
            .flatMap(
                p -> {
                  try (Stream<Path> files = Files.list(p)) {
                    return files
                        .filter(BaseOxfordNanoporeProcessor::isFileFast5)
                        .map(Path::toFile)
                        .findFirst()
                        .map(Stream::of)
                        .orElseGet(Stream::empty);
                  } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    return Stream.empty();
                  }
                })
            .findFirst()
            .orElseThrow(
                () -> new IOException("Cannot find FAST5 file in run directory: " + runDirectory));

    log.debug("For runDirectory = " + runDirectory + " we will be considering file: " + firstFile);
    OxfordNanoporeNotificationDto onnd = new OxfordNanoporeNotificationDto();
    try (IHDF5Reader genericReader = HDF5FactoryProvider.get().openForReading(firstFile)) {

      // Get the name of a read so we can access the metadata. getAllGroupMembers() doesn't return
      // names in any
      // particular order so this is arbitrary.
      String read_name = genericReader.object().getAllGroupMembers("/").get(0);
      log.debug("Randomly selected read " + read_name + "from " + firstFile);

      Path p = runDirectory.toPath();
      // nameCount - 1 is the position of the name furthest from the root
      onnd.setRunAlias(p.getName(p.getNameCount() - 1).toString());

      onnd.setSequencerFolderPath(runDirectory.toString());
      onnd.setSequencerName(SEQUENCER_NAME);

      trackingId = read_name + "/tracking_id";
      contextTags = read_name + "/context_tags";

      onnd.setContainerSerialNumber(genericReader.string().getAttr(trackingId, "flow_cell_id"));

      onnd.setContainerModel(genericReader.string().getAttr(contextTags, "flowcell_type"));

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

  protected abstract void additionalProcess(OxfordNanoporeNotificationDto nnd, IHDF5Reader reader);
}
