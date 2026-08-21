package ca.on.oicr.gsi.runscanner.scanner.processor.ultima;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.CRC32;
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
    private final String crc32Checksum;

    LocalStorageEntry(Path path) throws IOException {
      this.path = path;
      this.directory = Files.isDirectory(path);
      if (directory) {
        this.size = 0;
        this.createdTime = null;
        this.modifiedTime = null;
        this.crc32Checksum = null;
      } else {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        this.size = attributes.size();
        this.modifiedTime = attributes.lastModifiedTime().toInstant();
        this.createdTime = attributes.creationTime().toInstant();
        this.crc32Checksum = computeCrc32Checksum(path);
      }
    }

    private static String computeCrc32Checksum(Path path) throws IOException {
      // Note: this is a plain CRC32, different from GCS's CRC32C.
      byte[] data = Files.readAllBytes(path);
      CRC32 crc32 = new CRC32();
      crc32.update(data);
      return Long.toString(crc32.getValue());
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
    public String getCrc32Checksum() {
      return crc32Checksum;
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
