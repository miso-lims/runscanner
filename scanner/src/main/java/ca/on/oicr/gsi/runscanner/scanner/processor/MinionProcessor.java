package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MinionProcessor extends BaseOxfordNanoporeProcessor {
  private static final Pattern FAIL_DIR = Pattern.compile("/fail$");
  private static final Pattern TMP_DIR = Pattern.compile("/tmp$");
  private static final Pattern COMPLETE_READ = Pattern.compile("complete_read");
  private static final Pattern RAW_DATA = Pattern.compile("raw_data");
  private static final Pattern DOWNLOADS = Pattern.compile("/downloads$");
  private static final Pattern UPLOAD = Pattern.compile("/upload");

  public MinionProcessor(Builder builder) {
    super(builder);
  }

  @Override
  protected boolean excludedDirectoryFormat(Path path) {
    String strPath = path.toString();
    return COMPLETE_READ.matcher(strPath).find()
        || TMP_DIR.matcher(strPath).find()
        || RAW_DATA.matcher(strPath).find()
        || DOWNLOADS.matcher(strPath).find()
        || UPLOAD.matcher(strPath).find()
        || FAIL_DIR.matcher(strPath).find();
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
        path.resolve("fast5"),
        path.resolve("0"));
  }

  @Override
  protected void additionalProcess(OxfordNanoporeNotificationDto onnd, IHDF5Reader reader) {
    if (reader.hasAttribute(trackingId, "device_id")) {
      String deviceId = reader.string().getAttr(trackingId, "device_id");
      if (!(deviceId.equals(""))) onnd.setSequencerName(deviceId);
    }
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new MinionProcessor(builder);
  }
}
