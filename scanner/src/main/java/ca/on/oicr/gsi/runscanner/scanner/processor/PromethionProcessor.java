package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.stream.Stream;

public class PromethionProcessor extends BaseOxfordNanoporeProcessor {

  public PromethionProcessor(Builder builder, String seqName) {
    super(builder, seqName);
  }

  @Override
  protected Stream<Path> readsDirectoryForRun(Path path) {
    return Stream.of(
        path.resolve("fast5_pass"),
        path.resolve("fastq_pass"),
        path.resolve("fast5_fail"),
        path.resolve("fastq_fail"),
        path.resolve("fast5_skip"),
        path.resolve("fast5"),
        path.resolve("reads"));
  }

  @Override
  protected boolean excludedDirectoryFormat(Path path) {
    return false;
  }

  @Override
  protected void additionalProcess(OxfordNanoporeNotificationDto onnd, IHDF5Reader reader) {
    onnd.setSequencerPosition(reader.string().getAttr(trackingId, "device_id"));
    onnd.setContainerModel(reader.string().getAttr(contextTags, "flowcell_type"));
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new PromethionProcessor(builder, jsonNodes.get("name").asText());
  }
}
