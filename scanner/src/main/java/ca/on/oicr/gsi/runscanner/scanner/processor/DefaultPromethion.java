package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PromethionNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ch.systemsx.cisd.hdf5.HDF5FactoryProvider;
import ch.systemsx.cisd.hdf5.IHDF5StringReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import java.util.stream.Stream;

public class DefaultPromethion extends RunProcessor {
  private final String TRACKING_ID = "UniqueGlobalKey/tracking_id";
  private final String CONTEXT_TAGS = "UniqueGlobalKey/context_tags";
  private final int LANE_COUNT = 1;

  public DefaultPromethion(Builder builder) {
    super(builder);
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    // TODO Implement this for real
    return Stream.empty();
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    PromethionNotificationDto pnd = new PromethionNotificationDto();
    IHDF5StringReader reader = HDF5FactoryProvider.get().openForReading(runDirectory).string();

    pnd.setRunAlias(reader.getAttr(TRACKING_ID, "run_id"));
    pnd.setSequencerFolderPath(runDirectory.getParent());
    pnd.setSequencerPosition(reader.getAttr(TRACKING_ID, "device_id"));
    pnd.setContainerSerialNumber(reader.getAttr(TRACKING_ID, "flow_cell_id"));
    pnd.setContainerModel(
        reader.getAttr(CONTEXT_TAGS, "flowcell_type")); // Might be something else!
    pnd.setLaneCount(LANE_COUNT);
    pnd.setHealthType(HealthType.UNKNOWN);
    pnd.setStartDate(
        ZonedDateTime.parse(reader.getAttr(TRACKING_ID, "exp_start_time")).toLocalDateTime());

    return pnd;
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new DefaultPromethion(builder);
  }
}
