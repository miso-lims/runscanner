package ca.on.oicr.gsi.runscanner.util;
/**
 * A commandline tool for removing the data and untested metadata from a fast5 file. Used to prepare
 * files for use as resources in integration testing.
 */
import static java.lang.System.exit;

import java.io.File;
import java.nio.file.*;

public class SanitizeHDF5 {
  private static final int EXIT_ERROR = 1;
  private static final int EXIT_OK = 0;
  private File file;

  public SanitizeHDF5(File f) {
    file = f;
    exit(sanitize());
  }

  private int sanitize() {
    System.out.println("Congratulations you reached the end");
    return EXIT_OK;
  }

  public static void main(String[] args) {
    // Ensure correct number of arguments provided.
    if (args.length != 1) {
      System.err.println("Please specify one fast5 file to sanitize.");
      exit(EXIT_ERROR);
    }

    // Ensure a fast5 file was specified
    Path path = Paths.get(args[0]);
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.fast5");
    if (!matcher.matches(path)) {
      System.err.println("Please specify a file path ending with '.fast5'.");
      exit(EXIT_ERROR);
    }

    // Ensure the file exists and we can read and write to it
    File tempFile = path.toFile();
    if (!(tempFile.exists() && tempFile.canRead() && tempFile.canWrite())) {
      System.err.println(
          "Could not access specified path. May not exist or permissions may not be set.");
      exit(EXIT_ERROR);
    }

    // Start logic
    new SanitizeHDF5(tempFile);
  }
}
