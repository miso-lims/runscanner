package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.Consumable;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.UltimaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ca.on.oicr.gsi.runscanner.dto.type.UltimaProcessStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultUltima extends RunProcessor {

  // Cache to store API results during a single scan cycle
  protected final Map<String, JsonNode> runCache = new ConcurrentHashMap<>();

  DateTimeFormatter NEXUS_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final Logger log = LoggerFactory.getLogger(DefaultUltima.class);
  private final UltimaApiClient apiClient;

  public static DefaultUltima create(Builder builder, ObjectNode parameters) {
    try {
      return new DefaultUltima(
          builder,
          fetchNexusApiUrl(parameters),
          fetchNexusApiTokenFile(parameters),
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
      String apiUrlSampleDB,
      String tokenPathSampleDB)
      throws IOException {
    super(builder);
    this.apiClient =
        new UltimaApiClient(apiUrlNexus, tokenPathNexus, apiUrlSampleDB, tokenPathSampleDB);
  }

  protected DefaultUltima(Builder builder, UltimaApiClient apiClient) {
    super(builder);
    this.apiClient = apiClient;
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

      return allRunInfo.stream()
          .map(
              node -> {
                String runId = node.path("runid").asText();

                // Skip if runId is null, empty, or just whitespace
                if (runId == null || runId.trim().isEmpty()) {
                  return null;
                }

                runCache.put(runId, node); // Cache for the process() step

                // TODO eventually will be google bucket path with runID in subfolder name
                return new File(root, runId);
              })
          .filter(Objects::nonNull);
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

    values
        .addObject()
        .put("name", "Total Beads (M)")
        .put(
            "value",
            new BigDecimal(numBeads).toPlainString()); // Given in (M), decimal points not needed
    values
        .addObject()
        .put("name", "Output Reads")
        .put("value", json.path("pf_output_reads").asText("0"));
    values
        .addObject()
        .put("name", "Pass Filter %")
        .put("value", String.format("%.2f", passFilterPercent));
    values
        .addObject()
        .put("name", "Indel Rate TT %")
        .put("value", json.path("indel_rate").asText());
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

    return dto;
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
    // Example: FC12.2T0123456789 -> FTC12.2T
    Pattern pattern = Pattern.compile("^(.{4}\\..{2})");
    Matcher matcher = pattern.matcher(serialNumber);
    if (matcher.find()) {
      // Group 1 contains the extracted text
      return matcher.group(1);
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
          && samplePlateNode.get("pools").isArray()) {
        samplePlateNode.get("pools").forEach(pools::add);
      } else {
        log.error("Couldn't parse response from Sample DB, no pool names set");
      }

      pools.forEach(
          node -> {
            if (node.has("libraryPool")) {
              poolNames.add(node.get("libraryPool").asText());
            }
          });
    } catch (IOException e) {
      log.error("Couldn't access Sample DB, no pool names set", e);
    }
    return poolNames;
  }

  @Override
  public boolean validateParameters(ObjectNode parameters) {
    return parameters.hasNonNull("nexusApiAddress")
        && !parameters.get("nexusApiAddress").asText().isBlank()
        && parameters.hasNonNull("nexusApiTokenFile")
        && new File(parameters.get("nexusApiTokenFile").asText()).canRead();
  }
}
