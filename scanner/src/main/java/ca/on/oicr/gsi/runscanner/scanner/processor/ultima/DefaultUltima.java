package ca.on.oicr.gsi.runscanner.scanner.processor.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.Consumable;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.UltimaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ca.on.oicr.gsi.runscanner.dto.type.PipelineStatus;
import ca.on.oicr.gsi.runscanner.dto.type.UltimaProcessStatus;
import ca.on.oicr.gsi.runscanner.dto.type.WorkflowRunStatus;
import ca.on.oicr.gsi.runscanner.dto.ultima.CramAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaWorkflowRun;
import ca.on.oicr.gsi.runscanner.scanner.processor.PathType;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
  private final UltimaStorageClient storageClient;

  public static DefaultUltima create(Builder builder, ObjectNode parameters) {
    String nexusApiUrl = fetchNexusApiUrl(parameters);
    String nexusApiTokenFile = fetchNexusApiTokenFile(parameters);
    String googleCredentialsFile = fetchGoogleCredentialsFile(parameters);
    if (nexusApiUrl == null || nexusApiTokenFile == null || googleCredentialsFile == null) {
      log.error("Could not create Ultima run processor: missing required configuration");
      return null;
    }

    try {
      boolean useGoogleBucket = fetchUseGoogleBucket(parameters);
      UltimaStorageClient storageClient =
          useGoogleBucket
              ? new GoogleBucketStorageClient(googleCredentialsFile)
              : new LocalStorageClient();
      return new DefaultUltima(
          builder,
          nexusApiUrl,
          nexusApiTokenFile,
          storageClient,
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
      UltimaStorageClient storageClient,
      String apiUrlSampleDB,
      String tokenPathSampleDB)
      throws IOException {
    super(builder);
    this.apiClient =
        new UltimaApiClient(apiUrlNexus, tokenPathNexus, apiUrlSampleDB, tokenPathSampleDB);
    this.storageClient = storageClient;
  }

  public DefaultUltima(
      Builder builder, UltimaApiClient apiClient, UltimaStorageClient storageClient) {
    super(builder);
    this.apiClient = apiClient;
    this.storageClient = storageClient;
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
   * @return String filename where the Google service account key is stored
   */
  private static String fetchGoogleCredentialsFile(ObjectNode parameters) {
    if (parameters.hasNonNull("googleCredentialsFile")) {
      return parameters.get("googleCredentialsFile").asText();
    } else {
      log.error("No Google credentials file configured for Ultima, this config should be invalid");
      return null;
    }
  }

  /**
   * @param parameters ObjectNode
   * @return true if run output should be read from a Google Cloud Storage bucket, false to read
   *     from a local filesystem path. Defaults to true when unset
   */
  private static boolean fetchUseGoogleBucket(ObjectNode parameters) {
    return parameters.path("useGoogleBucket").asBoolean(true);
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
    } catch (IOException e) {
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
    Instant startDate;
    try {
      startDate = extractTime(ldt, zoneString);
    } catch (DateTimeException e) {
      log.error("Invalid timezone cannot determine start or completion time.");
      startDate = null;
    }
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
    addConsumable(consumables, "Amplification Sample Plate Serial Number", ampSamplePlate);
    addConsumable(
        consumables,
        "Amplification Chilled Rack Lot Number",
        json.path("AMP_ChilledRack").asText());
    addConsumable(
        consumables, "Amplification RT Rack Lot Number", json.path("AMP_RTRack").asText());
    addConsumable(
        consumables, "Amplification Tube Array Lot Number", json.path("AMP_TubeArray").asText());
    addConsumable(
        consumables,
        "Amplification Break Container Lot Number",
        json.path("AMP_BreakContainer").asText());
    addConsumable(consumables, "Amplification Wash 1 Lot Number", json.path("AMP_Wash1").asText());
    addConsumable(consumables, "Amplification Wash 2 Lot Number", json.path("AMP_Wash2").asText());
    addConsumable(
        consumables,
        "Amplification Enrichment Bead Lot Number",
        json.path("AMP_EnrichmentBead").asText());
    addConsumable(consumables, "Sequencing Rack Lot Number", json.path("SampleRack").asText());
    addConsumable(consumables, "Sample Tube Lot Number", json.path("SampleTube").asText());
    addConsumable(
        consumables, "Sequencing Cartridge Lot Number", json.path("SequencingCartridge").asText());
    addConsumable(consumables, "Wash Container Lot Number", json.path("WashContainer").asText());
    addConsumable(consumables, "Wafer Serial Number", json.path("Wafer").asText());

    dto.setConsumables(consumables);

    String runFolder = runFolderCache.get(runId);
    dto.setSequencerFolderPath(runFolder);
    // if runFolder is null we expect it either hasn't been uploaded yet (at sequencing step)
    // or it's old and has been removed
    if (runFolder != null) {
      dto.addPipelineRun(createPipelineRun(runId, runFolder, json, uploadStatus));
    }

    return dto;
  }

  /** Skips consumables with no meaningful lot number (missing, empty, or the literal "null"). */
  private static void addConsumable(List<Consumable> consumables, String type, String lotNumber) {
    if (lotNumber != null && !lotNumber.isEmpty() && !lotNumber.equalsIgnoreCase("null")) {
      consumables.add(new Consumable(type, lotNumber));
    }
  }

  /**
   * Builds the pipeline run for this Ultima run. Any failure anywhere in the process (parsing the
   * analysis start time, listing GCS folders/files, etc.) is caught here so it can't silently
   * result in a pipeline run that looks COMPLETE but is actually missing data - instead the whole
   * pipeline run is marked SCAN_ERROR.
   */
  private UltimaPipelineRun createPipelineRun(
      String runId, String runFolder, JsonNode json, UltimaProcessStatus uploadStatus) {
    // Ultima only ever has one attempt per runId.
    UltimaPipelineRun pipelineRun = new UltimaPipelineRun(1);
    try {
      UltimaWorkflowRun workflowRun = buildCramWorkflowRun(runFolder, json, uploadStatus);
      pipelineRun.put(workflowRun);
      pipelineRun.setPipelineStatus(
          workflowRun.getWorkflowRunStatus() == WorkflowRunStatus.PENDING
              ? PipelineStatus.INCOMPLETE
              : PipelineStatus.COMPLETE);
    } catch (DateTimeParseException | IOException e) {
      log.error("Failed to build pipeline run for Ultima run {}", runId, e);
      pipelineRun.setPipelineStatus(PipelineStatus.SCAN_ERROR);
    }
    return pipelineRun;
  }

  private UltimaWorkflowRun buildCramWorkflowRun(
      String runFolder, JsonNode json, UltimaProcessStatus uploadStatus) throws IOException {
    UltimaWorkflowRun workflowRun = new UltimaWorkflowRun("CRAMGeneration");

    // Analysis_Start_Time uses the same "yyyy-MM-dd HH:mm:ss" format as startdatetime
    String analysisStartStr = json.path("Analysis_Start_Time").asText("");
    if (!analysisStartStr.isBlank()) {
      // A malformed Analysis_Start_Time is a real data problem, so let it propagate and mark the
      // pipeline run as SCAN_ERROR. An invalid timezone just means we can't compute a start time.
      LocalDateTime ldt = LocalDateTime.parse(analysisStartStr, NEXUS_DATE_FORMATTER);
      workflowRun.setStartTime(extractTime(ldt, json.path("timezone").asText()));
    }
    workflowRun.setSoftwareVersion(json.path("AnalysisRecipe").asText(null));

    // Mirror uploadStatus: only COMPLETE and FAILED have explicit terminal states
    if (uploadStatus == UltimaProcessStatus.COMPLETE) {
      workflowRun.complete();
    } else if (uploadStatus == UltimaProcessStatus.FAILED) {
      workflowRun.fail();
    }

    workflowRun.setAnalysisOutputs(buildAnalysisUnits(runFolder));
    return workflowRun;
  }

  private List<UltimaAnalysisUnit> buildAnalysisUnits(String runFolder) throws IOException {
    List<UltimaAnalysisUnit> units = new ArrayList<>();
    for (UltimaStorageEntry folder : storageClient.ls(runFolder)) {
      if (!folder.isDirectory()) continue; // no run level files are expected
      String folderName = folder.getName();

      // Folder name: {runId}-{library}-{barcode}
      // Split on the first and last '-', library names can contain dashes
      int underscoreIdx = folderName.indexOf('-');
      if (underscoreIdx < 0) continue;
      String libBarcode = folderName.substring(underscoreIdx + 1);
      int lastDash = libBarcode.lastIndexOf('-');
      if (lastDash < 0) continue;
      String library = libBarcode.substring(0, lastDash);
      String barcode = libBarcode.substring(lastDash + 1);

      UltimaAnalysisUnit unit = new UltimaAnalysisUnit();
      unit.setBarcode(barcode);
      unit.setLibrary(library);
      unit.setFiles(buildAnalysisFiles(folder.getFullPath()));
      units.add(unit);
    }
    return units;
  }

  private List<AnalysisFile> buildAnalysisFiles(String barcodeFolderPath) throws IOException {
    List<AnalysisFile> files = new ArrayList<>();
    for (UltimaStorageEntry entry : storageClient.ls(barcodeFolderPath)) {
      if (entry.isDirectory()) continue;
      // Only cram files are grouped into the CRAMGeneration workflow run for now; other file
      // types have no consumer yet.
      if (!entry.getName().endsWith(".cram")) continue;
      AnalysisFile file = new CramAnalysisFile();
      file.setPath(entry.getPath());
      file.setCrc32Checksum(entry.getCrc32Checksum()); // base64
      file.setSize(entry.getSize());
      file.setCreatedTime(entry.getCreatedTime());
      file.setModifiedTime(entry.getModifiedTime());
      files.add(file);
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
    ZoneId zone = TimeZone.getTimeZone(timeZone).toZoneId();
    return datetime.atZone(zone).toInstant();
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

  private void populateRunFolderCache(String sequencerRootPath) throws IOException {
    List<UltimaStorageEntry> entries = storageClient.ls(sequencerRootPath);
    for (UltimaStorageEntry entry : entries) {
      if (!entry.isDirectory()) continue;
      String folderName = entry.getName();
      // Run folders are named {runId}-{YYYYMMDD}_{suffix}
      int dashIdx = folderName.indexOf('-');
      if (dashIdx < 0)
        continue; // only include folders with run directory formatting used by the Ultima machine
      String runId = folderName.substring(0, dashIdx);
      runFolderCache.put(runId, entry.getFullPath());
    }
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
    boolean baseValid =
        parameters.hasNonNull("nexusApiAddress")
            && !parameters.get("nexusApiAddress").asText().isBlank()
            && parameters.hasNonNull("nexusApiTokenFile")
            && new File(parameters.get("nexusApiTokenFile").asText()).canRead();
    if (!fetchUseGoogleBucket(parameters)) {
      return baseValid;
    }
    return baseValid
        && parameters.hasNonNull("googleCredentialsFile")
        && new File(parameters.get("googleCredentialsFile").asText()).canRead();
  }
}
