package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class for a sequencer-specific implementation to transform a directory containing a sequencer's
 * output into {@link NotificationDto} results.
 *
 * <p>The behaviour of this class is specific to either a sequencer or group of sequencers. For
 * instance, most Illumina sequencers' output can be processed the same way (except the GA and
 * GAII); therefore, if there is one "default" Illumina processor. This processor is unaware of the
 * MISO sequencer configuration, so the returned object type should match what the
 * platform-type-specific DTO in MISO.
 *
 * <p>It is important that the processor be stateless. The same input may be requested again based
 * on whether the resulting data was marked as incomplete.
 */
public abstract class RunProcessor {
  /** Factory for building a particular {@link RunProcessor} */
  public static final class Builder implements Function<ObjectNode, RunProcessor> {
    private final BiFunction<Builder, ObjectNode, RunProcessor> create;

    private final String name;
    private final Platform platformType;

    public Builder(
        Platform platformType, String name, BiFunction<Builder, ObjectNode, RunProcessor> create) {
      this.platformType = platformType;
      this.name = name;
      this.create = create;
    }

    @Override
    public RunProcessor apply(ObjectNode parameters) {
      return create.apply(this, parameters);
    }

    public String getName() {
      return name;
    }

    public Platform getPlatformType() {
      return platformType;
    }
  }

  private static final Logger log = LoggerFactory.getLogger(RunProcessor.class);

  /**
   * Find the builder that matches the requested parameters.
   *
   * @param pt the platform type of the processor
   * @param name the name of the processor
   * @return a builder if one exists
   */
  public static Optional<Builder> builderFor(Platform pt, String name) {
    return builders()
        .filter(builder -> builder.getPlatformType() == pt && builder.getName().equals(name))
        .findAny();
  }

  /** Produce a stream of all known builders of run processors. */
  public static Stream<Builder> builders() {
    Stream<Builder> standard =
        Stream.of(
            new Builder(Platform.ILLUMINA, "default", DefaultIllumina::create),
            new Builder(Platform.PACBIO, "default", DefaultPacBio::create),
            new Builder(Platform.PACBIO, "v2", V2PacBioProcessor::create),
            new Builder(Platform.OXFORDNANOPORE, "promethion", PromethionProcessor::create),
            new Builder(Platform.OXFORDNANOPORE, "minion", MinionProcessor::create));
    return Stream.concat(
        standard,
        Arrays.stream(Platform.values())
            .map(type -> new Builder(type, "testing", (builder, config) -> new Testing(builder))));
  }

  /** Compile a list of XPath expressions */
  public static XPathExpression[] compileXPath(String... expression) {
    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();
    XPathExpression[] expr = new XPathExpression[expression.length];
    try {
      for (int i = 0; i < expression.length; i++) {
        expr[i] = xpath.compile(expression[i]);
      }
      return expr;
    } catch (XPathExpressionException e) {
      throw new IllegalArgumentException("Failed to compile XPath expression: " + expression, e);
    }
  }

  /** Creates a JSON mapper that is configured to handle the dates in {@link NotificationDto}. */
  public static ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    return mapper;
  }

  /** Attempt to parse naively, and force parsing using UTF-8 if that doesn't work */
  public static Optional<Document> parseXml(File file) {
    try {
      return Optional.of(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file));
    } catch (SAXException e) {
      log.warn(
          "Not really a UTF-16 parsing exception, forcing UTF-8 parsing for {}", file.getPath());

      // Automatically detect BOMs and remove them from input stream
      // BOM characters can interfere with text processing if not properly removed
      try (BOMInputStream bomInputStream = new BOMInputStream(new FileInputStream(file));
          Reader reader = new InputStreamReader(bomInputStream, StandardCharsets.UTF_8)) {
        return Optional.of(
            DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new InputSource(reader)));
      } catch (SAXException | ParserConfigurationException | IOException e2) {
        log.error(
            "Failed to parse XML after forcing UTF-8 encoding for file: {}", file.getPath(), e);
        return Optional.empty();
      }
    } catch (ParserConfigurationException e) {
      log.error("Error in the configuration of the XML parser for file: {}", file.getPath(), e);
      return Optional.empty();
    } catch (IOException e) {
      log.error("IO error when parsing XML content for file: {}", file.getPath(), e);
      return Optional.empty();
    }
  }

  /**
   * Create a run processor for the request configuration, if one exists.
   *
   * @param pt the platform type of the processor
   * @param name the name of the processor
   * @param parameters a JSON object containing any other configuration parameters
   * @return
   */
  public static Optional<RunProcessor> processorFor(
      Platform pt, String name, ObjectNode parameters) {
    return builderFor(pt, name).map(builder -> builder.apply(parameters));
  }

  private final String name;

  private final Platform platformType;

  public RunProcessor(Builder builder) {
    super();
    platformType = builder.getPlatformType();
    name = builder.getName();
  }

  /**
   * This is the name of the sequencer.
   *
   * <p>It serves only to help the user match this processor with the configuration provided.
   */
  public final String getName() {
    return name;
  }

  /**
   * This is the platform type of the sequencer.
   *
   * <p>It serves only to help the user match this processor with the configuration provided. No
   * attempt is made to match the platform-type with the returned DTO.
   */
  public final Platform getPlatformType() {
    return platformType;
  }

  /**
   * Provide the directories containing runs given a user-specified configuration directory.
   *
   * <p>For most sequencer, the runs are the directories immediately under the sequencer's output
   * directory. In other platforms, they may be a subset of those directories or nested further
   * down. This method is to return the appropriate directories that are worth processing.
   *
   * @param root The directory as specified by the user.
   * @return a stream of directories to process
   */
  public abstract Stream<File> getRunsFromRoot(File root);

  /**
   * Read a run directory and compute a result that can be sent to MISO.
   *
   * @param runDirectory the directory to scan (which will be output from {@link
   *     #getRunsFromRoot(File)}
   * @param tz the user-specified timezone that the sequencer exists in
   * @return the DTO result for consumption by MISO; if {@link Platform#isDone} is false, this
   *     directory may be processed again.
   */
  public abstract NotificationDto process(File runDirectory, TimeZone tz) throws IOException;

  /**
   * Get the type of path (File, Directory) this processor requires for runs.
   *
   * @return PathType File or Directory
   */
  public abstract PathType getPathType();

  /**
   * Determine whether a File is readable by the processor.
   *
   * @param filesystemObject File object which may represent a directory or file.
   * @return true if processor can process filesystem object, false otherwise.
   */
  public boolean isFilePathValid(File filesystemObject) {
    boolean valid = false;

    switch (getPathType()) {
      case FILE:
        valid = filesystemObject.isFile();
        break;
      case DIRECTORY:
        valid = filesystemObject.isDirectory();
        break;
    }

    valid = valid && filesystemObject.canExecute() && filesystemObject.canRead();

    return valid;
  }
}
