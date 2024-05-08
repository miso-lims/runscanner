package ca.on.oicr.gsi.runscanner.scanner;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class SchedulerTest {

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
