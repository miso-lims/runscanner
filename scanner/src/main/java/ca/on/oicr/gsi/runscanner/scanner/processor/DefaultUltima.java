package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.Consumable;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.UltimaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ca.on.oicr.gsi.runscanner.dto.type.UltimaProcessStatus;
import ca.on.oicr.gsi.runscanner.dto.ultima.CramAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.MetadataAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaWorkflowRun;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloud.storage.Blob;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultUltima extends RunProcessor {

  // Caches populated during getRunsFromRoot() and consumed during process()
  protected final Map<String, JsonNode> runCache = new ConcurrentHashMap<>();
  protected final Map<String, String> runFolderCache = new ConcurrentHashMap<>();

  DateTimeFormatter NEXUS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  String CONTROL_BARCODE = "TT";

  private static final Logger log = LoggerFactory.getLogger(DefaultUltima.class);
  private final UltimaApiClient apiClient;
  private final UltimaGoogleBucketClient googleBucketClient;

  public static DefaultUltima create(Builder builder, ObjectNode parameters) {
    try {
      return new DefaultUltima(
          builder,
          fetchNexusApiUrl(parameters),
          fetchNexusApiTokenFile(parameters),
          fetchGoogleServiceAccount(parameters),
          fetchOptionalParameter(parameters, "sampleDBApiAddress"),
          fetchOptionalParameter(parameters, "sampleDBApiTokenFile"));
    } catch (IOException e) {
      log.error("Could not create Ultima run processor: {}", e.getMessage(), e);
      return null;
    }
  }

  protected DefaultUltima(
      Builder builder,
      String apiUrlNexus,
      String tokenPathNexus,
      String googleServiceAccount,
      String apiUrlSampleDB,
      String tokenPathSampleDB)
      throws IOException {
    super(builder);
    this.apiClient =
        new UltimaApiClient(apiUrlNexus, tokenPathNexus, apiUrlSampleDB, tokenPathSampleDB);
    this.googleBucketClient = new UltimaGoogleBucketClient(googleServiceAccount);
  }

  protected DefaultUltima(
      Builder builder, UltimaApiClient apiClient, UltimaGoogleBucketClient googleBucketClient) {
    super(builder);
    this.apiClient = apiClient;
    this.googleBucketClient = googleBucketClient;
  }

  /**
   * @param parameters ObjectNode
   * @return String with base URL for the ULTIMA Nexus API
   */
  private static String fetchNexusApiUrl(ObjectNode parameters) {
    if (parameters.hasNonNull("nexusApiAddress")) {
      return parameters.get("nexusApiAddress").asText();
    } else {
      log.error("No Nexus API URL configured for Ultima, this config should be invalid");
      return null;
    }
  }

  /**
   * @param parameters ObjectNode
   * @return String filename where Nexus token is stored
   */
  private static String fetchNexusApiTokenFile(ObjectNode parameters) {
    if (parameters.hasNonNull("nexusApiTokenFile")) {
      return parameters.get("nexusApiTokenFile").asText();
    } else {
      log.error("No Nexus API Token configured for Ultima, this config should be invalid");
      return null;
    }
  }

  /**
   * @param parameters ObjectNode
   * @return String with base URL for the ULTIMA Nexus API
   */
  private static String fetchGoogleServiceAccount(ObjectNode parameters) {
    if (parameters.hasNonNull("googleServiceAccount")) {
      return parameters.get("googleServiceAccount").asText();
    } else {
      log.error(
          "No Google Bucket Service Account configured for Ultima, this config should be invalid");
      return null;
    }
  }

  /**
   * @param parameters ObjectNode
   * @param parameterName String
   * @return The value stored at the parameter name or null if it doesn't exist
   */
  private static String fetchOptionalParameter(ObjectNode parameters, String parameterName) {
    if (parameters.hasNonNull(parameterName)) {
      return parameters.get(parameterName).asText();
    } else {
      return null;
    }
  }

  /**
   * Instead of scanning disk, we fetch the list of Run IDs from the API. We return "Virtual" File
   * objects representing each Run ID.
   */
  @Override
  public Stream<File> getRunsFromRoot(File root) {

    try {
      List<JsonNode> allRunInfo = apiClient.fetchAllRunSummaries();

      populateRunFolderCache(root.getPath());

      // nexus orders the runs by increasing runId (newer Run Id = higher num)
      // we want runscanner to scan newer runs first
      return allRunInfo.stream()
          .filter(n -> n.hasNonNull("runid") && !n.path("runid").asText().isBlank())
          .sorted(Comparator.comparingLong((JsonNode n) -> n.path("runid").asLong()).reversed())
          .map(
              node -> {
                String runId = node.path("runid").asText();
                runCache.put(runId, node);
                return new File(root, runId);
              });
    } catch (Exception e) {
      log.error("Failed to fetch run list from Ultima API", e);
      return Stream.empty();
    }
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    String runId = runDirectory.getName();

    // Get the run information from the cached API call
    JsonNode json = runCache.get(runId);

    if (json == null) {
      throw new IOException("Data for run " + runId + " missing from cache. API sync failure.");
    }

    UltimaNotificationDto dto = new UltimaNotificationDto();

    dto.setRunAlias(runId);
    dto.setSequencerName(json.path("sysid").asText());
    dto.setSoftware(json.path("SequencingRecipe").asText());

    dto.setCompletedFlows(json.path("completedflownum").asInt());
    dto.setExpectedFlows(json.path("numflows").asInt());
    dto.setReadLength(json.path("rl").asDouble());
    dto.setWaferShelf(json.path("wafershelf").asInt());

    ObjectMapper mapper = createObjectMapper();
    List<ObjectNode> metrics = new ArrayList<>();
    ObjectNode chartNode = mapper.createObjectNode();
    chartNode.put("type", "chart");
    ArrayNode values = chartNode.putArray("values");

    double numReadPass = json.path("num_reads_pass_filter").asDouble();
    double numBeads = json.path("numbeads").asDouble();
    double passFilterPercent = (numBeads > 0) ? (numReadPass / numBeads) * 100 : 0;
    double controlBasesQ30 = getControlQ30Bases(runId);

    values
        .addObject()
        .put("name", "Total Beads (M)")
        .put("value", String.format("%,.6f", numBeads));
    values
        .addObject()
        .put("name", "Output Reads")
        .put("value", String.format("%,d", json.path("pf_output_reads").asLong(0)));
    values
        .addObject()
        .put("name", "Pass Filter %")
        .put("value", String.format("%.2f", passFilterPercent));
    values
        .addObject()
        .put("name", "Indel Rate TT %")
        .put("value", json.path("indel_rate").asText());
    values
        .addObject()
        .put("name", "Control Sample Bases > Q30 %")
        .put("value", String.format("%.2f", controlBasesQ30));
    metrics.add(chartNode);
    dto.setMetrics(mapper.writeValueAsString(metrics));

    dto.setSequencerPosition(json.path("Chuck").asText());

    String wafer = json.path("wafer").asText();
    dto.setContainerSerialNumber(wafer);
    dto.setContainerModel(extractModel(wafer));

    int pctCompleted = json.path("runstatus").asInt(); // Percentage 0-100
    int succeeded = json.path("isruncompleted").asInt(); // Successful run 0 or 1
    String errmsg = json.path("errormsg").asText("");

    // the first and last digit are vestigial and should be ignored.
    // the second and third digit are the analysis and upload status respectively.
    // 0 is not started, 1 is in progress, 2 is complete, and 3+ is error
    String analAndUpStatus = json.path("analysisstatus").asText("0000");

    UltimaProcessStatus analysisStatus =
        UltimaProcessStatus.fromCode(Character.getNumericValue(analAndUpStatus.charAt(1)));
    dto.setAnalysisStatus(analysisStatus);
    UltimaProcessStatus uploadStatus =
        UltimaProcessStatus.fromCode(Character.getNumericValue(analAndUpStatus.charAt(2)));
    dto.setUploadStatus(uploadStatus);
    UltimaProcessStatus sequencingStatus = sequencingComplete(pctCompleted, succeeded, errmsg);
    dto.setSequencingStatus(sequencingStatus);

    // Ultima should only be considered complete once upload is complete (crams not available before
    // that time)
    HealthType health = translateStatus(sequencingStatus, analysisStatus, uploadStatus);
    dto.setHealthType(health);

    String zoneString = json.path("timezone").asText();
    LocalDateTime ldt =
        LocalDateTime.parse(json.path("startdatetime").asText(), NEXUS_DATE_FORMATTER);
    Instant startDate = extractTime(ldt, zoneString);
    dto.setStartDate(startDate);
    if (health.isDone()) {
      Duration runtime = Duration.ofMinutes(json.path("runtime").asInt());
      dto.setCompletionDate(calculateCompletedDate(startDate, runtime));
    }

    // No lanes on CD wafers (yet)
    dto.setLaneCount(1);
    // No paired ends
    dto.setPairedEndRun(false);

    String ampSamplePlate = json.path("AMP_SamplePlate").asText("");
    dto.setPoolNames(getPoolsFromSampleDB(ampSamplePlate));

    List<Consumable> consumables = new ArrayList<>();
    consumables.add(new Consumable("Amplification Sample Plate Serial Number", ampSamplePlate));
    consumables.add(
        new Consumable(
            "Amplification Chilled Rack Lot Number", json.path("AMP_ChilledRack").asText()));
    consumables.add(
        new Consumable("Amplification RT Rack Lot Number", json.path("AMP_RTRack").asText()));
    consumables.add(
        new Consumable("Amplification Tube Array Lot Number", json.path("AMP_TubeArray").asText()));
    consumables.add(
        new Consumable(
            "Amplification Break Container Lot Number", json.path("AMP_BreakContainer").asText()));
    consumables.add(
        new Consumable("Amplification Wash 1 Lot Number", json.path("AMP_Wash1").asText()));
    consumables.add(
        new Consumable("Amplification Wash 2 Lot Number", json.path("AMP_Wash2").asText()));
    consumables.add(
        new Consumable(
            "Amplification Enrichment Bead Lot Number", json.path("AMP_EnrichmentBead").asText()));
    consumables.add(new Consumable("Sequencing Rack Lot Number", json.path("SampleRack").asText()));
    consumables.add(new Consumable("Sample Tube Lot Number", json.path("SampleTube").asText()));
    consumables.add(
        new Consumable(
            "Sequencing Cartridge Lot Number", json.path("SequencingCartridge").asText()));
    consumables.add(
        new Consumable("Wash Container Lot Number", json.path("WashContainer").asText()));
    consumables.add(new Consumable("Wafer Serial Number", json.path("Wafer").asText()));

    dto.setConsumables(consumables);

    String runFolder = runFolderCache.get(runId);
    dto.setSequencerFolderPath(runFolder);
    dto.setWorkflowRuns(
        runFolder != null
            ? List.of(buildWorkflowRun(runId, runFolder, json, uploadStatus))
            : List.of());

    return dto;
  }

  private UltimaWorkflowRun buildWorkflowRun(
      String runId, String runFolder, JsonNode json, UltimaProcessStatus uploadStatus) {
    UltimaWorkflowRun workflowRun = new UltimaWorkflowRun();

    // Analysis_Start_Time uses the same "yyyy-MM-dd HH:mm:ss" format as startdatetime
    String analysisStartStr = json.path("Analysis_Start_Time").asText("");
    if (!analysisStartStr.isBlank()) {
      try {
        LocalDateTime ldt = LocalDateTime.parse(analysisStartStr, NEXUS_DATE_FORMATTER);
        workflowRun.setStartTime(extractTime(ldt, json.path("timezone").asText()));
      } catch (Exception e) {
        log.warn("Could not parse Analysis_Start_Time '{}' for run {}", analysisStartStr, runId);
      }
    }
    workflowRun.setSoftwareVersion(json.path("AnalysisRecipe").asText(null));

    // Mirror uploadStatus: only COMPLETE and FAILED have explicit terminal states
    if (uploadStatus == UltimaProcessStatus.COMPLETE) {
      workflowRun.complete();
    } else if (uploadStatus == UltimaProcessStatus.FAILED) {
      workflowRun.fail();
    }

    workflowRun.setAnalysisOutputs(buildAnalysisUnits(runId, runFolder));
    return workflowRun;
  }

  private List<UltimaAnalysisUnit> buildAnalysisUnits(String runId, String runFolder) {
    List<UltimaAnalysisUnit> units = new ArrayList<>();
    try {
      for (Blob folder : googleBucketClient.ls(runFolder)) {
        if (!folder.isDirectory()) continue; // no run level files are expected
        String folderName = extractFolderName(folder.getName());

        // Folder name: {runId}-{library}-{barcode}
        int underscoreIdx = folderName.indexOf('-');
        if (underscoreIdx < 0) continue;
        String libBarcode = folderName.substring(underscoreIdx + 1);
        int lastDash = libBarcode.lastIndexOf('-');
        if (lastDash < 0) continue;
        String library = libBarcode.substring(0, lastDash);
        String barcode = libBarcode.substring(lastDash + 1);

        String barcodeFolderPath = folder.getBucket() + "/" + folder.getName();
        UltimaAnalysisUnit unit = new UltimaAnalysisUnit();
        unit.setBarcode(barcode);
        unit.setLibrary(library);
        unit.setFiles(buildAnalysisFiles(barcodeFolderPath));
        units.add(unit);
      }
    } catch (Exception e) {
      log.warn("Failed to list barcode folders for run {} in GCS folder {}", runId, runFolder, e);
    }
    return units;
  }

  private List<AnalysisFile> buildAnalysisFiles(String barcodeFolderPath) {
    List<AnalysisFile> files = new ArrayList<>();
    try {
      for (Blob blob : googleBucketClient.ls(barcodeFolderPath)) {
        if (blob.isDirectory()) continue;
        String blobName = blob.getName();
        // Use CramAnalysisFile for .cram files; MetadataAnalysisFile for everything else
        AnalysisFile file =
            blobName.endsWith(".cram") ? new CramAnalysisFile() : new MetadataAnalysisFile();
        // Path stored as "bucket/object"
        // TODO do we want to be clear that it's GCS aka gs://bucket/object by including "gs://" in
        // the path?
        file.setPath(Path.of(blob.getBucket() + "/" + blobName));
        file.setCrc32Checksum(blob.getCrc32c()); // base64
        file.setSize(blob.getSize());
        if (blob.getCreateTimeOffsetDateTime() != null) {
          file.setCreatedTime(blob.getCreateTimeOffsetDateTime().toInstant());
        }
        if (blob.getUpdateTimeOffsetDateTime() != null) {
          file.setModifiedTime(blob.getUpdateTimeOffsetDateTime().toInstant());
        }
        files.add(file);
      }
    } catch (Exception e) {
      log.warn("Failed to list files in GCS barcode folder {}", barcodeFolderPath, e);
    }
    return files;
  }

  private UltimaProcessStatus sequencingComplete(int pctComplete, int succeeded, String errmsg) {
    if (!errmsg.isEmpty()
        && !errmsg.equals("null")) { // error message is "null" when run is first started
      return UltimaProcessStatus.FAILED;
    } else if (pctComplete == 100) {
      return (succeeded == 1) ? UltimaProcessStatus.COMPLETE : UltimaProcessStatus.FAILED;
    } else if (pctComplete > 0) {
      return UltimaProcessStatus.RUNNING;
    } else {
      return UltimaProcessStatus.PENDING;
    }
  }

  private Instant extractTime(LocalDateTime datetime, String timeZone) {
    try {
      ZoneId zone = TimeZone.getTimeZone(timeZone).toZoneId();
      return datetime.atZone(zone).toInstant();
    } catch (Exception e) {
      log.error("Invalid timezone cannot determine start or completion time.");
      return null;
    }
  }

  private String extractModel(String serialNumber) throws IOException {
    // as per conversation with Ultima, the last 10 characters are always to be removed
    if (!serialNumber.isBlank()) {
      return serialNumber.substring(0, serialNumber.length() - 10);
    } else {
      throw new IOException("Cannot determine container model from serial number: " + serialNumber);
    }
  }

  private HealthType translateStatus(
      UltimaProcessStatus sequencingStatus,
      UltimaProcessStatus analysisStatus,
      UltimaProcessStatus uploadStatus) {
    if (sequencingStatus == UltimaProcessStatus.FAILED
        || analysisStatus == UltimaProcessStatus.FAILED
        || uploadStatus == UltimaProcessStatus.FAILED) {
      return HealthType.FAILED;
    } else if (sequencingStatus == UltimaProcessStatus.COMPLETE
        && analysisStatus == UltimaProcessStatus.COMPLETE
        && uploadStatus == UltimaProcessStatus.COMPLETE) {
      return HealthType.COMPLETED;
    } else if (sequencingStatus == UltimaProcessStatus.RUNNING
        || sequencingStatus == UltimaProcessStatus.PENDING
        || analysisStatus == UltimaProcessStatus.RUNNING
        || uploadStatus == UltimaProcessStatus.RUNNING
        || (sequencingStatus == UltimaProcessStatus.COMPLETE
            && analysisStatus == UltimaProcessStatus.PENDING)
        || (sequencingStatus == UltimaProcessStatus.COMPLETE
            && analysisStatus == UltimaProcessStatus.COMPLETE
            && uploadStatus == UltimaProcessStatus.PENDING)) {
      return HealthType.RUNNING;
    } else {
      return HealthType.UNKNOWN;
    }
  }

  private Instant calculateCompletedDate(Instant startdate, Duration runtime) {
    return startdate.plus(runtime);
  }

  @Override
  public PathType getPathType() {
    return PathType.VIRTUAL;
  }

  private List<String> getPoolsFromSampleDB(String ampSamplePlate) {
    List<JsonNode> pools = new ArrayList<>();
    List<String> poolNames = new ArrayList<>();
    try {
      JsonNode samplePlateNode = apiClient.fetchSampleDB(ampSamplePlate);
      if (samplePlateNode != null
          && samplePlateNode.has("pools")
          && samplePlateNode.path("pools").isArray()) {
        samplePlateNode.path("pools").forEach(pools::add);
      } else {
        log.error("Couldn't parse response from Sample DB, no pool names set");
      }

      pools.forEach(
          node -> {
            if (node.has("libraryPool")) {
              poolNames.add(node.path("libraryPool").asText());
            }
          });
    } catch (IOException e) {
      log.error("Couldn't access Sample DB, no pool names set", e);
    }
    return poolNames;
  }

  private void populateRunFolderCache(String sequencerGcsPath) {
    try {
      List<Blob> entries = googleBucketClient.ls(sequencerGcsPath);
      for (Blob blob : entries) {
        if (!blob.isDirectory()) continue;
        String folderName = extractFolderName(blob.getName());
        // Run folders are named {runId}-{YYYYMMDD}_{suffix}
        int dashIdx = folderName.indexOf('-');
        if (dashIdx < 0)
          continue; // only include folders with run directory formatting used by the Ultima machine
        String runId = folderName.substring(0, dashIdx);
        runFolderCache.put(runId, blob.getBucket() + "/" + blob.getName());
      }
    } catch (Exception e) {
      log.warn("Failed to list run folders from GCS at {}", sequencerGcsPath, e);
    }
  }

  private static String extractFolderName(String blobName) {
    // GCS dir blobs have a trailing '/'. Strip it, then take everything after the last '/' to get
    // the folder name
    String stripped =
        blobName.endsWith("/") ? blobName.substring(0, blobName.length() - 1) : blobName;
    int lastSlash = stripped.lastIndexOf('/');
    return lastSlash >= 0 ? stripped.substring(lastSlash + 1) : stripped;
  }

  private double getControlQ30Bases(String runId) throws IOException {
    double percentBasesQ30 = 0;
    try {
      JsonNode metrics = apiClient.fetchBarcodeMetrics(runId, CONTROL_BARCODE);
      percentBasesQ30 = metrics.path("qtable").path("PCT_PF_Q30_bases").asDouble(0);
    } catch (IOException e) {
      log.error("Couldn't get control barcode metrics (Bases >Q30) for run {}.", runId, e);
    }
    return percentBasesQ30;
  }

  @Override
  public boolean validateParameters(ObjectNode parameters) {
    return parameters.hasNonNull("nexusApiAddress")
        && !parameters.get("nexusApiAddress").asText().isBlank()
        && parameters.hasNonNull("nexusApiTokenFile")
        && new File(parameters.get("nexusApiTokenFile").asText()).canRead()
        && parameters.hasNonNull("googleServiceAccount")
        && !parameters.get("googleServiceAccount").asText().isBlank();
  }
}
