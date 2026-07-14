package ca.on.oicr.gsi.runscanner.scanner;

import static org.junit.Assert.*;

import ca.on.oicr.gsi.Pair;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;

public class SchedulerTest {

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
}
