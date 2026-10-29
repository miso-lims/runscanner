package ca.on.oicr.gsi.runscanner.scanner.processor.ultima;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.CRC32C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Lists Ultima run output from a local filesystem directory. */
public class LocalStorageClient implements UltimaStorageClient {

  private static final Logger log = LoggerFactory.getLogger(LocalStorageClient.class);

  /** Mimics: ls {path} */
  @Override
  public List<UltimaStorageEntry> ls(String path) throws IOException {
    Path dir = Paths.get(path);
    List<Path> children;
    try (Stream<Path> stream = Files.list(dir)) {
      children = stream.toList();
    }
    List<UltimaStorageEntry> entries = new ArrayList<>();
    for (Path child : children) {
      entries.add(new LocalStorageEntry(child));
    }
    return entries;
  }

  private static class LocalStorageEntry implements UltimaStorageEntry {
    private final Path path;
    private final boolean directory;
    private final long size;
    private final Instant createdTime;
    private final Instant modifiedTime;

    LocalStorageEntry(Path path) throws IOException {
      this.path = path;
      this.directory = Files.isDirectory(path);
      if (directory) {
        this.size = 0;
        this.createdTime = null;
        this.modifiedTime = null;
      } else {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        this.size = attributes.size();
        this.modifiedTime = attributes.lastModifiedTime().toInstant();
        this.createdTime = attributes.creationTime().toInstant();
      }
    }

    private static String computeCrc32cBase64Checksum(Path path) throws IOException {
      // Same format GCS reports: base64 of the CRC32C as 4 big-endian bytes
      CRC32C crc32c = new CRC32C();
      byte[] buffer = new byte[8192];
      try (InputStream in = Files.newInputStream(path)) {
        int read;
        while ((read = in.read(buffer)) != -1) {
          crc32c.update(buffer, 0, read);
        }
      }
      byte[] checksum = ByteBuffer.allocate(4).putInt((int) crc32c.getValue()).array();
      return Base64.getEncoder().encodeToString(checksum);
    }

    @Override
    public boolean isDirectory() {
      return directory;
    }

    @Override
    public String getName() {
      return path.getFileName().toString();
    }

    @Override
    public String getFullPath() {
      return path.toAbsolutePath().toString();
    }

    @Override
    public URI getPath() {
      return path.toAbsolutePath().toUri();
    }

    @Override
    public long getSize() {
      return size;
    }

    @Override
    public String getCrc32cBase64Checksum() throws IOException {
      // Computed on demand: ls() also lists folders whose files are never used, and reading every
      // file (e.g. multi-GB CRAMs) on each scan would be expensive
      return directory ? null : computeCrc32cBase64Checksum(path);
    }

    @Override
    public Instant getCreatedTime() {
      // Some local filesystems don't expose a reliable creation time; fall back to modified time
      return createdTime != null ? createdTime : modifiedTime;
    }

    @Override
    public Instant getModifiedTime() {
      return modifiedTime;
    }
  }
}
