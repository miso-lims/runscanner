package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;

/** Scan PacBio runs from a directory. The address */
public class DefaultPacBio extends RunProcessor {

  /** Extract data from an XML metadata file and put it in the DTO. */
  interface ProcessMetadata {
    public void accept(Document document, PacBioNotificationDto dto, TimeZone timeZone)
        throws XPathException;
  }

  /**
   * This is the response object provided by the PacBio web service when queries about the state of
   * a plate.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StatusResponse {

    private String customState;
    private String status;

    public String getCustomState() {
      return customState;
    }

    public String getStatus() {
      return status;
    }

    @JsonProperty("CustomState")
    public void setCustomState(String customState) {
      this.customState = customState;
    }

    @JsonProperty("Status")
    public void setStatus(String status) {
      this.status = status;
    }

    public HealthType translateStatus() {
      if (status == null) {
        return HealthType.UNKNOWN;
      }
      switch (status) {
        case "Running":
          return HealthType.RUNNING;
        case "Aborted":
          return HealthType.STOPPED;
        case "Failed":
          return HealthType.FAILED;
        case "Complete":
          return HealthType.COMPLETED;
        default:
          return HealthType.UNKNOWN;
      }
    }
  }

  private static final Predicate<String> CELL_DIRECTORY =
      Pattern.compile("[A-Z]{1}[0-9]{2}_[0-9]{1}").asPredicate();

  private static final HttpComponentsClientHttpRequestFactory HTTP_REQUEST_FACTORY;

  private static final Pattern LINES = Pattern.compile("\\r?\\n");

  private static final Logger log = LoggerFactory.getLogger(DefaultPacBio.class);

  /** These are all the things that can be extracted from the PacBio metadata XML file. */
  private static final ProcessMetadata[] METADATA_PROCESSORS =
      new ProcessMetadata[] {
        processString("//Run/Name", PacBioNotificationDto::setRunAlias),
        processString("//InstrumentName", PacBioNotificationDto::setSequencerName),
        processString("//InstCtrlVer", PacBioNotificationDto::setSoftware),
        processString("//Sample/PlateId", PacBioNotificationDto::setContainerSerialNumber),
        processDate("//Run/WhenStarted", PacBioNotificationDto::setStartDate),
        processNumber(
            "//Movie/DurationInSec",
            (dto, duration) -> {
              Instant start = dto.getStartDate();
              if (start == null) {
                return;
              }
              dto.setCompletionDate(start.plusSeconds(duration.longValue()));
            }),
        processSampleInformation()
      };

  private static final Pattern RUN_DIRECTORY = Pattern.compile("^.+_\\d+$");

  private static final DateTimeFormatter URL_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private static final Pattern WELL_LINE = Pattern.compile("^([A-Z]\\d+),.*$");

  static {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setDefaultSocketConfig(
        SocketConfig.custom().setSoTimeout(20, TimeUnit.SECONDS).build());
    connectionManager.setDefaultConnectionConfig(
        ConnectionConfig.custom().setConnectTimeout(20, TimeUnit.SECONDS).build());
    HttpClient httpClient =
        HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(
                RequestConfig.custom().setConnectionRequestTimeout(20, TimeUnit.SECONDS).build())
            .build();
    HTTP_REQUEST_FACTORY = new HttpComponentsClientHttpRequestFactory(httpClient);
  }

  public static DefaultPacBio create(Builder builder, ObjectNode parameters) {
    JsonNode address = parameters.get("address");
    return address.isTextual()
        ? new DefaultPacBio(builder, address.textValue().replaceAll("/+$", ""))
        : null;
  }

