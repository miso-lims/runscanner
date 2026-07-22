package ca.on.oicr.gsi.runscanner.scanner.processor.ultima;

import java.nio.file.Path;
import java.time.Instant;

/**
 * One entry (file or directory) returned by {@link UltimaStorageClient#ls(String)}, abstracting
 * over Google Cloud Storage blobs and local filesystem entries.
 */
public interface UltimaStorageEntry {
  boolean isDirectory();

  /** The last path segment (folder/file name), with no trailing slash. */
  String getName();

  /** The full path for this entry */
  String getFullPath();

  /** The structured path to store on the resulting DTO. */
  Path getPath();

  long getSize();

  /** Base64 CRC32C checksum, or null if unavailable */
  String getCrc32Checksum();

  Instant getCreatedTime();

  Instant getModifiedTime();
}
