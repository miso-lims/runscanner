package ca.on.oicr.gsi.runscanner.rs.dto.test;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.UnknownAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

  /**
   * The other two invariant tests enumerate {@code @JsonSubTypes}, so a subclass that was never
   * registered there is invisible to them. Jackson does not complain either: serializing an
   * unregistered subtype silently falls back to writing the simple class name as the discriminator,
   * so a forgotten {@code @Type} ships as {@code "format":"BamAnalysisFile"} and every consumer
   * reads it as an UnknownAnalysisFile. This walks the compiled DTO classes instead, so the check
   * starts from what exists rather than from what was remembered.
   */
  @Test
  public void testEverySubclassIsRegistered() throws Exception {
    Set<Class<?>> registered =
        Arrays.stream(subTypes()).map(JsonSubTypes.Type::value).collect(Collectors.toSet());

    Set<Class<?>> unregistered = new TreeSet<>(Comparator.comparing(Class::getName));
    for (Class<?> candidate : compiledDtoClasses()) {
      if (AnalysisFile.class.isAssignableFrom(candidate)
          && !candidate.equals(AnalysisFile.class)
          // An abstract intermediate class is never instantiated by Jackson, so it needs no name.
          && !Modifier.isAbstract(candidate.getModifiers())
          && !registered.contains(candidate)) {
        unregistered.add(candidate);
      }
    }

    Assert.assertEquals(
        "AnalysisFile subclasses missing from @JsonSubTypes. Add a @Type(value = ..., name = ...)"
            + " for each, and a matching AnalysisFileFormat constant.",
        Collections.emptySet(),
        unregistered);
  }

  /** Every class compiled into the module that declares {@link AnalysisFile}. */
  private static List<Class<?>> compiledDtoClasses() throws Exception {
    Path root =
        Paths.get(AnalysisFile.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Assert.assertTrue(
        "Expected the DTO classes to be an exploded directory, but found " + root,
        Files.isDirectory(root));

    List<Class<?>> classes = new ArrayList<>();
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".class")).toList()) {
        String name =
            root.relativize(file)
                .toString()
                .replace(File.separatorChar, '.')
                .replaceAll("\\.class$", "");
        // Initializing is unnecessary and would run static blocks just to read the hierarchy.
        classes.add(Class.forName(name, false, AnalysisFile.class.getClassLoader()));
      }
    }
    Assert.assertFalse("Found no compiled classes under " + root, classes.isEmpty());
    return classes;
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
      Assert.assertEquals(subType.name(), deserialized.getRawFormat());
    }
  }

  /**
   * A format added by a newer producer must not be fatal. Before AnalysisFile had a defaultImpl,
   * this threw InvalidTypeIdException.
   */
  @Test
  public void testUnrecognisedFormatDeserializesToUnknownAnalysisFile() throws Exception {
    AnalysisFile file =
        mapper.readValue(
            "{\"format\":\"bam\",\"path\":\"file:/path/to/file\",\"size\":1000}",
            AnalysisFile.class);
    Assert.assertEquals(UnknownAnalysisFile.class, file.getClass());
    Assert.assertEquals(AnalysisFileFormat.UNKNOWN, file.getFormatType());
    // The enum cannot say more than UNKNOWN, so the raw discriminator is the only record of what
    // the producer actually called this format.
    Assert.assertEquals("bam", file.getRawFormat());
    Assert.assertEquals(URI.create("file:/path/to/file"), file.getPath());
    Assert.assertEquals(1000L, file.getSize());
  }

  @Test
  public void testMissingFormatDeserializesToUnknownAnalysisFile() throws Exception {
    AnalysisFile file =
        mapper.readValue("{\"path\":\"file:/path/to/file\",\"size\":1000}", AnalysisFile.class);
    Assert.assertEquals(UnknownAnalysisFile.class, file.getClass());
    Assert.assertEquals(AnalysisFileFormat.UNKNOWN, file.getFormatType());
    // There was no discriminator to preserve.
    Assert.assertNull(file.getRawFormat());
    Assert.assertEquals(URI.create("file:/path/to/file"), file.getPath());
  }

  /**
   * The point of the defaultImpl: one unrecognised file used to abort the read of the entire
   * enclosing notification, taking every well-formed file with it.
   */
  @Test
  public void testUnrecognisedFormatDoesNotSinkItsSiblings() throws Exception {
    List<AnalysisFile> files =
        mapper.readValue(
            "[{\"format\":\"cram\",\"path\":\"file:/a\"},"
                + "{\"format\":\"bam\",\"path\":\"file:/b\"},"
                + "{\"format\":\"metadata\",\"path\":\"file:/c\"}]",
            new TypeReference<List<AnalysisFile>>() {});

    Assert.assertEquals(3, files.size());
    Assert.assertEquals(AnalysisFileFormat.CRAM, files.get(0).getFormatType());
    Assert.assertEquals(AnalysisFileFormat.UNKNOWN, files.get(1).getFormatType());
    Assert.assertEquals("bam", files.get(1).getRawFormat());
    Assert.assertEquals(AnalysisFileFormat.METADATA, files.get(2).getFormatType());
  }

  /**
   * Re-serializing an unrecognised file cannot round-trip the format it came in with, since Jackson
   * writes the discriminator from the type. Consumers that forward AnalysisFiles need to know that
   * "bam" goes in and "unknown" comes out.
   */
  @Test
  public void testUnknownAnalysisFileSerializesAsUnknown() throws Exception {
    AnalysisFile file =
        mapper.readValue("{\"format\":\"bam\",\"path\":\"file:/b\"}", AnalysisFile.class);
    String serialized = mapper.writeValueAsString(file);
    Assert.assertEquals(serialized, 1, countOccurrences(serialized, "\"format\""));
    Assert.assertTrue(serialized, serialized.contains("\"format\":\"unknown\""));
    Assert.assertEquals("bam", file.getRawFormat());
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
