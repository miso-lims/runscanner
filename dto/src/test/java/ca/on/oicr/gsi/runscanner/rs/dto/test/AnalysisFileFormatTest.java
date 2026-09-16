package ca.on.oicr.gsi.runscanner.rs.dto.test;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

/**
 * Guards the contract that consuming projects rely on: the "format" discriminator written into an
 * AnalysisFile's JSON is exactly what getFormatType() reports, so a consumer never has to inspect
 * the raw JSON to find out what kind of file it has.
 */
public class AnalysisFileFormatTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  /**
   * The subtype name and the format returned by the instance are declared in two different places,
   * so nothing but this test stops them from drifting apart.
   */
  @Test
  public void testFormatTypeMatchesRegisteredSubTypeName() throws Exception {
    for (JsonSubTypes.Type subType : subTypes()) {
      AnalysisFile file = newInstance(subType);
      Assert.assertEquals(
          "getFormatType() disagrees with @JsonSubTypes for " + subType.value().getSimpleName(),
          subType.name(),
          file.getFormatType().getFormat());
    }
  }

  @Test
  public void testEveryFormatHasARegisteredSubType() {
    Set<String> registered =
        Arrays.stream(subTypes()).map(JsonSubTypes.Type::name).collect(Collectors.toSet());
    Set<String> known =
        Arrays.stream(AnalysisFileFormat.values())
            .map(AnalysisFileFormat::getFormat)
            .collect(Collectors.toSet());
    Assert.assertEquals(known, registered);
  }

  @Test
  public void testFormatSurvivesRoundTrip() throws Exception {
    for (JsonSubTypes.Type subType : subTypes()) {
      AnalysisFile file = newInstance(subType);
      file.setPath(URI.create("file:/path/to/file"));
      file.setCrc32Checksum("abcdefg");
      file.setSize(1000L);
      file.setCreatedTime(Instant.EPOCH);
      file.setModifiedTime(Instant.EPOCH);

      String serialized = mapper.writeValueAsString(file);
      // Jackson is solely responsible for writing the discriminator. Two copies would mean the DTO
      // is writing it as well, which not every parser on the consuming end will accept.
      Assert.assertEquals(serialized, 1, countOccurrences(serialized, "\"format\""));
      Assert.assertTrue(serialized, serialized.contains("\"format\":\"" + subType.name() + "\""));

      // Reading through the base type is how consumers deserialize a list of files.
      AnalysisFile deserialized = mapper.readValue(serialized, AnalysisFile.class);
      Assert.assertEquals(subType.value(), deserialized.getClass());
      Assert.assertEquals(file.getFormatType(), deserialized.getFormatType());
    }
  }

  private static JsonSubTypes.Type[] subTypes() {
    JsonSubTypes annotation = AnalysisFile.class.getAnnotation(JsonSubTypes.class);
    Assert.assertNotNull("AnalysisFile is missing @JsonSubTypes", annotation);
    return annotation.value();
  }

  private static AnalysisFile newInstance(JsonSubTypes.Type subType) throws Exception {
    return (AnalysisFile) subType.value().getDeclaredConstructor().newInstance();
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0;
    for (int i = haystack.indexOf(needle);
        i >= 0;
        i = haystack.indexOf(needle, i + needle.length())) {
      count++;
    }
    return count;
  }
}
