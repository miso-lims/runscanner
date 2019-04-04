package ca.on.oicr.gsi.runscanner.server.util;

import ca.on.oicr.gsi.runscanner.dto.ProgressiveRequestDto;
import ca.on.oicr.gsi.runscanner.dto.ProgressiveResponseDto;
import ca.on.oicr.gsi.runscanner.server.util.Scheduler.OutputSizeLimit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.Headers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provide information about the run server's current run cache via a REST interface. */
public class RestController {
  private static final Logger log = LoggerFactory.getLogger(RestController.class);
  private static final LatencyHistogram progressiveLatency =
      new LatencyHistogram(
          "miso_runscanner_progressive_latency",
          "Time to serve a progressive request (in seconds).");
  private final Scheduler scheduler;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  static {
    MAPPER.registerModule(new JavaTimeModule()).setDateFormat(new StdDateFormat());
  }

  // We create a token that is effectively random upon initialisation so that we know if the client
  // thinks it's talking to the same instance
  // of the server
  private final long token = System.currentTimeMillis();

  public RestController(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  /**
   * Given a known run name. If no run is found, null is returned. If there are multiple runs with
   * the same name that are from different sequencers, one is randomly selected.
   */
  public void getByName(String id, HttpServerExchange exchange) throws Exception {
    scheduler
        .finished()
        .filter(dto -> dto.getRunAlias().equals(id))
        .findAny()
        .<HttpHandler>map(
            run ->
                ex -> {
                  ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                  try (final var output = ex.getOutputStream()) {
                    MAPPER.writeValue(output, run);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
        .orElse(ResponseCodeHandler.HANDLE_404)
        .handleRequest(exchange);
  }

  /**
   * Given a known run name. get its metrics. If no run is found, null is returned. If there are
   * multiple runs with the same name that are from different sequencers, one is randomly selected.
   */
  public void getMetricsByName(String id, HttpServerExchange exchange) throws Exception {
    scheduler
        .finished()
        .filter(dto -> dto.getRunAlias().equals(id))
        .findAny()
        .<HttpHandler>map(
            run ->
                ex -> {
                  ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                  try (final var output = ex.getOutputStream()) {
                    output.write(run.getMetrics().getBytes(StandardCharsets.UTF_8));
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
        .orElse(ResponseCodeHandler.HANDLE_404)
        .handleRequest(exchange);
  }

  /**
   * Provide all the runs current cache as an array.
   *
   * <p>Runs are not guaranteed to have unique names.
   */
  public void list(HttpServerExchange exchange) throws IOException {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    try (final var output = exchange.getOutputStream()) {
      MAPPER.writeValue(output, scheduler.finished().collect(Collectors.toList()));
    }
  }

  /**
   * Send a progressive scan of results to the client.
   *
   * <p>This uses two pieces of information: a token and an epoch.
   *
   * <p>The purpose of the token is to identify ourselves. Since this service might be restarted
   * between the requests, the token identifies this instance of the server for the life time of its
   * run. If the token doesn't match, we send the client all the data we know about and give them
   * the new token.
   *
   * <p>We also need to track time. Rather than keep track of wall time, we use an incrementing
   * counter (epoch) that we increment whenever we finish processing a run. If the client sends us a
   * valid token, we will tell them the new epoch and give them only the work done since the last
   * epoch.
   */
  public void progressive(HttpServerExchange exchange) throws IOException {
    final ProgressiveRequestDto request;
    try (final var input = exchange.getInputStream()) {
      request = MAPPER.readValue(input, ProgressiveRequestDto.class);
    }

    final var response = new ProgressiveResponseDto();
    response.setToken(token);
    int requestedEpoch = request.getToken() == token ? request.getEpoch() : 0;
    try (AutoCloseable timer = progressiveLatency.start()) {
      Scheduler.OutputSizeLimit limit = new OutputSizeLimit(Math.min(request.getLimit(), 500));
      response.setUpdates(scheduler.finished(requestedEpoch, limit).collect(Collectors.toList()));
      response.setMoreAvailable(!limit.hasCapacity());
      response.setEpoch(limit.getEpoch());
    } catch (Exception e) {
      log.error("Error during progressive run", e);
      response.setUpdates(Collections.emptyList());
      response.setEpoch(requestedEpoch);
    }
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    try (final var output = exchange.getOutputStream()) {
      MAPPER.writeValue(output, response);
    }
  }
}
