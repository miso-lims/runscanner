package ca.on.oicr.gsi.runscanner.scanner.processor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.UltimaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.TimeZone;
import org.junit.Test;

public class UltimaProcessorTest extends AbstractProcessorTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  public UltimaProcessorTest() {
    super(UltimaNotificationDto.class);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {

    // Load the mock JSON data from the test directory
    File mockFile = new File(directory, "ultima-api-response.json");
    JsonNode mockJson = mapper.readTree(mockFile);

    UltimaApiClient mockClient = mock(UltimaApiClient.class);
    when(mockClient.fetchAllRunSummaries()).thenReturn(Collections.singletonList(mockJson));

    // Create the processor with the Mock client instead of the real one
    DefaultUltima processor =
        new DefaultUltima(new Builder(Platform.ULTIMA, "unittest", null), mockClient);

    // Manually populate the cache to avoid a "missing from cache" error
    processor.getRunsFromRoot(directory.getParentFile()).count();

    return processor.process(directory, TimeZone.getTimeZone("America/Toronto"));
  }

  @Override
  @Test
  public void testGoldens() throws IOException {
    checkDirectory("/ultima");
  }
}
