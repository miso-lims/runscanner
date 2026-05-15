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

  private static final Counter api_fetch_failed =
      Counter.builder()
          .name("runscanner_ultima_api_failed_request")
          .help("Number of exceptions thrown by the Ultima API client.")
          .register();

  private final String apiUrl;
  private final RestTemplate restTemplate;
  private final HttpHeaders headers;

  public UltimaApiClient(String apiUrl, String tokenPath) throws IOException {
    this.apiUrl = apiUrl;
    this.restTemplate =
        new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(5))
            .build();

    String token = Files.readString(Paths.get(tokenPath)).trim();
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, token);
    headers.set(HttpHeaders.ACCEPT, "application/json");
    this.headers = headers;
  }

  public List<JsonNode> fetchAllRunSummaries() throws IOException {

    String datetimeRestrictions =
        "from=01-01-1970&to="
            + DateTimeFormatter.ofPattern("MM-dd-yyyy", Locale.ENGLISH)
                .format(LocalDateTime.now().plusDays(1));
    String url = String.format("%s/api/data/runsummary?%s", apiUrl, datetimeRestrictions);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<JsonNode> response;
    try (AutoCloseable latency = all_run_api_fetch_time.start()) {
      response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

    } catch (Exception e) {
      api_fetch_failed.inc();
      throw new IOException(e);
    }
    List<JsonNode> nodes = new ArrayList<>();
    if (!response.getStatusCode().is2xxSuccessful()) {
      api_fetch_failed.inc();
      throw new IOException("API request failed: " + response.getStatusCode());
    }
    if (response.getBody() != null && response.getBody().isArray()) {
      response.getBody().forEach(nodes::add);
    } else {
      api_fetch_failed.inc();
      throw new IOException(
          "Unexpected JSON response format from API request: " + response.getBody());
    }
    return nodes;
  }
}
