package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.Histogram;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Periodically scan the run directories and cache the results. */
@Service
public class Scheduler {
  private static final String PLATFORM_LABEL = "platform";

  /** Holder for a run that has been scanned. */
  private static class FinishedWork {
    Instant created = Instant.now();
    NotificationDto dto;
    int epoch;

    /**
     * Determine if the run should be scanned again.
     *
     * <p>This only happens if the run is not marked as done by the processor and 10 minutes have
     * past since the last process. The automatic rerunning done by scheduler is not sufficient to
     * determine if the run needs to be reprocessed since it isn't clear how long the run waited in
     * the processing queue.
     */
    public boolean shouldRerun() {
      return !dto.isDone() && Duration.between(created, Instant.now()).toMinutes() > 10;
    }
  }

  public static class OutputSizeLimit implements Predicate<FinishedWork> {
    private int count;
    private int highestEpoch;
    private final int softLimit;

    public OutputSizeLimit(int softLimit) {
      super();
      this.softLimit = softLimit;
    }

    public int getEpoch() {
      return highestEpoch;
    }

    public boolean hasCapacity() {
      return count < softLimit;
    }

    @Override
    public boolean test(FinishedWork work) {
      if (hasCapacity()) {
        count++;
        highestEpoch = work.epoch;
        return true;
      }
      return work.epoch == highestEpoch;
    }
  }

  public static class SuppliedDirectoryConfig {

    private String name;
    private ObjectNode parameters;
    private String path;
    private Platform platformType;
    private String timeZone;
    private List<File> ignoreSubdirectories;

    public String getName() {
      return name;
    }

    public ObjectNode getParameters() {
      return parameters;
    }

    public String getPath() {
      return path;
    }

    public Platform getPlatformType() {
      return platformType;
    }

    public String getTimeZone() {
      return timeZone;
    }

