package ca.on.oicr.gsi.runscanner.oxfordnanopore;

import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.processor.RunProcessor;
import ch.systemsx.cisd.hdf5.IHDF5StringReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PromethionProcessor extends BaseOxfordNanoporeProcessor {

  public static Builder provider() {
    return new Builder(Platform.OXFORDNANOPORE, "promethion", PromethionProcessor::create);
  }

  public PromethionProcessor(Builder builder, String seqName) {
    super(builder, seqName);
  }

  @Override
  protected Stream<Path> readsDirectoryForRun(Path path) {
    return IntStream.of(0, 1).mapToObj(i -> path.resolve(Paths.get("reads", Integer.toString(i))));
  }

  @Override
  protected void additionalProcess(OxfordNanoporeNotificationDto onnd, IHDF5StringReader reader) {
    onnd.setSequencerPosition(reader.getAttr(TRACKING_ID, "device_id"));
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new PromethionProcessor(builder, jsonNodes.get("name").asText());
  }
}
