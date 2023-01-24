package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ca.on.oicr.gsi.runscanner.dto.type.IlluminaChemistry;
import ca.on.oicr.gsi.runscanner.dto.type.IndexSequencing;
import ca.on.oicr.gsi.runscanner.scanner.LatencyHistogram;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import io.prometheus.client.Counter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Scan an Illumina sequener's output using the Illumina Interop C++ library.
 *
 * <p>This should work for all sequencer execept the Genome Analyzer and Genome Analyzer II.
 */
public final class DefaultIllumina extends RunProcessor {
  private static final LatencyHistogram directory_scan_time =
      new LatencyHistogram(
          "runscanner_illumina_file_completness_time",
          "The time to scan all the output files in a sequencer's directory to tell if it's finished.");

  private static final Counter completness_method_success =
      Counter.build(
              "runscanner_illumina_completness_check",
              "The number of times a method was used to determine a run's completeness after sequencing")
          .labelNames("method")
          .register();

  private static final Pattern FAILED_MESSAGE =
      Pattern.compile(
          "(\\d{1,2}/\\d{1,2}/\\d{4},\\d{2}:\\d{2}:\\d{2}).*Application\\sexited\\sbefore\\scompletion.*");
  private static final DateTimeFormatter FAILED_MESSAGE_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("M/d/yyyy,HH:mm:ss");

  private static final Logger log = LoggerFactory.getLogger(DefaultIllumina.class);

  private static final XPathExpression RUN_COMPLETION_STATUS_EXPRESSION =
      compileXPath("//CompletionStatus")[0];

  private static final Predicate<String> BCL_FILENAME =
      Pattern.compile("^(s_[0-9]*_[0-9]*\\.bcl(\\.gz)?|L\\d*_\\d*.cbcl)").asPredicate();

  private static final Predicate<String> BCL_BGZF_FILENAME =
      Pattern.compile("^[0-9]*\\.(bcl\\.bgzf|cbcl)").asPredicate();

  private static final Set<XPathExpression> CONTAINER_PARTNUMBER_XPATHS;
  private static final Set<XPathExpression> POSITION_XPATHS;

  private static final XPathExpression FLOWCELL;
  private static final Pattern FLOWCELL_PATTERN =
      Pattern.compile("^([a-zA-Z]+(?: Rapid)?) (Flow Cell v\\d)$");
  private static final XPathExpression FLOWCELL_PAIRED;
  private static final XPathExpression WORKFLOW_TYPE;
  private static final XPathExpression SBS_CONSUMABLE_VERSION;
  private static final XPathExpression START_TIME_XPATH;
  private static final XPathExpression I5_REVERSE_COMPLIMENT;

  static {
    XPath xpath = XPathFactory.newInstance().newXPath();
    try {
      WORKFLOW_TYPE = xpath.compile("//WorkflowType/text()|//ClusteringChoice/text()");
      FLOWCELL = xpath.compile("//Setup/Flowcell/text()");
      FLOWCELL_PAIRED = xpath.compile("//Setup/PairEndFC/text()");
      SBS_CONSUMABLE_VERSION = xpath.compile("//RfidsInfo/SbsConsumableVersion/text()");

      XPathExpression miSeqPartNumber = xpath.compile("//FlowcellRFIDTag/PartNumber/text()");
      XPathExpression nextSeqPartNumber = xpath.compile("//FlowCellRfidTag/PartNumber/text()");
      XPathExpression novaSeqPartNum = xpath.compile("//RfidsInfo/FlowCellMode/text()");
      XPathExpression nextSeq2000PartNumber = xpath.compile("//FlowCellPartNumber/text()");
      CONTAINER_PARTNUMBER_XPATHS =
          Collections.unmodifiableSet(
              Sets.newHashSet(
                  miSeqPartNumber, nextSeqPartNumber, novaSeqPartNum, nextSeq2000PartNumber));

      XPathExpression hiSeqPosition = xpath.compile("//Setup/FCPosition/text()");
      XPathExpression novaSeqPosition = xpath.compile("//Side/text()");
      POSITION_XPATHS =
          Collections.unmodifiableSet(Sets.newHashSet(hiSeqPosition, novaSeqPosition));
      START_TIME_XPATH = xpath.compile("//RunStartTime/text()");
      I5_REVERSE_COMPLIMENT =
          xpath.compile("//Run/Reads/Read[@IsIndexedRead='Y'][2]/@IsReverseComplement");
    } catch (XPathExpressionException e) {
      throw new IllegalStateException("Failed to compile xpaths", e);
    }
  }

