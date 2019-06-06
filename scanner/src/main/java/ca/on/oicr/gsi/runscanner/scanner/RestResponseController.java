package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.ProgressiveRequestDto;
import ca.on.oicr.gsi.runscanner.dto.ProgressiveResponseDto;
import ca.on.oicr.gsi.runscanner.scanner.Scheduler.OutputSizeLimit;
import io.swagger.annotations.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

/** Provide information about the run scanner's current run cache via a REST interface. */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Api(tags = {"Runs"})
public class RestResponseController {
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
  @ApiOperation(
      value = "Get run by name",
      response = ca.on.oicr.gsi.runscanner.dto.NotificationDto.class,
      responseContainer = "ResponseEntity")
  @ApiResponses({
    @ApiResponse(code = 200, message = "Success"),
    @ApiResponse(code = 404, message = "Run not found")
  })
  public ResponseEntity<NotificationDto> getByName(
      @PathVariable("name") @ApiParam(value = "Run name") String id) {
    return scheduler
        .finished()
        .filter(dto -> dto.getRunAlias().equals(id))
        .findAny()
        .map(dto -> new ResponseEntity<>(dto, HttpStatus.OK))
        .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
  }

  /**
   * Given a known run name. get its metrics. If no run is found, null is returned. If there are
   * multiple runs with the same name that are from different sequencers, one is randomly selected.
   */
  @GetMapping("/run/{name}/metrics")
  @ApiOperation(
      value = "Get metrics by run name",
      response = Json[].class,
      responseContainer = "HttpEntity")
  @ApiResponses({@ApiResponse(code = 200, message = "Success")})
  public HttpEntity<byte[]> getMetricsByName(
      @PathVariable("name") @ApiParam(value = "Run name") String id) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON_UTF8)
        .body(
            scheduler
                .finished()
                .filter(dto -> dto.getRunAlias().equals(id))
                .findAny()
                .map(NotificationDto::getMetrics)
                .orElse("[]")
                .getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Provide all the runs current cache as an array.
   *
   * <p>Runs are not guaranteed to have unique names.
   */
  @GetMapping("/runs/all")
  @ApiOperation(
      value = "Get all runs",
      response = ca.on.oicr.gsi.runscanner.dto.NotificationDto.class,
      responseContainer = "List")
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
  @ApiOperation(
      value = "Progressive scan of runs",
      response = ca.on.oicr.gsi.runscanner.dto.ProgressiveResponseDto.class)
  @ResponseBody
  public ProgressiveResponseDto progressive(
      @ApiParam(value = "ProgressiveRequestDto object containing request options") @RequestBody
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
