package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NanoporeNotificationDto;
import ch.systemsx.cisd.hdf5.IHDF5StringReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PromethionProcessor extends BaseNanoporeProcessor {

  public PromethionProcessor(Builder builder, String seqName) {
    super(builder, seqName);
  }

  @Override
  protected Stream<Path> readsDirectoryForRun(Path path) {
    return IntStream.of(0, 1).mapToObj(i -> path.resolve(Paths.get("reads", Integer.toString(i))));
  }

  @Override
  protected void additionalProcess(NanoporeNotificationDto nnd, IHDF5StringReader reader) {
    nnd.setSequencerPosition(reader.getAttr(TRACKING_ID, "device_id"));
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new PromethionProcessor(builder, jsonNodes.get("name").asText());
  }
}
