package ca.on.oicr.gsi.runscanner.scanner.processor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PipelineRun;
import ca.on.oicr.gsi.runscanner.dto.UltimaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.PipelineStatus;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.dto.type.WorkflowRunStatus;
import ca.on.oicr.gsi.runscanner.dto.ultima.CramAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.MetadataAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaWorkflowRun;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
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

    UltimaGoogleBucketClient mockBucketClient = mock(UltimaGoogleBucketClient.class);
    when(mockBucketClient.ls(any())).thenReturn(Collections.emptyList());

    // Create the processor with the Mock clients instead of the real ones
    DefaultUltima processor =
        new DefaultUltima(
            new Builder(Platform.ULTIMA, "unittest", null), mockClient, mockBucketClient);

    // Manually populate the cache to avoid a "missing from cache" error
    processor.getRunsFromRoot(directory.getParentFile()).count();

    return processor.process(directory, TimeZone.getTimeZone("America/Toronto"));
  }

  /**
   * Verifies that GCS bucket contents (run folder, barcode folders, and files) are correctly mapped
   * into UltimaWorkflowRun/UltimaAnalysisUnit/AnalysisFile structures on the DTO.
   */
  @Test
  public void testWorkflowRuns() throws IOException {
    File directory = new File(this.getClass().getResource("/ultima/123456").getPath());

    // Load mock API responses
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

    // GCS path layout under test:
    // Sequencer root → run folder: {runId}-{YYYYMMDD}_{suffix}/
    //   Run folder → barcode folder: {runId}-{library}-{barcode}/
    //     Barcode folder → files: sample.cram, sample.csv
    String bucket = "test-bucket";
    String runFolderPath = "123456-20240507_1234/";
    String barcodeFolderPath = runFolderPath + "123456-lib1-lib2-TT/";
    String cramBlobName = barcodeFolderPath + "123456-lib1-lib2-TT.cram";
    String metaBlobName = barcodeFolderPath + "123456-lib1-lib2-TT.csv";

    // Fixed timestamp used for all GCS blob create/update times
    OffsetDateTime fixedTime = OffsetDateTime.of(2024, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC);

    // Run folder blob returned when listing the sequencer root directory
    Blob runFolderBlob = mock(Blob.class);
    when(runFolderBlob.isDirectory()).thenReturn(true);
    when(runFolderBlob.getName()).thenReturn(runFolderPath);
    when(runFolderBlob.getBucket()).thenReturn(bucket);

    // Barcode folder blob returned when listing the run folder
    Blob barcodeFolderBlob = mock(Blob.class);
    when(barcodeFolderBlob.isDirectory()).thenReturn(true);
    when(barcodeFolderBlob.getName()).thenReturn(barcodeFolderPath);
    when(barcodeFolderBlob.getBucket()).thenReturn(bucket);

    Blob cramBlob = mock(Blob.class);
    when(cramBlob.isDirectory()).thenReturn(false);
    when(cramBlob.getName()).thenReturn(cramBlobName);
    when(cramBlob.getBucket()).thenReturn(bucket);
    when(cramBlob.getCrc32c()).thenReturn("AAAAAA==");
    when(cramBlob.getSize()).thenReturn(1000L);
    when(cramBlob.getCreateTimeOffsetDateTime()).thenReturn(fixedTime);
    when(cramBlob.getUpdateTimeOffsetDateTime()).thenReturn(fixedTime);

    // Metadata (index) file blob
    Blob metaBlob = mock(Blob.class);
    when(metaBlob.isDirectory()).thenReturn(false);
    when(metaBlob.getName()).thenReturn(metaBlobName);
    when(metaBlob.getBucket()).thenReturn(bucket);
    when(metaBlob.getCrc32c()).thenReturn("AAAAAA==");
    when(metaBlob.getSize()).thenReturn(100L);
    when(metaBlob.getCreateTimeOffsetDateTime()).thenReturn(fixedTime);
    when(metaBlob.getUpdateTimeOffsetDateTime()).thenReturn(fixedTime);

    UltimaGoogleBucketClient mockBucketClient = mock(UltimaGoogleBucketClient.class);
    when(mockBucketClient.ls(any())).thenReturn(Collections.emptyList());
    // Listing the sequencer root (local test resource path) returns the run folder
    when(mockBucketClient.ls(directory.getParentFile().getPath()))
        .thenReturn(List.of(runFolderBlob));
    // Listing the run folder (bucket/path) returns the barcode folder
    when(mockBucketClient.ls(bucket + "/" + runFolderPath)).thenReturn(List.of(barcodeFolderBlob));
    // Listing the barcode folder returns the CRAM and metadata files
    when(mockBucketClient.ls(bucket + "/" + barcodeFolderPath))
        .thenReturn(List.of(cramBlob, metaBlob));

    DefaultUltima processor =
        new DefaultUltima(
            new Builder(Platform.ULTIMA, "unittest", null), mockClient, mockBucketClient);
    processor.getRunsFromRoot(directory.getParentFile()).count();
    UltimaNotificationDto result =
        (UltimaNotificationDto)
            processor.process(directory, TimeZone.getTimeZone("America/Toronto"));

    // One pipeline run per run folder
    List<PipelineRun> pipelineRuns = result.getPipelineRuns();
    assertEquals(1, pipelineRuns.size());

    UltimaPipelineRun pipelineRun = (UltimaPipelineRun) pipelineRuns.get(0);
    assertEquals(1, pipelineRun.getAttempt());
    assertEquals(PipelineStatus.COMPLETE, pipelineRun.getPipelineStatus());

    // One workflow run per pipeline run
    List<UltimaWorkflowRun> workflowRuns = pipelineRun.getWorkflowRuns();
    assertEquals(1, workflowRuns.size());

    UltimaWorkflowRun workflowRun = workflowRuns.get(0);
    assertEquals("initialCramGeneration", workflowRun.getWorkflowName());
    assertEquals(WorkflowRunStatus.COMPLETE, workflowRun.getWorkflowRunStatus());
    assertEquals("TestAnalysisRecipe", workflowRun.getSoftwareVersion());
    // "Eastern Standard Time" is not a valid Java timezone ID so TimeZone.getTimeZone() falls back
    // to UTC, meaning the parsed Analysis_Start_Time is interpreted as-is in UTC.
    assertEquals(Instant.parse("2024-05-07T12:00:00Z"), workflowRun.getStartTime());

    // One analysis unit per barcode folder
    List<UltimaAnalysisUnit> units = workflowRun.getAnalysisOutputs();
    assertEquals(1, units.size());

    UltimaAnalysisUnit unit = units.get(0);
    assertEquals("TT", unit.getBarcode());
    assertEquals("lib1-lib2", unit.getLibrary());

    // Two files: one CRAM, one metadata
    List<AnalysisFile> files = unit.getFiles();
    assertEquals(2, files.size());

    CramAnalysisFile cramFile =
        (CramAnalysisFile)
            files.stream().filter(f -> f instanceof CramAnalysisFile).findFirst().orElseThrow();
    assertEquals(Path.of("gs:/", bucket + "/" + cramBlobName), cramFile.getPath());
    assertEquals("AAAAAA==", cramFile.getCrc32Checksum());
    assertEquals(1000L, cramFile.getSize());
    assertEquals(fixedTime.toInstant(), cramFile.getCreatedTime());
    assertEquals(fixedTime.toInstant(), cramFile.getModifiedTime());

    MetadataAnalysisFile metaFile =
        (MetadataAnalysisFile)
            files.stream().filter(f -> f instanceof MetadataAnalysisFile).findFirst().orElseThrow();
    assertEquals(Path.of("gs:/", bucket + "/" + metaBlobName), metaFile.getPath());
    assertEquals("AAAAAA==", metaFile.getCrc32Checksum());
    assertEquals(100L, metaFile.getSize());
    assertEquals(fixedTime.toInstant(), metaFile.getCreatedTime());
    assertEquals(fixedTime.toInstant(), metaFile.getModifiedTime());
  }

  @Override
  @Test
  public void testGoldens() throws IOException {
    checkDirectory("/ultima");
  }
}
