package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import java.util.stream.Stream;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.h5.*;

public class DefaultPromethion extends RunProcessor {

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
    H5File file = new H5File(runDirectory.getPath());
    try {
      HObject h = file.get("/UniqueGlobalKey");
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new DefaultPromethion(builder);
  }
}
