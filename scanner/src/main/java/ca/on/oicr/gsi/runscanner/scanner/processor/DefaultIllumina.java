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
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Scan an Illumina sequencer's output using the Illumina Interop C++ library.
 *
 * <p>This should work for all sequencers except the Genome Analyzer and Genome Analyzer II.
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

  private static final Predicate<String> BCL_FILENAME =
      Pattern.compile("^(s_[0-9]*_[0-9]*\\.bcl(\\.gz)?|L\\d*_\\d*.cbcl)").asPredicate();

  private static final Predicate<String> BCL_BGZF_FILENAME =
      Pattern.compile("^[0-9]*\\.(bcl\\.bgzf|cbcl)").asPredicate();

  private static final XPath xpath = XPathFactory.newInstance().newXPath();

  // RunInfo XPaths
  private static final XPathExpression RUNINFO_START_TIME_XPATH = xpath("//Run/Date/text()");
  private static final XPathExpression I5_REVERSE_COMPLEMENT =
      xpath("//Run/Reads/Read[@IsIndexedRead='Y'][2]/@IsReverseComplement");

  // RunParameters XPaths
  private static final XPathExpression miSeqPartNumber =
      xpath("//FlowcellRFIDTag/PartNumber/text()");
  private static final XPathExpression nextSeqPartNumber =
      xpath("//FlowCellRfidTag/PartNumber/text()");
  private static final XPathExpression novaSeqPartNum = xpath("//RfidsInfo/FlowCellMode/text()");
  private static final XPathExpression nextSeq2000PartNumber = xpath("//FlowCellPartNumber/text()");
  private static final XPathExpression novaSeqXPartNumber =
      xpath(
          "//ConsumableInfo/ConsumableInfo/Type[text() = 'FlowCell']/parent::*/PartNumber/text()");
  private static final Set<XPathExpression> CONTAINER_PARTNUMBER_XPATHS =
      Collections.unmodifiableSet(
          Sets.newHashSet(
              miSeqPartNumber,
              nextSeqPartNumber,
              novaSeqPartNum,
              nextSeq2000PartNumber,
              novaSeqXPartNumber));
  private static final XPathExpression hiSeqPosition = xpath("//Setup/FCPosition/text()");
  private static final XPathExpression novaSeqPosition = xpath("//Side/text()");
  private static final Set<XPathExpression> POSITION_XPATHS =
      Collections.unmodifiableSet(Sets.newHashSet(hiSeqPosition, novaSeqPosition));;
  private static final XPathExpression FLOWCELL = xpath("//Setup/Flowcell/text()");
  private static final Pattern FLOWCELL_PATTERN =
      Pattern.compile("^([a-zA-Z]+(?: Rapid)?) (Flow Cell v\\d)$");
  private static final XPathExpression FLOWCELL_PAIRED = xpath("//Setup/PairEndFC/text()");
  private static final XPathExpression WORKFLOW_TYPE =
      xpath("//WorkflowType/text()|//ClusteringChoice/text()");
  private static final XPathExpression SBS_CONSUMABLE_VERSION =
      xpath("//RfidsInfo/SbsConsumableVersion/text()");
  private static final XPathExpression RUNPARAM_START_TIME_XPATH = xpath("//RunStartTime/text()");
  private static final XPathExpression RUNPARAM_END_TIME_XPATH = xpath("//RunEndTime/text()");

  // RunCompletionInfo XPaths
  private static final XPathExpression COMPLETION_STATUS_NEXTSEQ =
      xpath("//CompletionStatus/text()");
  private static final XPathExpression COMPLETION_STATUS_NOVASEQ = xpath("//RunStatus/text()");
  private static final XPathExpression START_TIME = xpath("//RunStartTime/text()");
  private static final XPathExpression END_TIME = xpath("//RunEndTime/text()");

  private static XPathExpression xpath(String expression) {
    try {
      return xpath.compile(expression);
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

  private static Optional<HealthType> getHealth(Document runCompletionStatus) {
    // NextSeq 550 pattern
    String status = getValueFromXml(runCompletionStatus, COMPLETION_STATUS_NEXTSEQ);
    if (status != null) {
      if (status.equals("CompletedAsPlanned")) {
        return Optional.of(HealthType.COMPLETED);
      } else {
        log.debug("New Illumina CompletionStatus found: {}", status);
        return Optional.empty();
      }
    }
    // NovaSeq X pattern
    status = getValueFromXml(runCompletionStatus, COMPLETION_STATUS_NOVASEQ);
    if (status != null) {
      if (status.equals("RunCompleted")) {
        return Optional.of(HealthType.COMPLETED);
      } else {
        log.debug("New Illumina RunStatus status found: {}", status);
        return Optional.empty();
      }
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

    // Grab .xml files with information about the run
    final Document runInfo = getXmlDocument(runDirectory, "RunInfo.xml");
    final Document runParameters = getRunParameters(runDirectory);
    final Document runCompletionStatus = getXmlDocument(runDirectory, "RunCompletionStatus.xml");

    if (runParameters != null) {
      // See if we can figure out the chemistry
      dto.setChemistry(
          Arrays.stream(IlluminaChemistry.values())
              .filter(chemistry -> chemistry.test(runParameters))
              .findFirst()
              .orElse(IlluminaChemistry.UNKNOWN));
      dto.setContainerModel(findContainerModel(runParameters));
      dto.setSequencerPosition(findSequencerPosition(runParameters));
      // See if we can figure out the workflow type on the NovaSeq or HiSeq. This
      // mostly
      // tells us how the clustering was done
      final String workflowType = getValueFromXml(runParameters, WORKFLOW_TYPE);
      if (workflowType != null && !workflowType.equals("None")) {
        dto.setWorkflowType(workflowType);
      }
      dto.setIndexSequencing(findIndexSequencing(runInfo, runParameters));
      if (dto.getStartDate() == null) {
        dto.setStartDate(findStartDate(runInfo, runParameters, runCompletionStatus));
      }
    }

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

    // If we have a date failed, use that as completion date
    if (failedDate != null) {
      dto.setHealthType(HealthType.FAILED);
      dto.setCompletionDate(failedDate);
    }

    // This run claims to be complete, but is it really?
    if (dto.getHealthType() == HealthType.COMPLETED) {
      // Maybe a NextSeq wrote a completion status, that we take as authoritative even
      // though it's totally undocumented behaviour.
      // Set updatedHealth to empty depending on if RunCompletionStatus is null
      Optional<HealthType> updatedHealth =
          runCompletionStatus == null ? Optional.empty() : getHealth(runCompletionStatus);

      // If we have RunCompletionStatus.xml, use that to grab the CompletionDate
      if (updatedHealth.isPresent()) {
        completness_method_success.labels("xml").inc();
        updateCompletionDateFromRunCompletionStatus(runDirectory, runCompletionStatus, dto);
      }

      // We don't have RunCompletionStatus.xml file, check other options
      if (!updatedHealth.isPresent() && dto.getNumReads() > 0) {
        // Do we have CopyComplete.txt?
        if (new File(runDirectory, "CopyComplete.txt").exists()) {
          // It's allegedly done.
          updatedHealth = Optional.of(HealthType.COMPLETED);
          completness_method_success.labels("complete.txt").inc();
          updateCompletionDateFromFile(runDirectory, "CopyComplete.txt", dto);
        }
        // Do we have RunEndTime in RunParameters.xml?
        else if (new File(runDirectory, "RunParameters.xml").exists()) {
          // Check if RunEndTime exists and use that for CompletionDate
          if (getValueFromXml(runParameters, RUNPARAM_END_TIME_XPATH) != null) {
            updatedHealth = Optional.of(HealthType.COMPLETED);
            completness_method_success.labels("complete.txt").inc();
            updateCompletionDateFromRunParameters(runDirectory, runParameters, dto);
          }
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
            // This might mean incomplete, or it might mean the sequencer never wrote the
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

  private Document getXmlDocument(File runDirectory, String filename) {
    File file = new File(runDirectory, filename);
    if (file.exists() && file.canRead()) {
      return RunProcessor.parseXml(file).orElse(null);
    }
    return null;
  }

  private Document getRunParameters(File runDirectory) {
    Document document = getXmlDocument(runDirectory, "runParameters.xml");
    if (document == null) {
      document = getXmlDocument(runDirectory, "RunParameters.xml");
    }
    return document;
  }

  private void updateCompletionDateFromRunCompletionStatus(
      File runDirectory, Document runCompletionStatus, IlluminaNotificationDto dto)
      throws IOException {
    Instant timestamp = getInstant(runCompletionStatus, END_TIME);
    if (timestamp != null) {
      dto.setCompletionDate(timestamp);
    } else {
      updateCompletionDateFromFile(runDirectory, "RunCompletionStatus.xml", dto);
    }
  }

  private void updateCompletionDateFromRunParameters(
      File runDirectory, Document runParameters, IlluminaNotificationDto dto) throws IOException {
    Instant timestamp = getInstant(runParameters, RUNPARAM_END_TIME_XPATH);
    if (timestamp != null) {
      dto.setCompletionDate(timestamp);
    } else {
      updateCompletionDateFromFile(runDirectory, "RunParameters.xml", dto);
    }
  }

  private void updateCompletionDateFromFile(
      File runDirectory, String fileName, IlluminaNotificationDto dto) throws IOException {
    if (dto.getCompletionDate() == null) {
      dto.setCompletionDate(
          Files.getLastModifiedTime(new File(runDirectory, fileName).toPath()).toInstant());
    }
  }

  private String findContainerModel(Document runParams) {
    // See if we can figure out the container model
    String partNum = getValueFromXml(runParams, CONTAINER_PARTNUMBER_XPATHS);
    if (partNum != null) {
      return partNum;
    }
    String flowcell = getValueFromXml(runParams, FLOWCELL);
    if (flowcell == null) {
      return null;
    }
    String paired = getValueFromXml(runParams, FLOWCELL_PAIRED);
    Matcher m = FLOWCELL_PATTERN.matcher(flowcell);
    if (paired != null && m.matches()) {
      return m.group(1) + (Boolean.parseBoolean(paired) ? " PE " : " SR ") + m.group(2);
    } else {
      return flowcell;
    }
  }

  private String findSequencerPosition(Document runParams) {
    String position = getValueFromXml(runParams, POSITION_XPATHS);
    position = fixIfHasBadNovaSeqPosition(position);
    return position;
  }

  private IndexSequencing findIndexSequencing(Document runInfo, Document runParams) {
    String i5Reverse = getValueFromXml(runInfo, I5_REVERSE_COMPLEMENT);
    if (Objects.equals(i5Reverse, "Y")) {
      return IndexSequencing.I5_REVERSE_COMPLEMENT;
    } else if (Objects.equals(i5Reverse, "N")) {
      return IndexSequencing.NORMAL;
    }
    try {
      String stringVersion = getValueFromXml(runParams, SBS_CONSUMABLE_VERSION);
      if (stringVersion == null) {
        return null;
      }
      int sbsConsumableVersion = Integer.parseInt(stringVersion);
      return IndexSequencing.getBySbsConsumableVersion(sbsConsumableVersion);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Instant findStartDate(
      Document runInfo, Document runParams, Document runCompletionStatus) {
    // NovaSeq X writes a more accurate start time here after the run completes
    Instant startDate = getInstant(runCompletionStatus, START_TIME);
    if (startDate != null) {
      return startDate;
    }
    startDate = getInstant(runParams, RUNPARAM_START_TIME_XPATH);
    if (startDate == null) {
      startDate = getInstant(runInfo, RUNINFO_START_TIME_XPATH);
    }
    return startDate;
  }

  private static Instant getInstant(Document document, XPathExpression xpath) {
    try {
      String timestamp = getValueFromXml(document, xpath);
      if (timestamp == null) {
        return null;
      }
      return Instant.parse(timestamp);
    } catch (DateTimeParseException e) {
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

  private static String getValueFromXml(Document xml, XPathExpression xpath) {
    String value;
    try {
      value = xpath.evaluate(xml);
      if (isStringEmptyOrNull(value)) {
        return null;
      } else {
        return value;
      }
    } catch (XPathExpressionException e) {
      // ignore
      return null;
    }
  }

  private static String getValueFromXml(Document xml, Collection<XPathExpression> xpaths) {
    return xpaths
        .stream()
        .map(xpath -> getValueFromXml(xml, xpath))
        .filter(Objects::nonNull)
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
