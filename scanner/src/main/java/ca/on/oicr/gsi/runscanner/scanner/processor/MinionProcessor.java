package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ch.systemsx.cisd.hdf5.IHDF5StringReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class MinionProcessor extends BaseOxfordNanoporeProcessor {
  public MinionProcessor(Builder builder, String seqName) {
    super(builder, seqName);
  }

  @Override
  protected Stream<Path> readsDirectoryForRun(Path path) {
    return Stream.of(path.resolve(Paths.get("fast5", "0")));
  }

  @Override
  protected void additionalProcess(OxfordNanoporeNotificationDto onnd, IHDF5StringReader reader) {
    return;
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new MinionProcessor(builder, jsonNodes.get("name").asText());
  }
}
