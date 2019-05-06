import static org.junit.Assert.fail;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import java.io.File;
import java.io.IOException;

public class MinionProcessorTest extends AbstractProcessorTest {
  public MinionProcessorTest() {
    super(OxfordNanoporeNotificationDto.class);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {
    return null;
  }

  @Override
  public void testGoldens() throws IOException {
    fail("Test not yet implemented.");
  }
}
