package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MinionProcessor extends BaseOxfordNanoporeProcessor {
  private static final Pattern READS_DIR = Pattern.compile("/reads$");
  private static final Pattern PASS_DIR = Pattern.compile("/pass$");
  private static final Pattern FAIL_DIR = Pattern.compile("/fail$");
  private static final Pattern TMP_DIR = Pattern.compile("/tmp$");
  private static final Pattern ENDS_WITH_NUM = Pattern.compile("/[0-9]+$");
  private static final Pattern FAST5_DIR = Pattern.compile("/fast5/[0-9]+$");

  public MinionProcessor(Builder builder, String seqName) {
    super(builder, seqName);
  }

  @Override
  protected boolean excludedDirectoryFormat(Path path) {
    String strPath = path.toString();
    return READS_DIR.matcher(strPath).find()
        || PASS_DIR.matcher(strPath).find()
        || FAIL_DIR.matcher(strPath).find()
        || TMP_DIR.matcher(strPath).find()
        || (!FAST5_DIR.matcher(strPath).find() && ENDS_WITH_NUM.matcher(strPath).find());
  }

  @Override
  protected Stream<Path> readsDirectoryForRun(Path path) {
    return Stream.of(
        path.resolve("fast5_pass"),
        path.resolve("fastq_pass"),
        path.resolve("fast5_fail"),
        path.resolve("fastq_fail"),
        path.resolve("fast5_skip"),
        path.resolve("sequencing_summary"),
        path.resolve(Paths.get("fast5", "0")));
  }

  @Override
  protected void additionalProcess(OxfordNanoporeNotificationDto onnd, IHDF5Reader reader) {
    onnd.setContainerModel(reader.string().getAttr(contextTags, "sequencing_kit"));
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new MinionProcessor(builder, jsonNodes.get("name").asText());
  }
}
