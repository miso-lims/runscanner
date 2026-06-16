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

    // Load the mock JSONs data from the test directory
    File mockRunsummaryFile = new File(directory, "ultima-api-runsummary-response.json");
    JsonNode mockRunsummaryJson = mapper.readTree(mockRunsummaryFile);
    File mockSampleDBFile = new File(directory, "sampledb-api-response.json");
    JsonNode mockSampleDBJson = mapper.readTree(mockSampleDBFile);
    File mockTTMetricsFile = new File(directory, "ultima-api-TT-metric-response.json");
    JsonNode mockTTMetricsJson = mapper.readTree(mockTTMetricsFile);

    String runId = mockRunsummaryJson.path("runid").asText("");
    String samplePlate = mockRunsummaryJson.path("AMP_SamplePlate").asText("");

    UltimaApiClient mockClient = mock(UltimaApiClient.class);
    when(mockClient.fetchAllRunSummaries())
        .thenReturn(Collections.singletonList(mockRunsummaryJson));
    when(mockClient.fetchSampleDB(samplePlate)).thenReturn(mockSampleDBJson);
    when(mockClient.fetchBarcodeMetrics(runId, "TT")).thenReturn(mockTTMetricsJson);

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