  /**
   * Extract a PacBio-formatted string from the metadata file and put the parsed result into the
   * DTO.
   *
   * @param expression the XPath expression yielding the date
   * @param setter the writer for the date
   */
  private static ProcessMetadata processDate(
      String expression, BiConsumer<PacBioNotificationDto, Instant> setter) {
    XPathExpression expr = RunProcessor.compileXPath(expression)[0];
    return (document, dto, timeZone) -> {
      String result = (String) expr.evaluate(document, XPathConstants.STRING);
      if (result != null) {
        setter.accept(
            dto,
            LocalDateTime.parse(result)
                .toInstant(timeZone.toZoneId().getRules().getOffset(LocalDateTime.parse(result))));
      }
    };
  }

  /**
   * Extract a number from the metadata file and put the result into the DTO.
   *
   * @param expression the XPath expression yielding the number
   * @param setter the writer for the number
   * @return
   */
  private static ProcessMetadata processNumber(
      String expression, BiConsumer<PacBioNotificationDto, Double> setter) {
    XPathExpression expr = RunProcessor.compileXPath(expression)[0];
    return (document, dto, timeZone) -> {
      Double result = (Double) expr.evaluate(document, XPathConstants.NUMBER);
      if (result != null) {
        setter.accept(dto, result);
      }
    };
  }

  /**
   * Create a metadata processor to populate the map of wellname → poolbarcodes in the DTO.
   *
   * @return
   */
  private static ProcessMetadata processSampleInformation() {
    XPathExpression[] expr = RunProcessor.compileXPath("//Sample/WellName", "//Sample/Name");
    return (document, dto, timeZone) -> {
      String well = (String) expr[0].evaluate(document, XPathConstants.STRING);
      String name = (String) expr[1].evaluate(document, XPathConstants.STRING);
      if (isStringBlankOrNull(name) || isStringBlankOrNull(well)) {
        return;
      }
      Map<String, String> poolInfo = dto.getPoolNames();
      if (poolInfo == null) {
        poolInfo = new HashMap<>();
        dto.setPoolNames(poolInfo);
      } else if (poolInfo.containsKey(well)) {
        // If there are multiple things assigned to this well in the sample sheet, then
        // MISO will
        // not be able to figure out a single pool to
        // assign to this well. In this case, we set the pool to be the empty string so
        // that nothing
        // will be automatically assigned.
        log.warn(
            String.format(
                "Multiple pools in well %s on run %s; abandoning automatic pool assignment",
                well, dto.getRunAlias()));
        name = "";
      }
      poolInfo.put(well, name);
    };
  }

  /**
   * Extract a string expression from the metadata file and write it into the DTO.
   *
   * @param expression the XPath expression yielding the string
   * @param setter writer for the string
   * @return
   */
  private static ProcessMetadata processString(
      String expression, BiConsumer<PacBioNotificationDto, String> setter) {
    XPathExpression expr = RunProcessor.compileXPath(expression)[0];
    return (document, dto, timeZone) -> {
      String result = (String) expr.evaluate(document, XPathConstants.STRING);
      if (result != null) {
        setter.accept(dto, result);
      }
    };
  }

  private final String address;

  public DefaultPacBio(Builder builder, String address) {
    super(builder);
    this.address = address;
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    return Arrays.stream(
        root.listFiles(f -> f.isDirectory() && RUN_DIRECTORY.matcher(f.getName()).matches()));
  }

  protected String getSampleSheet(String url) {
    return new RestTemplate(HTTP_REQUEST_FACTORY).getForObject(url, String.class);
  }

