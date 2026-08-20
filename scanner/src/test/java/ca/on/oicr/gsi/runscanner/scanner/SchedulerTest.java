package ca.on.oicr.gsi.runscanner.scanner;

import static org.junit.Assert.*;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.scanner.processor.PathType;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;

public class SchedulerTest {

  private final List<Scheduler> schedulersToStop = new ArrayList<>();

  @After
  public void stopSchedulers() {
    schedulersToStop.forEach(Scheduler::stop);
  }

  private static void configureForTest(Configuration configuration, RunProcessor processor) {
    configuration.setProcessor(processor);
    configuration.setTimeZone(TimeZone.getTimeZone("UTC"));
    configuration.setIgnoreSubdirectories(Collections.emptyList());
  }

  private static RunProcessor recordingProcessor(
      List<String> executionOrder, CountDownLatch started) {
    return new RunProcessor(
        new RunProcessor.Builder(Platform.ILLUMINA, "test-recording", (b, p) -> null)) {
      @Override
      public Stream<File> getRunsFromRoot(File root) {
        return Stream.empty();
      }

      @Override
      public PathType getPathType() {
        return PathType.DIRECTORY;
      }

      @Override
      public NotificationDto process(File runDirectory, TimeZone tz) {
        executionOrder.add(runDirectory.getName());
        started.countDown();
        return new NotificationDto() {
          @Override
          public Platform getPlatformType() {
            return Platform.ILLUMINA;
          }
        };
      }
    };
  }

