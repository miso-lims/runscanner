import static org.junit.Assert.fail;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import java.io.File;
import java.io.IOException;

public class MinionProcessorTest extends AbstractProcessorTest {
  public MinionProcessorTest(Class<? extends NotificationDto> clazz) {
    super(clazz);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {
    fail("Test not yet implemented.");
    return null;
  }
}
