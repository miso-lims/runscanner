package ca.on.oicr.gsi.runscanner.rs.dto.test;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisFileFormat;
import ca.on.oicr.gsi.runscanner.dto.ultima.CramAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaAnalysisFile;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
import java.util.HashSet;
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
 *
 * <p>Each analysis suite declares its own discriminator and subtypes, so every check runs once per
 * suite base class.
 */
public class AnalysisFileFormatTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  /** Every suite's base class. A new analysis suite must be added here. */
  private static final List<Class<? extends AnalysisFile>> SUITES =
      List.of(DragenAnalysisFile.class, UltimaAnalysisFile.class);

  /**
   * The subtype name and the format returned by the instance are declared in two different places,
   * so nothing but this test stops them from drifting apart.
   */
  @Test
  public void testFormatTypeMatchesRegisteredSubTypeName() throws Exception {
    for (Class<? extends AnalysisFile> suite : SUITES) {
      for (JsonSubTypes.Type subType : subTypes(suite)) {
        AnalysisFile file = newInstance(subType);
        Assert.assertEquals(
            "getFormatType() disagrees with @JsonSubTypes for " + subType.value().getName(),
            subType.name(),
            file.getFormatType().getFormat());
      }
    }
  }

  /** Each format must be produced by some suite, and every suite must handle "unknown". */
  @Test
  public void testEveryFormatHasARegisteredSubType() {
    Set<String> registered = new HashSet<>();
    for (Class<? extends AnalysisFile> suite : SUITES) {
      Set<String> suiteNames =
          Arrays.stream(subTypes(suite)).map(JsonSubTypes.Type::name).collect(Collectors.toSet());
      Assert.assertTrue(
          suite.getSimpleName() + " does not register \"unknown\"",
          suiteNames.contains(AnalysisFileFormat.UNKNOWN.getFormat()));
      registered.addAll(suiteNames);
    }
    Set<String> known =
        Arrays.stream(AnalysisFileFormat.values())
            .map(AnalysisFileFormat::getFormat)
            .collect(Collectors.toSet());
    Assert.assertEquals(known, registered);
  }

  /**
   * The other invariant tests enumerate {@code @JsonSubTypes}, so a subclass that was never
   * registered there is invisible to them. Jackson does not complain either: serializing an
   * unregistered subtype silently falls back to writing the simple class name as the discriminator,
   * so a forgotten {@code @Type} ships as {@code "format":"BamAnalysisFile"} and every consumer
   * reads it as an UnknownAnalysisFile. This walks the compiled DTO classes instead, so the check
   * starts from what exists rather than from what was remembered.
   */
  @Test
  public void testEverySubclassIsRegistered() throws Exception {
    Set<Class<?>> unregistered = new TreeSet<>(Comparator.comparing(Class::getName));
    for (Class<?> candidate : compiledDtoClasses()) {
      // An abstract intermediate class is never instantiated by Jackson, so it needs no name.
      if (!AnalysisFile.class.isAssignableFrom(candidate)
          || Modifier.isAbstract(candidate.getModifiers())) {
        continue;
      }
      Class<?> suite =
          SUITES.stream().filter(s -> s.isAssignableFrom(candidate)).findFirst().orElse(null);
      if (suite == null
          || Arrays.stream(subTypes(suite))
              .map(JsonSubTypes.Type::value)
              .noneMatch(candidate::equals)) {
        unregistered.add(candidate);
      }
    }

    Assert.assertEquals(
        "AnalysisFile subclasses that don't extend a suite base class, or are missing from its"
            + " @JsonSubTypes. Add a @Type(value = ..., name = ...) for each, and a matching"
            + " AnalysisFileFormat constant.",
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
    for (Class<? extends AnalysisFile> suite : SUITES) {
      for (JsonSubTypes.Type subType : subTypes(suite)) {
        AnalysisFile file = newInstance(subType);
        file.setPath(URI.create("file:/path/to/file"));
        file.setSize(1000L);
        file.setCreatedTime(Instant.EPOCH);
        file.setModifiedTime(Instant.EPOCH);

        String serialized = mapper.writeValueAsString(file);
        // Jackson is solely responsible for writing the discriminator. Two copies would mean the
        // DTO is writing it as well, which not every parser on the consuming end will accept.
        Assert.assertEquals(serialized, 1, countOccurrences(serialized, "\"format\""));
        Assert.assertTrue(serialized, serialized.contains("\"format\":\"" + subType.name() + "\""));

        // Reading through the suite base type is how consumers deserialize a list of files.
        AnalysisFile deserialized = mapper.readValue(serialized, suite);
        Assert.assertEquals(subType.value(), deserialized.getClass());
        Assert.assertEquals(file.getFormatType(), deserialized.getFormatType());
        Assert.assertEquals(subType.name(), deserialized.getRawFormat());
      }
    }
  }

  /**
   * The same format name can mean a different class in each suite, so it is only ever resolved
   * against the suite being read. DRAGEN registers no CRAM, so there "cram" is unrecognised.
   */
  @Test
  public void testFormatResolvesWithinItsSuite() throws Exception {
    String json = "{\"format\":\"cram\",\"path\":\"file:/path/to/file\"}";
    Assert.assertEquals(
        CramAnalysisFile.class, mapper.readValue(json, UltimaAnalysisFile.class).getClass());
    Assert.assertEquals(
        unknownType(DragenAnalysisFile.class),
        mapper.readValue(json, DragenAnalysisFile.class).getClass());
  }

  /**
   * A format added by a newer producer must not be fatal. Before AnalysisFile had a defaultImpl,
   * this threw InvalidTypeIdException.
   */
  @Test
  public void testUnrecognisedFormatDeserializesToUnknownAnalysisFile() throws Exception {
    for (Class<? extends AnalysisFile> suite : SUITES) {
      AnalysisFile file =
          mapper.readValue(
              "{\"format\":\"bam\",\"path\":\"file:/path/to/file\",\"size\":1000}", suite);
      Assert.assertEquals(unknownType(suite), file.getClass());
      Assert.assertEquals(AnalysisFileFormat.UNKNOWN, file.getFormatType());
      // The enum cannot say more than UNKNOWN, so the raw discriminator is the only record of what
      // the producer actually called this format.
      Assert.assertEquals("bam", file.getRawFormat());
      Assert.assertEquals(URI.create("file:/path/to/file"), file.getPath());
      Assert.assertEquals(1000L, file.getSize());
    }
  }

  /**
   * Checksums and other format-specific fields live on the subclasses, so an unrecognised format's
   * extra properties must not make it fail to deserialize.
   */
  @Test
  public void testUnrecognisedFormatIgnoresFormatSpecificProperties() throws Exception {
    for (Class<? extends AnalysisFile> suite : SUITES) {
      AnalysisFile file =
          mapper.readValue(
              "{\"format\":\"bam\",\"path\":\"file:/path/to/file\",\"size\":1000,"
                  + "\"crc32cBase64Checksum\":\"AAAAAA==\",\"readCount\":5}",
              suite);
      Assert.assertEquals(unknownType(suite), file.getClass());
      Assert.assertEquals("bam", file.getRawFormat());
      Assert.assertEquals(URI.create("file:/path/to/file"), file.getPath());
    }
  }

  /**
   * A known format carrying a checksum key from another DTO version (e.g. a cram served with the
   * old "crc32Checksum") must still deserialize rather than sink the enclosing notification.
   */
  @Test
  public void testKnownFormatIgnoresForeignChecksum() throws Exception {
    UltimaAnalysisFile file =
        mapper.readValue(
            "{\"format\":\"cram\",\"path\":\"file:/path/to/file\",\"size\":1000,"
                + "\"crc32Checksum\":\"abcdefg\"}",
            UltimaAnalysisFile.class);
    Assert.assertEquals(CramAnalysisFile.class, file.getClass());
    Assert.assertNull(file.getCrc32cBase64Checksum());
    Assert.assertEquals(URI.create("file:/path/to/file"), file.getPath());
  }

  @Test
  public void testMissingFormatDeserializesToUnknownAnalysisFile() throws Exception {
    for (Class<? extends AnalysisFile> suite : SUITES) {
      AnalysisFile file =
          mapper.readValue("{\"path\":\"file:/path/to/file\",\"size\":1000}", suite);
      Assert.assertEquals(unknownType(suite), file.getClass());
      Assert.assertEquals(AnalysisFileFormat.UNKNOWN, file.getFormatType());
      // There was no discriminator to preserve.
      Assert.assertNull(file.getRawFormat());
      Assert.assertEquals(URI.create("file:/path/to/file"), file.getPath());
    }
  }

  /**
   * The point of the defaultImpl: one unrecognised file used to abort the read of the entire
   * enclosing notification, taking every well-formed file with it.
   */
  @Test
  public void testUnrecognisedFormatDoesNotSinkItsSiblings() throws Exception {
    List<UltimaAnalysisFile> files =
        mapper.readValue(
            "[{\"format\":\"cram\",\"path\":\"file:/a\"},"
                + "{\"format\":\"bam\",\"path\":\"file:/b\"},"
                + "{\"format\":\"metadata\",\"path\":\"file:/c\"}]",
            new TypeReference<List<UltimaAnalysisFile>>() {});

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
    for (Class<? extends AnalysisFile> suite : SUITES) {
      AnalysisFile file = mapper.readValue("{\"format\":\"bam\",\"path\":\"file:/b\"}", suite);
      String serialized = mapper.writeValueAsString(file);
      Assert.assertEquals(serialized, 1, countOccurrences(serialized, "\"format\""));
      Assert.assertTrue(serialized, serialized.contains("\"format\":\"unknown\""));
      Assert.assertEquals("bam", file.getRawFormat());
    }
  }

  private static JsonSubTypes.Type[] subTypes(Class<?> suite) {
    JsonSubTypes annotation = suite.getAnnotation(JsonSubTypes.class);
    Assert.assertNotNull(suite.getSimpleName() + " is missing @JsonSubTypes", annotation);
    return annotation.value();
  }

  /** The suite's {@code UnknownAnalysisFile}, i.e. the defaultImpl of its discriminator. */
  private static Class<?> unknownType(Class<?> suite) {
    JsonTypeInfo annotation = suite.getAnnotation(JsonTypeInfo.class);
    Assert.assertNotNull(suite.getSimpleName() + " is missing @JsonTypeInfo", annotation);
    return annotation.defaultImpl();
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