  protected StatusResponse getStatus(String url) {
    return new RestTemplate(HTTP_REQUEST_FACTORY).getForObject(url, StatusResponse.class);
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    // We create one DTO for a run, but there are going to be many wells with
    // independent and
    // duplicate metadata that we will simply
    // overwrite in the shared DTO. If the data differs, the last well wins.
    PacBioNotificationDto dto = new PacBioNotificationDto();
    dto.setPairedEndRun(false);
    dto.setSequencerFolderPath(runDirectory.getAbsolutePath());
    // This will be incremented during the metadata scan
    dto.setLaneCount(0);
    // Read all the metadata files and write their results into the DTO.
    Arrays.stream(
            runDirectory.listFiles(
                cellDirectory ->
                    cellDirectory.isDirectory() && CELL_DIRECTORY.test(cellDirectory.getName())))
        .flatMap(
            cellDirectory ->
                Arrays.stream(
                    cellDirectory.listFiles(file -> file.getName().endsWith(".metadata.xml"))))
        .map(RunProcessor::parseXml)
        .filter(Optional::isPresent)
        .forEach(metadata -> processMetadata(metadata.get(), dto, tz));

    // The current job state is not available from the metadata files, so contact the PacBio
    // instrument's web service.
    String plateUrl = dto.getContainerSerialNumber();
    dto.setHealthType(
        getStatus(String.format("%s/Jobs/Plate/%s/Status", address, plateUrl)).translateStatus());
    // If the metadata gave us a completion date, but the web service told us the run isn't
    // complete, delete the completion date of lies.
    if (!dto.getHealthType().isDone()) {
      dto.setCompletionDate(null);
    }

    String sampleSheet = getSampleSheet(String.format("%s/SampleSheet/%s", address, plateUrl));
    dto.setLaneCount(
        (int)
            LINES
                .splitAsStream(sampleSheet)
                .map(WELL_LINE::matcher)
                .filter(Matcher::matches)
                .map(m -> m.group(1))
                .distinct()
                .count());

    ObjectMapper mapper = createObjectMapper();
    ArrayNode metrics = mapper.createArrayNode();

    try {
      ObjectNode dashboardMetric = metrics.addObject();
      dashboardMetric.put("type", "link");
      dashboardMetric.put("name", "PacBio Dashboard");
      URIBuilder builder = new URIBuilder(address + "/Metrics/RSRunReport");
      builder.addParameter("instrument", dto.getSequencerName());
      builder.addParameter("run", dto.getRunAlias());
      builder.addParameter(
          "from",
          dto.getStartDate()
              .atZone(tz.toZoneId())
              .truncatedTo(ChronoUnit.DAYS)
              .format(URL_DATE_FORMAT));
      if (dto.getHealthType().isDone()) {
        builder.addParameter(
            "to",
            dto.getCompletionDate()
                .atZone(tz.toZoneId())
                .plusDays(1)
                .truncatedTo(ChronoUnit.DAYS)
                .format(URL_DATE_FORMAT));
      } else {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime maxRunTime =
            LocalDateTime.ofInstant(dto.getStartDate(), tz.toZoneId()).plusDays(7);
        builder.addParameter(
            "to",
            (today.isBefore(maxRunTime) ? today : maxRunTime)
                .truncatedTo(ChronoUnit.DAYS)
                .format(URL_DATE_FORMAT));
      }
      dashboardMetric.put("href", builder.build().toASCIIString());
    } catch (URISyntaxException e) {
      throw new IOException(e);
    }

    dto.setMetrics(mapper.writeValueAsString(metrics));

    return dto;
  }

  /**
   * Parse a metadata XML file and put all the relevant data into the DTO.
   *
   * @param metadata the path to the XML file
   * @param dto the DTO to update
   */
  private void processMetadata(Document metadata, PacBioNotificationDto dto, TimeZone timeZone) {
    for (ProcessMetadata processor : METADATA_PROCESSORS) {
      try {
        processor.accept(metadata, dto, timeZone);
      } catch (XPathException e) {
        log.error("Failed to extract metadata", e);
      }
    }
  }

  /**
   * Tests whether a String is blank (empty or just spaces) or null. Duplicated from MISO's
   * LimsUtils.
   *
   * @param s String to test for blank or null
   * @return true if blank or null String provided
   */
  private static boolean isStringBlankOrNull(String s) {
    return s == null || "".equals(s.trim());
  }

  @Override
  public PathType getPathType() {
    return PathType.DIRECTORY;
  }
}
