package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.junit.Assert;

public class RevioPacBioProcessorTest<T> extends AbstractProcessorTest<PacBioNotificationDto> {
  private final RevioPacBioProcessor instance =
      new RevioPacBioProcessor(new Builder(Platform.PACBIO, "unittest", null));

  public RevioPacBioProcessorTest() {
    super(PacBioNotificationDto.class);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {
    return instance.process(directory, TimeZone.getTimeZone("America/Toronto"));
  }

  @Override
  public void beforeComparison(PacBioNotificationDto reference, PacBioNotificationDto result) {
    // Check if run name matches specific run directory
    if (result.getRunAlias().equals("r84028_20250129_101315")) {
      // Assert that a start time (any) was detected
      Assert.assertNotNull(result.getStartDate());

      // Need to modify result to match reference.json because for runs that have just started,
      // the only information available are run name and start time using file creation. For
      // tests, we cannot use file creation time as these aren't stored in git
      result.setStartDate(
          LocalDateTime.parse(
                  "2025-03-10 20:11:46,494376012Z",
                  DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSSSSSSSSXX"))
              .atOffset(ZoneOffset.UTC)
              .toInstant());
    }
  }

  @Override
  public void testGoldens() throws IOException {
    checkDirectory("/pacbiorevio");
  }
}
