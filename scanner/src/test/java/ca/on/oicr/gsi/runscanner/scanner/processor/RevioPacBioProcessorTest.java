package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import org.junit.Assert;

public class RevioPacBioProcessorTest<T> extends AbstractProcessorTest<PacBioNotificationDto> {
  private final RevioPacBioProcessor instance =
      new RevioPacBioProcessor(new Builder(Platform.PACBIO, "unittest", null));
  private NotificationDto afterProcessing;

  public RevioPacBioProcessorTest() {
    super(PacBioNotificationDto.class);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {
    afterProcessing = instance.process(directory, TimeZone.getTimeZone("America/Toronto"));
    return afterProcessing;
    // return instance.process(directory, TimeZone.getTimeZone("America/Toronto"));
  }

  @Override
  public void beforeComparison(PacBioNotificationDto reference, PacBioNotificationDto result) {
    // Check if run name matches new run directory
    if (afterProcessing.getRunAlias().equals(result.getRunAlias())) {
      // Assert that a start time (any) was detected
      Assert.assertNotNull(afterProcessing.getStartDate());

      // Set the start date to match the reference json
      afterProcessing.setStartDate(reference.getStartDate());
    }
  }

  @Override
  public void testGoldens() throws IOException {
    checkDirectory("/pacbiorevio");
  }
}
