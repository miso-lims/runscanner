package ca.on.oicr.gsi.runscanner.scanner.processor;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltimaGoogleBucketClient {

  private static final Logger log = LoggerFactory.getLogger(UltimaGoogleBucketClient.class);
  private static final List<String> GCS_SCOPES =
      List.of("https://www.googleapis.com/auth/cloud-platform");

  private final Storage storage;

  public UltimaGoogleBucketClient(String googleServiceAccount) throws IOException {
    GoogleCredentials sourceCredentials = GoogleCredentials.getApplicationDefault();
    ImpersonatedCredentials impersonatedCredentials =
        ImpersonatedCredentials.create(
            sourceCredentials, googleServiceAccount, null, GCS_SCOPES, 300);
    this.storage =
        StorageOptions.newBuilder().setCredentials(impersonatedCredentials).build().getService();
  }

  /** Mimics: gcloud storage ls --impersonate-service-account {serviceAccount} {gcsPath} */
  public List<Blob> ls(String gcsPath) {
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

    List<Blob> blobs = new ArrayList<>();
    storage
        .list(bucket, options.toArray(new Storage.BlobListOption[0]))
        .iterateAll()
        .forEach(blobs::add);
    return blobs;
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
}
