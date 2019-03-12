package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NanoporeNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.IHDF5StringReader;
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

public abstract class BaseNanoporeProcessor extends RunProcessor {
  protected static final String TRACKING_ID = "UniqueGlobalKey/tracking_id";
  protected static final String CONTEXT_TAGS = "UniqueGlobalKey/context_tags";
  protected final String SEQUENCER_NAME;
  protected static final int LANE_COUNT = 1;

  protected static boolean isFileFast5(String fileName) {
    return fileName.endsWith(".fast5");
  }

  protected static boolean isFileFast5(File file) {
    return isFileFast5(file.getName());
  }

  protected static boolean isFileFast5(Path file) {
    return isFileFast5(file.getFileName().toString());
  }

  public BaseNanoporeProcessor(Builder builder, String seqName) {
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
              if (readsDirectoryForRun(path).anyMatch(Files::isDirectory)) {
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
      e.printStackTrace();
    }
    return runDirectories.stream();
  }

  protected abstract Stream<Path> readsDirectoryForRun(Path path);

  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    final File firstFile =
        readsDirectoryForRun(runDirectory.toPath())
            .filter(Files::isDirectory)
            .flatMap(
                p -> {
                  try (Stream<Path> files = Files.list(p)) {
                    return files
                        .filter(BaseNanoporeProcessor::isFileFast5)
                        .map(Path::toFile)
                        .findFirst()
                        .map(Stream::of)
                        .orElseGet(Stream::empty);
                  } catch (IOException e) {
                    e.printStackTrace();
                    return Stream.empty();
                  }
                })
            .findFirst()
            .orElseThrow(
                () -> new IOException("Cannot find FAST5 file in run directory: " + runDirectory));

    NanoporeNotificationDto nnd = new NanoporeNotificationDto();
    IHDF5StringReader reader = HDF5FactoryProvider.get().openForReading(firstFile).string();
    nnd.setRunAlias(reader.getAttr(TRACKING_ID, "run_id"));
    nnd.setSequencerFolderPath(runDirectory.toString());
    nnd.setSequencerName(SEQUENCER_NAME);
    nnd.setContainerSerialNumber(reader.getAttr(TRACKING_ID, "flow_cell_id"));
    nnd.setContainerModel(reader.getAttr(CONTEXT_TAGS, "flowcell_type"));
    nnd.setLaneCount(LANE_COUNT);
    nnd.setHealthType(HealthType.UNKNOWN);
    nnd.setStartDate(
        ZonedDateTime.parse(reader.getAttr(TRACKING_ID, "exp_start_time")).toLocalDateTime());
    nnd.setSoftware(
        reader.getAttr(TRACKING_ID, "version")
            + " + "
            + reader.getAttr(TRACKING_ID, "protocols_version"));

    additionalProcess(nnd, reader);
    return nnd;
  }

  protected abstract void additionalProcess(NanoporeNotificationDto nnd, IHDF5StringReader reader);
}
