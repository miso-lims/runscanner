package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.scanner.LatencyHistogram;
import com.fasterxml.jackson.databind.JsonNode;
import io.prometheus.metrics.core.metrics.Counter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class UltimaApiClient {

  private static final LatencyHistogram all_run_api_fetch_time =
      new LatencyHistogram(
          "runscanner_ultima_fetch_all_runs_time", "The time to query the NEXUS API for all runs.");

  private static final LatencyHistogram nexus_control_metric_api_fetch_time =
      new LatencyHistogram(
          "runscanner_ultima_fetch_run_metric_time",
          "The time to query the NEXUS API for run metrics.");

  private static final LatencyHistogram sample_db_fetch_time =
      new LatencyHistogram(
          "runscanner_ultima_fetch_sample_db",
          "The time to query the Sample DB API for a single run.");

  private static final Counter api_nexus_fetch_failed =
      Counter.builder()
          .name("runscanner_ultima_nexus_api_failed_request")
          .help("Number of exceptions thrown by the Ultima Nexus API client.")
          .register();

  private static final Counter api_sample_db_fetch_failed =
      Counter.builder()
          .name("runscanner_ultima_sampledb_api_failed_request")
          .help("Number of exceptions thrown by the Ultima Sample DB API client.")
          .register();

  private static final Logger log = LoggerFactory.getLogger(UltimaApiClient.class);

  private final String apiUrlNexus;
  private final RestTemplate restTemplate;
  private final HttpHeaders headersNexus;
  private final String apiUrlSampleDB;
  private final HttpHeaders headersSampleDB;

  public UltimaApiClient(
      String apiUrlNexus, String tokenPathNexus, String apiUrlSampleDB, String tokenPathSampleDB)
      throws IOException {
    this.apiUrlNexus = apiUrlNexus;
    this.restTemplate =
        new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();

    String token = Files.readString(Paths.get(tokenPathNexus)).trim();
    HttpHeaders headersNexus = new HttpHeaders();
    headersNexus.set(HttpHeaders.AUTHORIZATION, token);
    headersNexus.set(HttpHeaders.ACCEPT, "application/json");
    this.headersNexus = headersNexus;

    this.apiUrlSampleDB = apiUrlSampleDB;

    if (tokenPathSampleDB != null) {
      String tokenSampleDB = Files.readString(Paths.get(tokenPathSampleDB)).trim();
      HttpHeaders headersSampleDB = new HttpHeaders();
      headersSampleDB.set(HttpHeaders.AUTHORIZATION, tokenSampleDB);
      headersSampleDB.set(HttpHeaders.ACCEPT, "application/json");
      this.headersSampleDB = headersSampleDB;
    } else {
      this.headersSampleDB = null;
    }
  }

  public List<JsonNode> fetchAllRunSummaries() throws IOException {

    String datetimeRestrictions =
        "from=01-01-1970&to="
            + DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH)
                .format(LocalDateTime.now().plusDays(1));
    String url = String.format("%s/api/data/runsummary?%s", apiUrlNexus, datetimeRestrictions);

    HttpEntity<String> entity = new HttpEntity<>(headersNexus);

    ResponseEntity<JsonNode> response;
    try (AutoCloseable latency_nexus = all_run_api_fetch_time.start()) {
      response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
      if (!response.getStatusCode().is2xxSuccessful()) {
        api_nexus_fetch_failed.inc();
        throw new IllegalStateException(
            "API request to "
                + url
                + " failed, unexpected response status: "
                + response.getStatusCode());
      }
    } catch (Exception e) {
      api_nexus_fetch_failed.inc();
      throw new IOException(e);
    }
    List<JsonNode> nodes = new ArrayList<>();
    if (response.getBody() != null && response.getBody().isArray()) {
      response.getBody().forEach(nodes::add);
    } else {
      api_nexus_fetch_failed.inc();
      throw new IOException(
          "Unexpected JSON response format from API request to "
              + url
              + " : "
              + response.getBody());
    }
    return nodes;
  }

  public JsonNode fetchBarcodeMetrics(String runId, String barcode) throws IOException {

    HttpEntity<String> entity = new HttpEntity<>(headersNexus);
    String url =
        String.format("%s/api/data/metrics/%s/%s?purpose=qtable", apiUrlNexus, runId, barcode);

    ResponseEntity<JsonNode> response;
    try (AutoCloseable latency_nexus_metric = nexus_control_metric_api_fetch_time.start()) {
      response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
      if (!response.getStatusCode().is2xxSuccessful()) {
        api_nexus_fetch_failed.inc();
        throw new IllegalStateException(
            "API request to "
                + url
                + " failed, unexpected response status: "
                + response.getStatusCode());
      }
    } catch (Exception e) {
      api_nexus_fetch_failed.inc();
      throw new IOException(e);
    }
    if (response.getBody() != null) {
      if (response.getBody().isArray()) {
        return response.getBody().get(0); // API returns a list of 1
      } else {
        return response.getBody();
      }
    } else {
      api_nexus_fetch_failed.inc();
      throw new IOException(
          "Unexpected JSON response format from API request to "
              + url
              + " : "
              + response.getBody());
    }
  }

  public JsonNode fetchSampleDB(String samplePlateId) throws IOException {

    String url =
        String.format("%s/api/v2/lib-pools?samplePlateId=%s", apiUrlSampleDB, samplePlateId);

    HttpEntity<String> entity = new HttpEntity<>(headersSampleDB);

    ResponseEntity<JsonNode> response;
    try (AutoCloseable latency_sampledb = sample_db_fetch_time.start()) {
      response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
    } catch (Exception e) {
      api_sample_db_fetch_failed.inc();
      throw new IOException(e);
    }
    return response.getBody();
  }
}