  public static DefaultIllumina create(Builder builder, ObjectNode parameters) {
    return new DefaultIllumina(builder, calculateCheckOutput(parameters));
  }

  /**
   * Calculates whether or not to check output based on parameter.
   *
   * <p>If checkOutput is not specified (i.e., is null), check output. (If checkOutput is not
   * non-null, return true) If checkOutput is specified, use its value.
   *
   * <p>checkOutput | return ==================== null | T T | T F | F
   *
   * @param parameters ObjectNode possibly containing checkOutput parameter
   * @return true if checkOutput is true or null, false if checkOutput is explicitly false.
   */
  private static boolean calculateCheckOutput(ObjectNode parameters) {
    return !parameters.hasNonNull("checkOutput") || parameters.get("checkOutput").asBoolean();
  }

  private static Optional<HealthType> getHealth(Document document) {
    try {
      String status =
          (String) RUN_COMPLETION_STATUS_EXPRESSION.evaluate(document, XPathConstants.STRING);
      if (status.equals("CompletedAsPlanned")) {
        return Optional.of(HealthType.COMPLETED);
      } else {
        log.debug("New Illumina completion status found: {}", status);
      }
    } catch (XPathExpressionException e) {
      log.error("Failed to evaluate completion status", e);
    }
    return Optional.empty();
  }

  private final boolean checkOutput;

  public DefaultIllumina(Builder builder, boolean checkOutput) {
    super(builder);
    this.checkOutput = checkOutput;
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    return Arrays.stream(root.listFiles(f -> f.isDirectory() && !f.getName().equals("Instrument")));
  }

  private boolean isLaneComplete(Path laneDir, IlluminaNotificationDto dto) {
    // For MiSeq and HiSeq, check for a complete set of BCL files per each cycle
    boolean completeCycleData =
        IntStream.rangeClosed(1, dto.getNumCycles()) //
            .mapToObj(cycle -> String.format("C%d.1", cycle)) //
            .map(laneDir::resolve) //
            .filter(p -> p.toFile().exists()) //
            .allMatch(
                cycleDir -> {
                  try (Stream<Path> cycleWalk = Files.walk(cycleDir, 1)) {

                    long bclCount =
                        cycleWalk //
                            .map(file -> file.getFileName().toString()) //
                            .filter(BCL_FILENAME) //
                            .count();
                    return bclCount == dto.getBclCount();
                  } catch (IOException e) {
                    log.error("Failed to walk lane directory: " + laneDir.toString(), e);
                    return false;
                  }
                });
    if (completeCycleData) {
      return true;
    }
    // For NextSeq, check for a file per cycle
    try (Stream<Path> laneWalk = Files.walk(laneDir, 1)) {
      // First, examine the control files to determine all the BCL files we intend to
      // find for each
      // cycle.
      long bgzfCount =
          laneWalk //
              .map(file -> file.getFileName().toString()) //
              .filter(BCL_BGZF_FILENAME) //
              .count();
      if (bgzfCount == dto.getNumCycles()) {
        return true;
      }
    } catch (IOException e) {
      log.error("Failed to walk lane directory: " + laneDir.toString(), e);
    }
    return false;
  }

