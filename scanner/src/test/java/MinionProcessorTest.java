import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.MinionProcessor;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

public class MinionProcessorTest extends AbstractProcessorTest {
  private final MinionProcessor instance =
      new MinionProcessor(
          new RunProcessor.Builder(Platform.OXFORDNANOPORE, "minion", null), "unittest");

  public MinionProcessorTest() {
    super(OxfordNanoporeNotificationDto.class);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {
    return instance.process(directory, TimeZone.getTimeZone("America/Toronto"));
  }

  @Override
  public void testGoldens() throws IOException {
    checkDirectory("/oxfordnanopore/minion");
  }
}
