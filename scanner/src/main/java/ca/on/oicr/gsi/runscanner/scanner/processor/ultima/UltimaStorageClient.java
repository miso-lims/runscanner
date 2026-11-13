package ca.on.oicr.gsi.runscanner.scanner.processor.ultima;

import java.io.IOException;
import java.util.List;

/**
 * Lists the contents of a directory-like path for Ultima run output, whether backed by a Google
 * Cloud Storage bucket or a local filesystem directory.
 */
public interface UltimaStorageClient {

  List<UltimaStorageEntry> ls(String path) throws IOException;

  /**
   * Reads the full contents of a file as UTF-8 text.
   *
   * @param path the file's full path, as returned by {@link UltimaStorageEntry#getFullPath()}
   */
  String readFile(String path) throws IOException;
}
