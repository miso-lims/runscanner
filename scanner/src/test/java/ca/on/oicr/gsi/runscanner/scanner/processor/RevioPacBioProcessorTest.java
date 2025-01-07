package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

public class RevioPacBioProcessorTest extends AbstractProcessorTest {
  private final RevioPacBioProcessor instance =
      new RevioPacBioProcessor(new Builder(Platform.PACBIO, "unittest", null), "Address");

  @Override
  protected NotificationDto process(File directory) throws IOException {
    return instance.process(directory, TimeZone.getTimeZone("America/Toronto"));
  }

  public RevioPacBioProcessorTest() {
    super(PacBioNotificationDto.class);
  }

  @Override
  public void testGoldens() throws IOException {
    checkDirectory("/pacbiorevio");
  }
}
