package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.IHDF5StringReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.stream.Stream;

public abstract class BaseOxfordNanoporeProcessor extends RunProcessor {
  protected static final String TRACKING_ID = "UniqueGlobalKey/tracking_id";
  protected static final String CONTEXT_TAGS = "UniqueGlobalKey/context_tags";
  protected final String SEQUENCER_NAME;
  protected static final int LANE_COUNT = 1;

    private static final Logger log = LoggerFactory.getLogger(BaseOxfordNanoporeProcessor.class);

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
              if (readsDirectoryForRun(path).map(p -> p.toFile()).anyMatch(File::isDirectory)) {
                runDirectories.add(path.toFile());
                return FileVisitResult.SKIP_SUBTREE;
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes)
                throws IOException {
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
              e.printStackTrace();
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      log.error("Failed getting runs from: " + root, e);
    }
    return runDirectories.stream();
  }

  protected abstract Stream<Path> readsDirectoryForRun(Path path);

  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    final File firstFile =
        readsDirectoryForRun(runDirectory.toPath())
                .map(p -> p.toFile())
            .filter(File::isDirectory)
                .map(f -> f.toPath())
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
                    log.error("Failed to process run at: " + runDirectory.getAbsolutePath(), e);
                    return Stream.empty();
                  }
                })
            .findFirst()
            .orElseThrow(
                () -> new IOException("Cannot find FAST5 file in run directory: " + runDirectory));

    OxfordNanoporeNotificationDto onnd = new OxfordNanoporeNotificationDto();
    IHDF5StringReader reader = HDF5FactoryProvider.get().openForReading(firstFile).string();
    onnd.setRunAlias(reader.getAttr(TRACKING_ID, "run_id"));
    onnd.setSequencerFolderPath(runDirectory.toString());
    onnd.setSequencerName(SEQUENCER_NAME);
    onnd.setContainerSerialNumber(reader.getAttr(TRACKING_ID, "flow_cell_id"));
    onnd.setContainerModel(reader.getAttr(CONTEXT_TAGS, "flowcell_type").toUpperCase());
    onnd.setLaneCount(LANE_COUNT);
    onnd.setHealthType(HealthType.UNKNOWN);
    onnd.setStartDate(
        ZonedDateTime.parse(reader.getAttr(TRACKING_ID, "exp_start_time")).toLocalDateTime());
    onnd.setSoftware(
        reader.getAttr(TRACKING_ID, "version")
            + " + "
            + reader.getAttr(TRACKING_ID, "protocols_version"));

    additionalProcess(onnd, reader);
    return onnd;
  }

  protected abstract void additionalProcess(
      OxfordNanoporeNotificationDto nnd, IHDF5StringReader reader);
}
