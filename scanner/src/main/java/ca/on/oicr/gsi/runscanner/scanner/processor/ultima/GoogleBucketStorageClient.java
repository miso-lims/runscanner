package ca.on.oicr.gsi.runscanner.scanner.processor.ultima;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleBucketStorageClient implements UltimaStorageClient {

  private static final Logger log = LoggerFactory.getLogger(GoogleBucketStorageClient.class);
  private static final List<String> GCS_SCOPES =
      List.of("https://www.googleapis.com/auth/cloud-platform");

  private final Storage storage;

  public GoogleBucketStorageClient(String googleCredentialsFile) throws IOException {
    GoogleCredentials credentials;
    try (FileInputStream keyStream = new FileInputStream(googleCredentialsFile)) {
      credentials = GoogleCredentials.fromStream(keyStream).createScoped(GCS_SCOPES);
    }
    this.storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
  }

  /** Mimics: gcloud storage ls {gcsPath} */
  @Override
  public List<UltimaStorageEntry> ls(String gcsPath) throws IOException {
    String[] parts = parseGcsPath(gcsPath);
    String bucket = parts[0];
    String runPath = parts[1];

    List<Storage.BlobListOption> options = new ArrayList<>();
    if (!runPath.isEmpty()) {
      options.add(Storage.BlobListOption.prefix(runPath));
    }
    // delimiter("/") makes the listing non-recursive: objects whose names contain a '/' after the
    // prefix are collapsed into virtual directory entries rather than being enumerated.
    options.add(Storage.BlobListOption.delimiter("/"));

    List<UltimaStorageEntry> entries = new ArrayList<>();
    try {
      storage
          .list(bucket, options.toArray(new Storage.BlobListOption[0]))
          .iterateAll()
          .forEach(blob -> entries.add(new GcsStorageEntry(blob)));
    } catch (StorageException e) {
      throw new IOException("Failed to list GCS path: " + gcsPath, e);
    }
    return entries;
  }

  private static String[] parseGcsPath(String gcsPath) {
    // Paths are passed as "bucket", "bucket/runFolder/", or "bucket/runFolder/barcodeFolder/".
    // Split on the first '/' to separate the bucket name from the rest of the path, if present.
    int slashIdx = gcsPath.indexOf('/');
    if (slashIdx < 0) {
      return new String[] {gcsPath, ""};
    }
    String bucket = gcsPath.substring(0, slashIdx);
    String runPath = gcsPath.substring(slashIdx + 1);
    // GCS prefix-based listing treats a trailing '/' as a directory boundary. Without it, a prefix
    // of "seq" would also match "seq2/", "seq-old/", etc.
    if (!runPath.isEmpty() && !runPath.endsWith("/")) {
      runPath = runPath + "/";
    }
    return new String[] {bucket, runPath};
  }

  private static String extractFolderName(String blobName) {
    // GCS dir blobs have a trailing '/'. Strip it, then take everything after the last '/' to get
    // folder name
    String[] blobNameParts = blobName.split("/");
    return blobNameParts[blobNameParts.length - 1];
  }

  private static class GcsStorageEntry implements UltimaStorageEntry {
    private final Blob blob;
    private final URI path;

    GcsStorageEntry(Blob blob) {
      this.blob = blob;
      try {
        this.path = new URI("gs", blob.getBucket(), "/" + blob.getName(), null);
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public boolean isDirectory() {
      return blob.isDirectory();
    }

    @Override
    public String getName() {
      return extractFolderName(blob.getName());
    }

    @Override
    public String getFullPath() {
      return blob.getBucket() + "/" + blob.getName();
    }

    @Override
    public URI getPath() {
      return path;
    }

    @Override
    public long getSize() {
      return blob.getSize();
    }

    @Override
    public String getCrc32Checksum() {
      return blob.getCrc32c();
    }

    @Override
    public Instant getCreatedTime() {
      return blob.getCreateTimeOffsetDateTime() == null
          ? null
          : blob.getCreateTimeOffsetDateTime().toInstant();
    }

    @Override
    public Instant getModifiedTime() {
      return blob.getUpdateTimeOffsetDateTime() == null
          ? null
          : blob.getUpdateTimeOffsetDateTime().toInstant();
    }
  }
}
