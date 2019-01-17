import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.DefaultIllumina;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;
import org.junit.Test;

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

  @Test
  public void testGoldens() throws IOException {
    if (!System.getProperty("skipIllumina", "true").equals("true")) {
      checkDirectory("/illumina");
    }
  }
}