    public List<File> getIgnoreSubdirectories() {
      return ignoreSubdirectories;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setParameters(ObjectNode parameters) {
      this.parameters = parameters;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public void setPlatformType(Platform platformType) {
      this.platformType = platformType;
    }

    public void setTimeZone(String timeZone) {
      this.timeZone = timeZone;
    }

    public void setIgnoreSubdirectories(ArrayList<File> ignoreSubdirectories) {
      this.ignoreSubdirectories = ignoreSubdirectories;
    }
  }

  private static class UnreadableDirectories implements Predicate<File> {
    private final Set<File> rejects = new HashSet<>();

    public Set<File> getRejects() {
      return rejects;
    }

    @Override
    public boolean test(File directory) {
      boolean result = directory.canRead() && directory.canExecute();
      if (!result) rejects.add(directory);
      return result;
    }
  }

  private static final Gauge acceptedDirectories =
      Gauge.builder()
          .name("miso_runscanner_directories_accepted")
          .help(
              "The number of directories that were readable and sent for processing in the last pass.")
          .register();

  private static final Gauge attemptedDirectories =
      Gauge.builder()
          .name("miso_runscanner_directories_attempted")
          .help("The number of directories that were considered in the last pass.")
          .register();

  private static final Gauge goodRuns =
      Gauge.builder()
          .name("miso_runscanner_good_runs")
          .help("The number of runs that succeeded in processing.")
          .register();

  private static final Gauge badRuns =
      Gauge.builder()
          .name("miso_runscanner_bad_runs")
          .help("The number of runs that failed to process.")
          .register();

  private static final Gauge configurationEntries =
      Gauge.builder()
          .name("miso_runscanner_configuration_entries")
          .help("The number of entries from the last configuration.")
          .register();

  private static final Gauge configurationTimestamp =
      Gauge.builder()
          .name("miso_runscanner_configuration_timestamp")
          .help("The epoch time when the configuration was last read.")
          .register();

  private static final Gauge configurationValid =
      Gauge.builder()
          .name("miso_runscanner_configuration_valid")
          .help("Whether the configuration loaded from disk is valid.")
          .register();

  private static final Gauge epochGauge =
      Gauge.builder()
          .name("miso_runscanner_epoch")
          .help(
              "The current round of processing done for keeping the client in sync when progressively scanning.")
          .register();

  private static final Counter errors =
      Counter.builder()
          .name("miso_runscanner_errors")
          .help("The number of bad directories encountered.")
          .labelNames(PLATFORM_LABEL)
          .register();

  private static Logger log = LoggerFactory.getLogger(Scheduler.class);

  private static final Gauge newRunsScanned =
      Gauge.builder()
          .name("miso_runscanner_new_runs_scanned")
          .help("The number of runs discovered in the last pass.")
          .register();

  private static final Gauge processingRuns =
      Gauge.builder()
          .name("miso_runscanner_processing_runs")
          .labelNames(PLATFORM_LABEL)
          .help("The number of runs currently being processed.")
          .register();

  private static final Histogram processTime =
      Histogram.builder()
          .classicUpperBounds(1, 5, 10, 30, 60, 300, 600, 3600)
          .name("miso_runscanner_directory_process_time")
          .help("Time to process a run directories in seconds.")
          .labelNames(PLATFORM_LABEL, "instrument")
          .register();

  private static final Counter reentered =
      Counter.builder()
          .name("miso_runscanner_reentered")
          .help("The number of times the scanner was already running while scheduled to run again.")
          .register();

  private static final LatencyHistogram scanTime =
      new LatencyHistogram(
          "miso_runscanner_directory_scan_time", "Time to scan the run directories in seconds.");

  private static final Gauge waitingRuns =
      Gauge.builder()
          .name("miso_runscanner_waiting_runs")
          .help("The number of runs waiting to be processed.")
          .labelNames(PLATFORM_LABEL)
          .register();

  private static final Gauge lastScanStartTime =
      Gauge.builder()
          .name("miso_runscanner_last_scan_start_time_seconds")
          .help("start time of last scan.")
          .register();

  private static final Gauge loadingRunDirectoryValid =
      Gauge.builder()
          .name("miso_runscanner_run_directory_valid")
          .help("The current state of run directories displaying if they are valid or not.")
          .labelNames("directory")
          .register();

  private File configurationFile;

  private Instant configurationLastRead = Instant.now();

  private final AtomicInteger epoch = new AtomicInteger();

  // The paths that threw an exception while processing.
  private final Map<File, Instant> failed = new ConcurrentHashMap<>();

  // The paths for which we have a notification to send.
  private final Map<File, FinishedWork> finishedWork = new ConcurrentHashMap<>();

  private boolean isConfigurationGood = true;

  // The paths that are currently being processed (and the corresponding processor).
  private final Set<File> processing = new ConcurrentSkipListSet<>();

  // The directories that contain run directories that need to be scanned and the processors for
  // those runs.
  private List<Configuration> roots = Collections.emptyList();

  private ScheduledFuture<?> scanDirectoriesFuture = null;

  private Instant scanLastStarted = null;

  private boolean scanningNow = false;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private UnreadableDirectories unreadableDirectories;
  private final ExecutorService workPool = Executors.newWorkStealingPool();

  // The paths that need to be processed (and the corresponding processor).
  private final Set<File> workToDo = new ConcurrentSkipListSet<>();

  public Stream<NotificationDto> finished() {
    return finished(0, x -> true);
  }

  public Stream<NotificationDto> finished(long epoch, Predicate<FinishedWork> filter) {
    return finishedWork
        .values()
        .stream()
        .filter(x -> x.epoch >= epoch)
        .sorted((a, b) -> a.epoch - b.epoch)
        .filter(filter)
        .map(x -> x.dto);
  }

  public Stream<Configuration> getConfiguration() {
    return roots.stream();
  }

  public Instant getConfigurationLastRead() {
    return configurationLastRead;
  }

  public Set<File> getCurrentWork() {
    return processing;
  }

  public int getEpoch() {
    return epoch.get();
  }

  public Set<File> getFailedDirectories() {
    return failed.keySet();
  }

  public Set<File> getFinishedDirectories() {
    return finishedWork.keySet();
  }

  public Set<File> getRoots() {
    return roots.stream().map(Configuration::getPath).collect(Collectors.toSet());
  }

  public Instant getScanLastStarted() {
    return scanLastStarted;
  }

  public Set<File> getScheduledWork() {
    return workToDo;
  }

  public Set<File> getFSUnreadableDirectories() {
    Set<File> unreadables;

    if (getScanLastStarted() != null) {
      unreadables = unreadableDirectories.getRejects();
    } else {
      unreadables = Collections.emptySet();
    }

    return unreadables;
  }

  public boolean isConfigurationGood() {
    return isConfigurationGood;
  }

  public boolean isScanningEnabled() {
    return scanDirectoriesFuture != null;
  }

  public boolean isScanningNow() {
    return scanningNow;
  }

  public boolean invalidate(String runName) {
    return finishedWork
        .keySet()
        .stream()
        .filter(file -> file.getName().equals(runName))
        .map(finishedWork::remove)
        .anyMatch(Objects::nonNull);
  }

  private static boolean isSubDirectory(File baseDirectory, File subDirectory) {
    File parent = subDirectory.getParentFile();
    while (parent != null) {
      if (baseDirectory.equals(parent)) {
        return true;
      }
      parent = parent.getParentFile();
    }
    return false;
  }

  /** Helper to check if current run directory should be ignored */
  protected static boolean skipSubDirectory(
      File currentRunDirectory, List<File> ignoreDirectories, File baseDirectory) {
    // Loop through list of directories to ignore
    for (File currentIgnoreDirectory : ignoreDirectories) {
      File basePlusSubDirectory = new File(baseDirectory, currentIgnoreDirectory.getPath());

      // Check if current directory is same as ignore directory
      if (currentRunDirectory.equals(basePlusSubDirectory)) {
        return true;
      }

      // Check if current directory is subdirectory of ignore directory
      if (isSubDirectory(basePlusSubDirectory, currentRunDirectory)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine if a run directory is in need of processing.
   *
   * <p>This means that it is not in a processing queue, failed processing last time, not in the
   * list of subdirectories to ignore nor needs reprocessing (for runs still active on the
   * sequencer)
   */
  private boolean isUnprocessed(File directory, List<File> ignoreDirectories, File baseDirectory) {
    return !workToDo.contains(directory)
        && !processing.contains(directory)
        && (!failed.containsKey(directory)
            || Duration.between(failed.get(directory), Instant.now()).toMinutes() > 20)
        && (!finishedWork.containsKey(directory) || finishedWork.get(directory).shouldRerun())
        // Exclude from processing if directory name in list of directories to ignore,
        // or it is a sub-directory of an ignore directory
        && !skipSubDirectory(directory, ignoreDirectories, baseDirectory);
  }

  /** Push a run directory into the processing queue. */
  private void queueDirectory(
      final File directory, final RunProcessor processor, final TimeZone tz) {
    workToDo.add(directory);
    waitingRuns.labelValues(processor.getPlatformType().name()).inc();
    workPool.submit(
        () -> {
          Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
          processing.add(directory);
          processingRuns.labelValues(processor.getPlatformType().name()).inc();
          workToDo.remove(directory);
          waitingRuns.labelValues(processor.getPlatformType().name()).dec();

          long runStartTime = System.nanoTime();
          String instrumentName = "unknown";
          try {
            NotificationDto dto = processor.process(directory, tz);
            if (!isStringBlankOrNull(dto.getSequencerName())) {
              instrumentName = dto.getSequencerName();
            }
            FinishedWork work = new FinishedWork();
            work.dto = dto;
            work.epoch = epoch.incrementAndGet();
            finishedWork.put(directory, work);
            failed.remove(directory);
            epochGauge.set(work.epoch);
          } catch (Exception e) {
            log.error("Failed to process run: " + directory.getPath(), e);
            errors.labelValues(processor.getPlatformType().name()).inc();
            failed.put(directory, Instant.now());
          }
          goodRuns.set(finishedWork.size());
          badRuns.set(failed.size());
          processTime
              .labelValues(processor.getPlatformType().name(), instrumentName)
              .observe((System.nanoTime() - runStartTime) / 1e9);
          processing.remove(directory);
          processingRuns.labelValues(processor.getPlatformType().name()).dec();
        });
  }

  /**
   * Rebuild the set of sequencer directories to scan from the configuration file.
   *
   * <p>If the configuration file is unreadable or contains no entries, the configuration is bad.
   * The configuration file may still contain defective/invalid entries and those directories will
   * not be scanned.
   *
   * <p>Changing the configuration does not clear the cache. So if a sequencer's configuration is
   * changed from valid to invalid to valid again, it will not trigger re-processing of the previous
   * output, even if the timezone or processor is changed.
   */
  private void readConfiguration() {
    ObjectMapper mapper = new ObjectMapper();
    configurationLastRead = Instant.now();
    configurationTimestamp.set(configurationLastRead.getEpochSecond());
    try {
      roots =
          Arrays.stream(mapper.readValue(configurationFile, SuppliedDirectoryConfig[].class))
              .map(
                  source -> {
                    Configuration destination = new Configuration();
                    destination.setPath(new File(source.getPath()));
                    destination.setTimeZone(TimeZone.getTimeZone(source.getTimeZone()));
                    destination.setProcessor(
                        RunProcessor.processorFor(
                                source.getPlatformType(), source.getName(), source.getParameters())
                            .orElse(null));
                    destination.setIgnoreSubdirectories(source.getIgnoreSubdirectories());
                    // Create gauge metric to inform us if directory is valid or not
                    loadingRunDirectoryValid
                        .labelValues(source.getPath())
                        .set(destination.isValid() ? 1 : 0);

                    return destination;
                  })
              .collect(Collectors.toList());
      configurationEntries.set(roots.size());
      isConfigurationGood = !roots.isEmpty();
    } catch (IOException e) {
      log.error("Configuration is bad.", e);
      isConfigurationGood = false;
    }
    configurationValid.set(isConfigurationGood ? 1 : 0);
  }

  @Value("${runscanner.configFile}")
  public void setConfigurationFile(String filename) {
    configurationFile = new File(filename);
    readConfiguration();
  }

  /** Initiate scanning every 15 minutes until stopped. */
  public synchronized void start() {
    if (scanDirectoriesFuture == null) {
      scanDirectoriesFuture =
          scheduler.scheduleWithFixedDelay(
              () -> {
                if (scanningNow) {
                  reentered.inc();
                  return;
                }
                scanningNow = true;
                if (configurationFile != null
                    && configurationFile.exists()
                    && configurationFile.lastModified() > configurationLastRead.getEpochSecond()) {
                  readConfiguration();
                }
                scanLastStarted = Instant.now();
                lastScanStartTime.set(System.currentTimeMillis());
                UnreadableDirectories newUnreadableDirectories = new UnreadableDirectories();
                try (StreamCountSpy<Pair<File, Configuration>> newRuns =
                        new StreamCountSpy<>(newRunsScanned);
                    StreamCountSpy<Pair<File, Configuration>> attempted =
                        new StreamCountSpy<>(attemptedDirectories);
                    StreamCountSpy<Pair<File, Configuration>> accepted =
                        new StreamCountSpy<>(acceptedDirectories);
                    AutoCloseable timer = scanTime.start()) {
                  roots
                      .stream() //
                      .filter(Configuration::isValid) //
                      .flatMap(Configuration::getRuns) //
                      .peek(attempted) //
                      .filter(entry -> newUnreadableDirectories.test(entry.first())) //
                      .peek(accepted) //
                      .filter(
                          entry -> {
                            return isUnprocessed(
                                entry.first(),
                                entry.second().getIgnoreSubdirectories(),
                                entry.second().getPath());
                          }) //
                      .peek(newRuns) //
                      .forEach(
                          entry ->
                              queueDirectory(
                                  entry.first(),
                                  entry.second().getProcessor(),
                                  entry.second().getTimeZone()));
                } catch (Exception e) {
                  log.error("Error scanning directory.", e);
                }
                unreadableDirectories = newUnreadableDirectories;
                scanningNow = false;
              },
              1,
              15,
              TimeUnit.MINUTES);
    }
  }

  /** Stop scanning. Queued run directories will still be processed. */
  public synchronized void stop() {
    if (scanDirectoriesFuture != null) {
      scanDirectoriesFuture.cancel(false);
      scanDirectoriesFuture = null;
    }
    workPool.shutdownNow();
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
}
