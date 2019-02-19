package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import java.util.stream.Stream;

public class DefaultMinion extends RunProcessor {

  public DefaultMinion(Builder builder) {
    super(builder);
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    return null;
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    return null;
  }

  public static RunProcessor create(Builder builder, ObjectNode jsonNodes) {
    return new DefaultMinion(builder);
  }
}