  private static RunProcessor mixedResultProcessor() {
    return new RunProcessor(
        new RunProcessor.Builder(Platform.ILLUMINA, "test-mixed", (b, p) -> null)) {
      @Override
      public Stream<File> getRunsFromRoot(File root) {
        return Stream.empty();
      }

      @Override
      public PathType getPathType() {
        return PathType.DIRECTORY;
      }

      @Override
      public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
        if (runDirectory.getName().equals("badRun")) {
          throw new IOException("simulated processing failure");
        }
        return new NotificationDto() {
          @Override
          public Platform getPlatformType() {
            return Platform.ILLUMINA;
          }
        };
      }
    };
  }

  private static void awaitUninterruptibly(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  // Returns as soon as the condition holds and fails if it's never met within the timeout.
  private static void awaitUntil(BooleanSupplier condition, Duration timeout)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(timeout);
    while (!condition.getAsBoolean()) {
      if (Instant.now().isAfter(deadline)) {
        fail("Condition not met within " + timeout);
      }
      Thread.sleep(5);
    }
  }

  private static Configuration makeConfiguration(String path) {
    Configuration configuration = new Configuration();
    configuration.setPath(new File(path));
    return configuration;
  }

  private static Pair<File, Configuration> makeRun(Configuration configuration, String runName) {
    return new Pair<>(new File(configuration.getPath(), runName), configuration);
  }

  private static List<Pair<File, Configuration>> runsFor(
      Configuration configuration, String... runNames) {
    return Arrays.stream(runNames)
        .map(runName -> makeRun(configuration, runName))
        .collect(Collectors.toList());
  }

  @Test
  public void testRoundRobinAlternatesEvenLists() {
    Configuration sequencerA = makeConfiguration("/data/sequencerA");
    Configuration sequencerB = makeConfiguration("/data/sequencerB");

    List<List<Pair<File, Configuration>>> runsBySequencer =
        Arrays.asList(
            runsFor(sequencerA, "run1", "run2", "run3"),
            runsFor(sequencerB, "run1", "run2", "run3"));

    List<Pair<File, Configuration>> result =
        Scheduler.roundRobin(runsBySequencer).collect(Collectors.toList());

    assertEquals(
        Arrays.asList(
            makeRun(sequencerA, "run1"),
            makeRun(sequencerB, "run1"),
            makeRun(sequencerA, "run2"),
            makeRun(sequencerB, "run2"),
            makeRun(sequencerA, "run3"),
            makeRun(sequencerB, "run3")),
        result);
  }

  @Test
  public void testRoundRobinContinuesAfterShorterSequencerRunsOutOfRuns() {
    Configuration sequencerA = makeConfiguration("/data/sequencerA");
    Configuration sequencerB = makeConfiguration("/data/sequencerB");
    Configuration sequencerC = makeConfiguration("/data/sequencerC");

    List<List<Pair<File, Configuration>>> runsBySequencer =
        Arrays.asList(
            runsFor(sequencerA, "run1", "run2", "run3", "run4"),
            runsFor(sequencerB, "run1"),
            runsFor(sequencerC, "run1", "run2"));

    List<Pair<File, Configuration>> result =
        Scheduler.roundRobin(runsBySequencer).collect(Collectors.toList());

    assertEquals(
        Arrays.asList(
            makeRun(sequencerA, "run1"),
            makeRun(sequencerB, "run1"),
            makeRun(sequencerC, "run1"),
            makeRun(sequencerA, "run2"),
            makeRun(sequencerC, "run2"),
            makeRun(sequencerA, "run3"),
            makeRun(sequencerA, "run4")),
        result);
  }

  @Test
  public void testRoundRobinHandlesEmptyInput() {
    List<List<Pair<File, Configuration>>> runsBySequencer = Collections.emptyList();

    Stream<Pair<File, Configuration>> result = Scheduler.roundRobin(runsBySequencer);

    assertTrue(result.collect(Collectors.toList()).isEmpty());
  }

  @Test
  public void testBasicIgnoreSubdirectoryShouldReturnFalse() {
    File currentDirectory = new File("/base/test1/");
    File ignoreDirectory = new File("ignore");
    File baseDirectory = new File("/base/");
    List<File> ignoreDirectories = Collections.singletonList(ignoreDirectory);

    assertFalse(Scheduler.skipSubDirectory(currentDirectory, ignoreDirectories, baseDirectory));
  }

  @Test
  public void testBasicIgnoreSubdirectoryShouldReturnTrue() {
    File currentDirectory = new File("/base/test2/");
    File ignoreDirectory = new File("test2");
    File baseDirectory = new File("/base/");
    List<File> ignoreDirectories = Collections.singletonList(ignoreDirectory);

    assertTrue(Scheduler.skipSubDirectory(currentDirectory, ignoreDirectories, baseDirectory));
  }

  @Test
  public void testIgnoreSameSubdirectoryNameShouldReturnFalse() {
    File currentDirectory = new File("/base/run3/");
    File ignoreDirectory = new File("/run3/run3/");
    File baseDirectory = new File("/base/");
    List<File> ignoreDirectories = Collections.singletonList(ignoreDirectory);

    assertFalse(Scheduler.skipSubDirectory(currentDirectory, ignoreDirectories, baseDirectory));
  }

  @Test
  public void testIgnoreSameDirectoryNameShouldReturnTrue() {
    File currentDirectory = new File("/base/run4/run4");
    File ignoreDirectory = new File("/run4/");
    File baseDirectory = new File("/base/");
    List<File> ignoreDirectories = Collections.singletonList(ignoreDirectory);

    assertTrue(Scheduler.skipSubDirectory(currentDirectory, ignoreDirectories, baseDirectory));
  }

  @Test
  public void testMultiLevelIgnoreSubdirectoryShouldReturnTrue() {
    File currentDirectory = new File("/base/ignore/test5/");
    File ignoreDirectory = new File("/ignore/");
    File baseDirectory = new File("/base/");
    List<File> ignoreDirectories = Collections.singletonList(ignoreDirectory);

    assertTrue(Scheduler.skipSubDirectory(currentDirectory, ignoreDirectories, baseDirectory));
  }

  @Test
  public void testMultiLevelRunDirectoryShouldReturnFalse() {
    File currentDirectory = new File("/base/bbb/aaa/");
    File ignoreDirectory = new File("/aaa/");
    File baseDirectory = new File("/base/");
    List<File> ignoreDirectories = Collections.singletonList(ignoreDirectory);

    assertFalse(Scheduler.skipSubDirectory(currentDirectory, ignoreDirectories, baseDirectory));
  }

  @Test
  public void testNewestFirstQueueInsertsNewTaskAheadOfExistingBacklog() {
    Scheduler.NewestFirstQueue queue = new Scheduler.NewestFirstQueue();
    Runnable oldBacklogTask = () -> {};
    Runnable freshTask = () -> {};

    queue.offer(oldBacklogTask);
    queue.offer(freshTask);

    assertEquals(Arrays.asList(freshTask, oldBacklogTask), new ArrayList<>(queue));
  }

  @Test
  public void testQueueDirectoryForwardSubmissionRunsFreshBatchBeforeOlderBacklogNewestFirst()
      throws InterruptedException {
    Scheduler scheduler = new Scheduler();
    schedulersToStop.add(scheduler);

    int coreThreads = scheduler.workPool.getCorePoolSize();
    // Occupy all-but-one worker thread permanently (torn down via stop()'s shutdownNow() in the
    // @After teardown), and put the last one behind a separately releasable gate, so exactly one
    // thread is ever free to drain the queue. This matters because dequeue order alone doesn't
    // determine *recorded* completion order: if N>1 threads were released together, each would
    // independently race to record its own item after dequeuing, so a correct, deterministic
    // dequeue order could still show up scrambled in the recorded order purely from thread
    // scheduling after the dequeue. With only one thread ever free, it must finish recording item
    // 1 before it can even ask for item 2, so recorded order and dequeue order coincide.
    CountDownLatch permanentGate = new CountDownLatch(1);
    CountDownLatch releaseGate = new CountDownLatch(1);
    CountDownLatch allWorkersStarted = new CountDownLatch(coreThreads);
    for (int i = 0; i < coreThreads - 1; i++) {
      scheduler.workPool.execute(
          () -> {
            allWorkersStarted.countDown();
            awaitUninterruptibly(permanentGate);
          });
    }
    scheduler.workPool.execute(
        () -> {
          allWorkersStarted.countDown();
          awaitUninterruptibly(releaseGate);
        });
    assertTrue(allWorkersStarted.await(10, TimeUnit.SECONDS));

    List<String> executionOrder = new CopyOnWriteArrayList<>();
    CountDownLatch allProcessed = new CountDownLatch(4); // 1 backlog run + 3 fresh runs

    Configuration backlogConfig = makeConfiguration("/data/backlogSequencer");
    configureForTest(backlogConfig, recordingProcessor(executionOrder, allProcessed));

    Configuration freshConfig = makeConfiguration("/data/freshSequencer");
    configureForTest(freshConfig, recordingProcessor(executionOrder, allProcessed));

    // Simulate a run left over from an earlier scan cycle.
    scheduler.queueDirectory(
        new File(backlogConfig.getPath(), "oldBacklogRun"),
        backlogConfig.getProcessor(),
        backlogConfig.getTimeZone());

    // DefaultIllumina/DefaultUltima return runs newest-first; Scheduler.start() reverses each
    // sequencer's list back to oldest-first (see Scheduler#oldestFirstRuns) before submitting via
    // forEach -- so feed queueDirectory in that same oldest-first order here.
    for (Pair<File, Configuration> entry :
        runsFor(freshConfig, "freshRunOldest", "freshRunMiddle", "freshRunNewest")) {
      scheduler.queueDirectory(
          entry.first(), entry.second().getProcessor(), entry.second().getTimeZone());
    }

    releaseGate.countDown();
    assertTrue(allProcessed.await(10, TimeUnit.SECONDS));

    assertEquals(
        Arrays.asList("freshRunNewest", "freshRunMiddle", "freshRunOldest", "oldBacklogRun"),
        executionOrder);
  }

  @Test
  public void testQueueDirectoryPreservesBookkeepingAndErrorHandling() throws InterruptedException {
    Scheduler scheduler = new Scheduler();
    schedulersToStop.add(scheduler);

    Configuration configuration = makeConfiguration("/data/mixedSequencer");
    configureForTest(configuration, mixedResultProcessor());

    File goodRunFile = new File(configuration.getPath(), "goodRun");
    File badRunFile = new File(configuration.getPath(), "badRun");

    int epochBefore = scheduler.getEpoch();
    scheduler.queueDirectory(
        goodRunFile, configuration.getProcessor(), configuration.getTimeZone());
    scheduler.queueDirectory(badRunFile, configuration.getProcessor(), configuration.getTimeZone());

    awaitUntil(
        () ->
            scheduler.getFinishedDirectories().contains(goodRunFile)
                && scheduler.getFailedDirectories().contains(badRunFile)
                && scheduler.getCurrentWork().isEmpty(),
        Duration.ofSeconds(10));

    assertEquals(Collections.singleton(goodRunFile), scheduler.getFinishedDirectories());
    assertEquals(Collections.singleton(badRunFile), scheduler.getFailedDirectories());
    assertTrue(scheduler.getCurrentWork().isEmpty());
    assertTrue(scheduler.getEpoch() > epochBefore);
  }
}
