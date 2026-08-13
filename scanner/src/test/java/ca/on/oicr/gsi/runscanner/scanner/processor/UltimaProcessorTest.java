package ca.on.oicr.gsi.runscanner.scanner.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaWorkflowRun;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import ca.on.oicr.gsi.runscanner.scanner.processor.ultima.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.CRC32;
import org.apache.commons.io.FileUtils;
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

    UltimaStorageClient mockStorageClient = mock(UltimaStorageClient.class);
    when(mockStorageClient.ls(any())).thenReturn(Collections.emptyList());

    // Create the processor with the Mock clients instead of the real ones
    DefaultUltima processor =
        new DefaultUltima(
            new Builder(Platform.ULTIMA, "unittest", null), mockClient, mockStorageClient);

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
    String runFolderName = "123456-20240507_1234";
    String runFolderFullPath = bucket + "/" + runFolderName + "/";
    String barcodeFolderName = "123456-lib1-lib2-TT";
    String barcodeFolderFullPath = runFolderFullPath + barcodeFolderName + "/";
    String cramFileName = barcodeFolderName + ".cram";
    String metaFileName = barcodeFolderName + ".csv";

    // Fixed timestamp used for all GCS blob create/update times
    OffsetDateTime fixedTime = OffsetDateTime.of(2024, 5, 7, 10, 0, 0, 0, ZoneOffset.UTC);

    // Run folder entry returned when listing the sequencer root directory
    UltimaStorageEntry runFolderEntry = mock(UltimaStorageEntry.class);
    when(runFolderEntry.isDirectory()).thenReturn(true);
    when(runFolderEntry.getName()).thenReturn(runFolderName);
    when(runFolderEntry.getFullPath()).thenReturn(runFolderFullPath);

    // Barcode folder entry returned when listing the run folder
    UltimaStorageEntry barcodeFolderEntry = mock(UltimaStorageEntry.class);
    when(barcodeFolderEntry.isDirectory()).thenReturn(true);
    when(barcodeFolderEntry.getName()).thenReturn(barcodeFolderName);
    when(barcodeFolderEntry.getFullPath()).thenReturn(barcodeFolderFullPath);

    UltimaStorageEntry cramEntry = mock(UltimaStorageEntry.class);
    when(cramEntry.isDirectory()).thenReturn(false);
    when(cramEntry.getName()).thenReturn(cramFileName);
    when(cramEntry.getPath())
        .thenReturn(URI.create("gs://" + barcodeFolderFullPath + cramFileName));
    when(cramEntry.getCrc32Checksum()).thenReturn("AAAAAA==");
    when(cramEntry.getSize()).thenReturn(1000L);
    when(cramEntry.getCreatedTime()).thenReturn(fixedTime.toInstant());
    when(cramEntry.getModifiedTime()).thenReturn(fixedTime.toInstant());

    // Metadata (index) file entry
    UltimaStorageEntry metaEntry = mock(UltimaStorageEntry.class);
    when(metaEntry.isDirectory()).thenReturn(false);
    when(metaEntry.getName()).thenReturn(metaFileName);
    when(metaEntry.getPath())
        .thenReturn(URI.create("gs://" + barcodeFolderFullPath + metaFileName));
    when(metaEntry.getCrc32Checksum()).thenReturn("AAAAAA==");
    when(metaEntry.getSize()).thenReturn(100L);
    when(metaEntry.getCreatedTime()).thenReturn(fixedTime.toInstant());
    when(metaEntry.getModifiedTime()).thenReturn(fixedTime.toInstant());

    UltimaStorageClient mockStorageClient = mock(UltimaStorageClient.class);
    when(mockStorageClient.ls(any())).thenReturn(Collections.emptyList());
    // Listing the sequencer root (local test resource path) returns the run folder
    when(mockStorageClient.ls(directory.getParentFile().getPath()))
        .thenReturn(List.of(runFolderEntry));
    // Listing the run folder (bucket/path) returns the barcode folder
    when(mockStorageClient.ls(runFolderFullPath)).thenReturn(List.of(barcodeFolderEntry));
    // Listing the barcode folder returns the CRAM and metadata files
    when(mockStorageClient.ls(barcodeFolderFullPath)).thenReturn(List.of(cramEntry, metaEntry));

    DefaultUltima processor =
        new DefaultUltima(
            new Builder(Platform.ULTIMA, "unittest", null), mockClient, mockStorageClient);
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
    assertEquals("CRAMGeneration", workflowRun.getWorkflowName());
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

    // Only the CRAM file is included; the metadata file has no consumer yet and is excluded.
    List<AnalysisFile> files = unit.getFiles();
    assertEquals(1, files.size());

    CramAnalysisFile cramFile = (CramAnalysisFile) files.get(0);
    assertEquals(URI.create("gs://" + barcodeFolderFullPath + cramFileName), cramFile.getPath());
    assertEquals("AAAAAA==", cramFile.getCrc32Checksum());
    assertEquals(1000L, cramFile.getSize());
    assertEquals(fixedTime.toInstant(), cramFile.getCreatedTime());
    assertEquals(fixedTime.toInstant(), cramFile.getModifiedTime());
  }

  /**
   * Verifies that the same run folder/barcode folder/CRAM layout is correctly mapped when served by
   * a real local filesystem directory via {@link LocalStorageClient}, instead of a mocked GCS
   * bucket.
   */
  @Test
  public void testLocalStorageWorkflowRuns() throws IOException {
    File directory = new File(this.getClass().getResource("/ultima/123456").getPath());

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

    // Local path layout under test, mirroring testWorkflowRuns():
    // sequencerRoot -> run folder -> barcode folder -> files
    Path sequencerRoot = Files.createTempDirectory("ultima-local-test");
    try {
      Path runFolder = Files.createDirectory(sequencerRoot.resolve("123456-20240507_1234"));
      Path barcodeFolder = Files.createDirectory(runFolder.resolve("123456-lib1-lib2-TT"));
      Path cramFile = Files.createFile(barcodeFolder.resolve("123456-lib1-lib2-TT.cram"));
      Files.write(cramFile, new byte[1000]);
      Files.createFile(barcodeFolder.resolve("123456-lib1-lib2-TT.csv"));

      DefaultUltima processor =
          new DefaultUltima(
              new Builder(Platform.ULTIMA, "unittest", null), mockClient, new LocalStorageClient());
      processor.getRunsFromRoot(sequencerRoot.toFile()).count();
      UltimaNotificationDto result =
          (UltimaNotificationDto)
              processor.process(
                  new File(sequencerRoot.toFile(), runId), TimeZone.getTimeZone("America/Toronto"));

      UltimaPipelineRun pipelineRun = (UltimaPipelineRun) result.getPipelineRuns().get(0);
      assertEquals(PipelineStatus.COMPLETE, pipelineRun.getPipelineStatus());

      UltimaWorkflowRun workflowRun = pipelineRun.getWorkflowRuns().get(0);
      List<UltimaAnalysisUnit> units = workflowRun.getAnalysisOutputs();
      assertEquals(1, units.size());

      UltimaAnalysisUnit unit = units.get(0);
      assertEquals("TT", unit.getBarcode());
      assertEquals("lib1-lib2", unit.getLibrary());

      // Only the CRAM file is included; the metadata file has no consumer yet and is excluded.
      List<AnalysisFile> files = unit.getFiles();
      assertEquals(1, files.size());

      CramAnalysisFile cramAnalysisFile = (CramAnalysisFile) files.get(0);
      assertEquals(cramFile.toAbsolutePath().toUri(), cramAnalysisFile.getPath());
      assertEquals(1000L, cramAnalysisFile.getSize());
      // Local storage has no equivalent to GCS's CRC32C; a plain CRC32 is computed from disk.
      CRC32 expectedCrc32 = new CRC32();
      expectedCrc32.update(Files.readAllBytes(cramFile));
      assertEquals(Long.toString(expectedCrc32.getValue()), cramAnalysisFile.getCrc32Checksum());
      assertNotNull(cramAnalysisFile.getCreatedTime());
      assertNotNull(cramAnalysisFile.getModifiedTime());
    } finally {
      FileUtils.deleteDirectory(sequencerRoot.toFile());
    }
  }

  @Override
  @Test
  public void testGoldens() throws IOException {
    checkDirectory("/ultima");
  }
}
