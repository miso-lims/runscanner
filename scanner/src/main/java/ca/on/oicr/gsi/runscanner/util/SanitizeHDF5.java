package ca.on.oicr.gsi.runscanner.util;
/**
 * A commandline tool for removing the data and untested metadata from a fast5 file. Used to prepare
 * files for use as resources in integration testing. Non-destructive, creates a new file.
 *
 * <p>Creates a new file and copies what we need into it from an existing file.
 */
import static java.lang.System.exit;
import static java.nio.file.StandardCopyOption.*;

import ch.systemsx.cisd.base.mdarray.MDArray;
import ch.systemsx.cisd.hdf5.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SanitizeHDF5 {
  private static final int EXIT_ERROR = 1, EXIT_OK = 0;
  private File oldFile, cleanFile;
  private List<String> groupsToKeep =
      Stream.of("context_tags", "tracking_id").collect(Collectors.toList());

  private SanitizeHDF5(File existingFast5) {
    try {
      oldFile = existingFast5;
      cleanFile = newSanitizedFile(existingFast5);
    } catch (IOException e) {
      errorOut("Unable to create a new file.");
    }
    exit(sanitize());
  }

  private int sanitize() {
    try {
      // Open old file read-only, open new file with write capabilities
      IHDF5Reader reader = HDF5FactoryProvider.get().openForReading(oldFile);
      IHDF5Writer writer =
          HDF5FactoryProvider.get().open(cleanFile); // root object is created automatically

      // Get the names of all the reads in the fast5
      List<String> allReads = reader.object().getAllGroupMembers("/");

      // Arbitrarily select one to keep
      String readToKeep = allReads.get(0);

      // Create a group under its name in the new file
      writer.object().createGroup("/" + readToKeep);

      // Copy the groups we want
      for (String group : groupsToKeep) {
        for (HDF5MDDataBlock<MDArray<String>> array :
            reader.string().getMDArrayNaturalBlocks(readToKeep + "/" + group)) {
          System.out.println(array);
        }
        String newName = "/" + readToKeep + "/" + group;
        writer.object().createGroup(newName);
        writer
            .string()
            .setMDArrayAttr(readToKeep, group, reader.string().getMDArrayAttr(readToKeep, group));
      }

      // Reaching this line indicates success
      return EXIT_OK;

    } catch (Exception e) {
      System.err.println("Error occured while cleaning fast5: " + e.getMessage());
      e.printStackTrace();
      return EXIT_ERROR;
    }
  }

  private File newSanitizedFile(File oldFile) throws IOException {
    Path oldLocation = oldFile.toPath();
    Path newLocation =
        oldLocation.resolveSibling(
            oldLocation.getFileName().toString().replace(".fast5", "_clean.fast5"));

    // If file has been cleaned previously, do not overwrite but create a new file.
    int i = 1;
    while (Files.exists(newLocation)) {
      newLocation =
          newLocation.resolveSibling(
              newLocation
                  .getFileName()
                  .toString()
                  .replace("_clean.fast5", "_clean_" + i + ".fast5")
                  .replace("_clean_" + (i - 1) + ".fast5", "_clean_" + i + ".fast5"));
      i++;
    }
    return Files.createFile(newLocation).toFile();
  }

  private static void errorOut(String message) {
    System.err.println(message);
    exit(EXIT_ERROR);
  }

  public static void main(String[] args) {
    // Ensure correct number of arguments provided.
    if (args.length != 1) {
      errorOut("Please specify one fast5 file to sanitize.");
    }

    // Ensure a fast5 file was specified
    Path path = Paths.get(args[0]);
    PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.fast5");
    if (!matcher.matches(path)) {
      errorOut("Please specify a file path ending with '.fast5'.");
    }

    // Ensure the file exists and we can read and write to it
    File tempFile = path.toFile();
    if (!(tempFile.exists() && tempFile.canRead() && tempFile.canWrite())) {
      errorOut(
          "Could not access specified path. Please verify file exists and permissions are set appropriately.");
    }

    // Start logic
    new SanitizeHDF5(tempFile);
  }
}