  /**
   * Define a Module with custom Instant parsing behaviour to handle datetime strings in a time zone
   * other than UTC.
   *
   * @param tz Time Zone to expect for datetime string
   * @return Module with custom Instant parsing
   */
  private Module setUpCustomModule(TimeZone tz) {
    SimpleModule module = new SimpleModule("customInstantParsingModule");

    module.addSerializer(
        Instant.class,
        new JsonSerializer<Instant>() {
          @Override
          public void serialize(
              Instant instant, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
              throws IOException {
            jsonGenerator.writeString(instant.atZone(tz.toZoneId()).toString());
          }
        });

    module.addDeserializer(
        Instant.class,
        new JsonDeserializer<Instant>() {
          @Override
          public Instant deserialize(
              JsonParser jsonParser, DeserializationContext deserializationContext)
              throws IOException, JsonProcessingException {
            String inststr = jsonParser.getText();
            try {
              return ZonedDateTime.parse(inststr).toInstant();
            } catch (DateTimeParseException dtpe) {
              throw new JsonParseException(jsonParser, "Failed to parse Instant", dtpe);
            }
          }
        });

    return module;
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    // Call the C++ program to do the real work and write a notification DTO to
    // standard output. The
    // C++ object has no direct binding to the
    // DTO, so any changes to the DTO must be manually changed in the C++ code.
    ProcessBuilder builder =
        new ProcessBuilder("nice", "runscanner-illumina", runDirectory.getAbsolutePath())
            .directory(runDirectory)
            .redirectError(Redirect.INHERIT);
    builder.environment().put("TZ", tz.getID());
    Process process = builder.start();

    IlluminaNotificationDto dto;
    int exitcode;
    try (InputStream output = process.getInputStream();
        OutputStream input = process.getOutputStream()) {
      dto =
          new ObjectMapper()
              .registerModule(setUpCustomModule(tz))
              .readValue(output, IlluminaNotificationDto.class);
      dto.setSequencerFolderPath(runDirectory.getAbsolutePath());
    } finally {
      try {
        exitcode = process.waitFor();
      } catch (InterruptedException e) {
        throw new IOException(e);
      } finally {
        process.destroy();
      }
    }

    if (exitcode != 0) {
      throw new IOException(
          "Illumina run processor did not exit cleanly: " + runDirectory.getAbsolutePath());
    }

    final Document runInfo = getRunInfo(runDirectory);

    Stream.of("runParameters.xml", "RunParameters.xml")
        .map(f -> new File(runDirectory, f))
        .filter(file -> file.exists() && file.canRead())
        .findAny()
        .flatMap(RunProcessor::parseXml)
        .ifPresent(
            runParams -> {
              // See if we can figure out the chemistry
              dto.setChemistry(
                  Arrays.stream(IlluminaChemistry.values())
                      .filter(chemistry -> chemistry.test(runParams))
                      .findFirst()
                      .orElse(IlluminaChemistry.UNKNOWN));
              dto.setContainerModel(findContainerModel(runParams));
              dto.setSequencerPosition(findSequencerPosition(runParams));
              // See if we can figure out the workflow type on the NovaSeq or HiSeq. This
              // mostly
              // tells us how the clustering was done
              final String workflowType = findWorkflowType(runParams);
              if (workflowType != null && !workflowType.equals("None")) {
                dto.setWorkflowType(workflowType);
              }
              dto.setIndexSequencing(findIndexSequencing(runInfo, runParams));
              if (dto.getStartDate() == null) {
                dto.setStartDate(findStartDate(runParams));
              }
            });

    // The Illumina library can't distinguish between a failed run and one that
    // either finished or
    // is still going. Scan the logs, if
    // available to determine if the run failed.
    File rtaLogDir = new File(runDirectory, "/Data/RTALogs");
    Instant failedDate =
        Optional.ofNullable(
                rtaLogDir.listFiles(
                    file ->
                        file.getName().endsWith("Log.txt")
                            || file.getName().endsWith("Log_00.txt"))) //
            .map(Arrays::stream) //
            .orElseGet(Stream::empty)
            .map(
                file -> {
                  try (Scanner scanner = new Scanner(file)) {
                    String failMessage = scanner.findWithinHorizon(FAILED_MESSAGE, 0);
                    if (failMessage == null) {
                      return null;
                    }
                    Matcher m = FAILED_MESSAGE.matcher(failMessage);
                    // Somehow, scanner will return things that don't match, so, we check again
                    return m.matches()
                        ? LocalDateTime.parse(m.group(1), FAILED_MESSAGE_DATE_FORMATTER)
                            .atZone(tz.toZoneId())
                            .toInstant()
                        : null;
                  } catch (FileNotFoundException e) {
                    log.error("RTA file vanished before reading", e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .sorted(Instant::compareTo)
            .findFirst()
            .orElse(null);

    if (failedDate != null) {
      dto.setHealthType(HealthType.FAILED);
      dto.setCompletionDate(failedDate);
    }

    // This run claims to be complete, but is it really?
    if (dto.getHealthType() == HealthType.COMPLETED) {
      // Maybe a NextSeq wrote a completion status, that we take as authoritative even
      // though it's
      // totally undocumented behaviour.
      Optional<HealthType> updatedHealth =
          Optional.of(new File(runDirectory, "RunCompletionStatus.xml")) //
              .filter(File::canRead) //
              .flatMap(RunProcessor::parseXml) //
              .flatMap(DefaultIllumina::getHealth);

      if (updatedHealth.isPresent()) {
        completness_method_success.labels("xml").inc();
        updateCompletionDateFromFile(runDirectory, "RunCompletionStatus.xml", dto);
      }

      if (!updatedHealth.isPresent() && dto.getNumReads() > 0) {
        if (new File(runDirectory, "CopyComplete.txt").exists()) {
          // It's allegedly done.
          updatedHealth = Optional.of(HealthType.COMPLETED);
          completness_method_success.labels("complete.txt").inc();
          updateCompletionDateFromFile(runDirectory, "CopyComplete.txt", dto);
        } else {
          // Well, that didn't work. Maybe there are netcopy files.
          long netCopyFiles =
              IntStream.rangeClosed(1, dto.getNumReads()) //
                  .mapToObj(
                      read -> String.format("Basecalling_Netcopy_complete_Read%d.txt", read)) //
                  .map(fileName -> new File(runDirectory, fileName)) //
                  .filter(File::exists) //
                  .count();
          if (netCopyFiles == 0) {
            // This might mean incomplete or it might mean the sequencer never wrote the
            // files
          } else {
            // If we see some net copy files, then it's still running; if they're all here,
            // assume
            // it's done.
            if (netCopyFiles < dto.getNumReads()) {
              updatedHealth = Optional.of(HealthType.RUNNING);
            } else {
              updatedHealth = Optional.of(HealthType.COMPLETED);
              updateCompletionDateFromFile(
                  runDirectory,
                  String.format("Basecalling_Netcopy_complete_Read%d.txt", dto.getNumReads()),
                  dto);
            }
            completness_method_success.labels("netcopy").inc();
          }
        }
      }

      // Check that all the data files have copied. This is a really expensive check,
      // so we let the
      // user disable it.
      if (!updatedHealth.isPresent() && checkOutput) {
        try (AutoCloseable latency = directory_scan_time.start()) {
          Path baseCallDirectory =
              Paths.get(dto.getSequencerFolderPath(), "Data", "Intensities", "BaseCalls");
          // Check that each lane directory is complete
          boolean dataCopied =
              IntStream.rangeClosed(1, dto.getLaneCount()) //
                  .mapToObj(lane -> String.format("L%03d", lane)) //
                  .map(baseCallDirectory::resolve) //
                  .allMatch(laneDir -> isLaneComplete(laneDir, dto));
          if (!dataCopied) {
            updatedHealth = Optional.of(HealthType.RUNNING);
            completness_method_success.labels("dirscan").inc();
          }
        } catch (Exception e) {
          throw new IOException(e);
        }
      }
      updatedHealth.ifPresent(dto::setHealthType);
    }

    return dto;
  }

  private Document getRunInfo(File runDirectory) {
    File runInfoFile = new File(runDirectory, "RunInfo.xml");
    if (runInfoFile.exists() && runInfoFile.canRead()) {
      return RunProcessor.parseXml(runInfoFile).orElse(null);
    }
    return null;
  }

  private void updateCompletionDateFromFile(
      File runDirectory, String fileName, IlluminaNotificationDto dto) throws IOException {
    if (dto.getCompletionDate() == null) {
      dto.setCompletionDate(
          Files.getLastModifiedTime(new File(runDirectory, fileName).toPath())
              .toInstant()
              .atZone(ZoneId.of("Z"))
              .toInstant());
    }
  }

  private String findContainerModel(Document runParams) {
    // See if we can figure out the container model
    String partNum = getValueFromXml(runParams, CONTAINER_PARTNUMBER_XPATHS);
    if (partNum != null) {
      return partNum;
    }
    try {
      String flowcell = FLOWCELL.evaluate(runParams);
      if (isStringEmptyOrNull(flowcell)) {
        return null;
      }
      String paired = FLOWCELL_PAIRED.evaluate(runParams);
      Matcher m = FLOWCELL_PATTERN.matcher(flowcell);
      if (!isStringEmptyOrNull(paired) && m.matches()) {
        return m.group(1) + (Boolean.parseBoolean(paired) ? " PE " : " SR ") + m.group(2);
      } else {
        return flowcell;
      }
    } catch (XPathExpressionException e) {
      // ignore
      return null;
    }
  }

  private String findSequencerPosition(Document runParams) {
    String position = getValueFromXml(runParams, POSITION_XPATHS);
    position = fixIfHasBadNovaSeqPosition(position);
    return position;
  }

  private String findWorkflowType(Document runParams) {
    String workflowType;
    try {
      workflowType = WORKFLOW_TYPE.evaluate(runParams);
    } catch (XPathExpressionException e) {
      workflowType = null;
    }
    return isStringEmptyOrNull(workflowType) ? null : workflowType;
  }

  private IndexSequencing findIndexSequencing(Document runInfo, Document runParams) {
    try {
      String i5Reverse = I5_REVERSE_COMPLIMENT.evaluate(runInfo);
      if (Objects.equals(i5Reverse, "Y")) {
        return IndexSequencing.I5_REVERSE_COMPLEMENT;
      } else if (Objects.equals(i5Reverse, "N")) {
        return IndexSequencing.NORMAL;
      }
    } catch (XPathExpressionException e) {
      // continue to try other source
    }
    try {
      String stringVersion = SBS_CONSUMABLE_VERSION.evaluate(runParams);
      if (isStringEmptyOrNull(stringVersion)) {
        return null;
      }
      int sbsConsumableVersion = Integer.parseInt(stringVersion);
      return IndexSequencing.getBySbsConsumableVersion(sbsConsumableVersion);
    } catch (XPathExpressionException | NumberFormatException e) {
      return null;
    }
  }

  private Instant findStartDate(Document runParams) {
    try {
      String timestamp = START_TIME_XPATH.evaluate(runParams);
      return Instant.parse(timestamp);
    } catch (XPathExpressionException | DateTimeParseException e) {
      return null;
    }
  }

  /**
   * Some earlier NovaSeqs had software which wrote the sides as "Left"/"Right" instead of
   * Illumina's usual "A"/"B". Later versions of the software corrected this, so this standardizes
   * the runscanner position for all NovaSeqs.
   *
   * @param position
   * @return String position
   */
  private String fixIfHasBadNovaSeqPosition(String position) {
    if (position == null) return position;
    if ("Left".equals(position)) return "A";
    if ("Right".equals(position)) return "B";
    return position;
  }

  private static String getValueFromXml(Document xml, Collection<XPathExpression> xpaths) {
    return xpaths
        .stream()
        .map(
            expr -> {
              try {
                return expr.evaluate(xml);
              } catch (XPathExpressionException e) {
                // ignore
                return null;
              }
            })
        .filter(model -> !isStringEmptyOrNull(model))
        .findAny()
        .orElse(null);
  }

  /**
   * Checks if String is empty or null. Borrowed from LimsUtils
   *
   * @param s String to check
   * @return true if String is empty or null
   */
  public static boolean isStringEmptyOrNull(String s) {
    return "".equals(s) || s == null;
  }

  public static <T> Predicate<T> rejectUntil(Predicate<T> check) {
    return new Predicate<T>() {
      private boolean state = false;

      @Override
      public boolean test(T t) {
        if (state) {
          return true;
        }
        state = check.test(t);
        return false;
      }
    };
  }

  @Override
  public PathType getPathType() {
    return PathType.DIRECTORY;
  }
}
