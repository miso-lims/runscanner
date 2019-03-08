package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NanoporeNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.IHDF5StringReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PromethionProcessor extends BaseNanoporeProcessor {
  private final int LANE_COUNT = 1;

  public PromethionProcessor(Builder builder, String seqName) {
    super(builder, seqName);
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

  private Stream<Path> readsDirectoryForRun(Path path) {
    return IntStream.of(0, 1).mapToObj(i -> path.resolve(Paths.get("reads", Integer.toString(i))));
  }

  @Override
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

    NanoporeNotificationDto pnd = new NanoporeNotificationDto();
    IHDF5StringReader reader = HDF5FactoryProvider.get().openForReading(firstFile).string();

    pnd.setRunAlias(reader.getAttr(TRACKING_ID, "run_id"));
    pnd.setSequencerFolderPath(runDirectory.toString());
    pnd.setSequencerName(SEQUENCER_NAME);
    pnd.setSequencerPosition(reader.getAttr(TRACKING_ID, "device_id"));
    pnd.setContainerSerialNumber(reader.getAttr(TRACKING_ID, "flow_cell_id"));
    pnd.setContainerModel(
        reader.getAttr(CONTEXT_TAGS, "flowcell_type")); // Might be something else!
    pnd.setLaneCount(LANE_COUNT);
    pnd.setHealthType(HealthType.UNKNOWN);
    pnd.setStartDate(
        ZonedDateTime.parse(reader.getAttr(TRACKING_ID, "exp_start_time")).toLocalDateTime());
    pnd.setSoftware(
        reader.getAttr(TRACKING_ID, "version")
            + " + "
            + reader.getAttr(TRACKING_ID, "protocols_version"));

    return pnd;
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new PromethionProcessor(builder, jsonNodes.get("name").asText());
  }
}
