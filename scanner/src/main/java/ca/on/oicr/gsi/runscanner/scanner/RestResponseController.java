package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.ProgressiveRequestDto;
import ca.on.oicr.gsi.runscanner.dto.ProgressiveResponseDto;
import ca.on.oicr.gsi.runscanner.scanner.Scheduler.OutputSizeLimit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Provide information about the run scanner's current run cache via a REST interface. */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Runs")
public class RestResponseController {
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  public class ResourceNotFoundException extends RuntimeException {}

  private static final Logger log = LoggerFactory.getLogger(RestResponseController.class);
  private static final LatencyHistogram progressiveLatency =
      new LatencyHistogram(
          "miso_runscanner_progressive_latency",
          "Time to serve a progressive request (in seconds).");
  @Autowired private Scheduler scheduler;

  // We create a token that is effectively random upon initialisation so that we know if the client
  // thinks it's talking to the same instance
  // of the server
  private final long token = System.currentTimeMillis();

  /**
   * Given a known run name. If no run is found, null is returned. If there are multiple runs with
   * the same name that are from different sequencers, one is randomly selected.
   */
  @GetMapping("/run/{name}")
  @Operation(summary = "Get run by name")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
    @ApiResponse(responseCode = "404", description = "Run not found", content = @Content)
  })
  public ResponseEntity<NotificationDto> getByName(
      @PathVariable("name") @Parameter(description = "Run name") String id) {
    return scheduler
        .finished()
        .filter(dto -> dto.getRunAlias().equals(id))
        .findAny()
        .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  @DeleteMapping("/run/{name}")
  @Operation(summary = "Invalidate cache for run by run directory name")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Success"),
    @ApiResponse(responseCode = "404", description = "Run not found")
  })
  @ResponseStatus(code = HttpStatus.NO_CONTENT)
  public void deleteByRunDirectoryName(
      @PathVariable("name") @Parameter(description = "Run directory name") String id) {
    if (!scheduler.invalidate(id)) throw new ResourceNotFoundException();
  }

  /**
   * Given a known run name. get its metrics. If no run is found, null is returned. If there are
   * multiple runs with the same name that are from different sequencers, one is randomly selected.
   */
  @GetMapping("/run/{name}/metrics")
  @Operation(summary = "Get metrics by run name")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
    @ApiResponse(responseCode = "404", description = "Run not found", content = @Content)
  })
  @ResponseBody
  public JsonNode getMetricsByName(
      @PathVariable("name") @Parameter(description = "Run name") String id) throws IOException {
    String response =
        scheduler
            .finished()
            .filter(dto -> dto.getRunAlias().equals(id))
            .findAny()
            .map(NotificationDto::getMetrics)
            .orElse(null);
    if (response == null) throw new ResourceNotFoundException();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readTree(response);
    return node;
  }

  /**
   * Provide all the runs current cache as an array.
   *
   * <p>Runs are not guaranteed to have unique names.
   */
  @GetMapping("/runs/all")
  @Operation(summary = "Get all runs")
  @ResponseBody
  public List<NotificationDto> list() {
    return scheduler.finished().collect(Collectors.toList());
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
   *
   * @param request
   * @return
   */
  @PostMapping("/runs/progressive")
  @Operation(summary = "Progressive scan of runs")
  @ResponseBody
  public ProgressiveResponseDto progressive(
      @Parameter(description = "ProgressiveRequestDto object containing request options")
          @RequestBody
          ProgressiveRequestDto request) {
    ProgressiveResponseDto response = new ProgressiveResponseDto();
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
    return response;
  }
}
