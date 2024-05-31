package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

public class IlluminaProcessorTest extends AbstractProcessorTest {
  private final DefaultIllumina instance =
      new DefaultIllumina(new Builder(Platform.ILLUMINA, "unittest", null), true);

  public IlluminaProcessorTest() {
    super(IlluminaNotificationDto.class);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {
    return instance.process(directory, TimeZone.getTimeZone("America/Toronto"));
  }

  @Override
  public void testGoldens() throws IOException {
    checkDirectory("/illumina");
  }
}
