package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto.SMRTCellPosition;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/** Scan PacBio Revio runs from a directory. */
public class RevioPacBioProcessor extends RunProcessor {

  /** Extract data from an XML metadata file and put it in the DTO. */
  interface ProcessMetadata {

    public void accept(Document document, PacBioNotificationDto dto, TimeZone timeZone)
        throws XPathException;
  }

  private static final Predicate<String> REVIO_CELL_DIRECTORY =
      Pattern.compile("[0-9]_[A-Z][0-9]{2}").asPredicate();

  private static final Predicate<String> TRANSFER_TEST =
      Pattern.compile("Transfer_Test_[0-9]{6}_[0-9]{6}.txt").asPredicate();

  private static final Predicate<String> TRANSFER_DONE =
      Pattern.compile("[a-z][0-9]{5}_[0-9]{6}_[0-9]{6}_s[0-9].transferdone").asPredicate();

  private static final Logger log = LoggerFactory.getLogger(RevioPacBioProcessor.class);

  private static final Pattern RUN_DIRECTORY = Pattern.compile("^.+_\\d+$");

  // Run information extracted from metadata XML file
  private static final RevioPacBioProcessor.ProcessMetadata[] REVIO_METADATA_PROCESSORS =
      new RevioPacBioProcessor.ProcessMetadata[] {
        processString("//RunDetails/TimeStampedName", PacBioNotificationDto::setRunAlias),
        processString(
            "//CollectionMetadata/@InstrumentName", PacBioNotificationDto::setSequencerName),
        processSampleInformation()
      };

  /**
   * Extract a PacBio-formatted string from the metadata file and put the parsed result into the
   * DTO.
   *
   * @param expression the XPath expression yielding the date
   * @param setter the writer for the date
   */
  private static RevioPacBioProcessor.ProcessMetadata processDate(
      String expression, BiConsumer<PacBioNotificationDto, Instant> setter) {
    XPathExpression expr = RunProcessor.compileXPath(expression)[0];
    return (document, dto, timeZone) -> {
      String result = (String) expr.evaluate(document, XPathConstants.STRING);
      if (result != null) {
        setter.accept(
            dto,
            LocalDateTime.parse(result)
                .toInstant(timeZone.toZoneId().getRules().getOffset(LocalDateTime.parse(result))));
      }
    };
  }

  /**
   * Extract a number from the metadata file and put the result into the DTO.
   *
   * @param expression the XPath expression yielding the number
   * @param setter the writer for the number
   */
  private static RevioPacBioProcessor.ProcessMetadata processNumber(
      String expression, BiConsumer<PacBioNotificationDto, Double> setter) {
    XPathExpression expr = RunProcessor.compileXPath(expression)[0];
    return (document, dto, timeZone) -> {
      Double result = (Double) expr.evaluate(document, XPathConstants.NUMBER);
      if (result != null) {
        setter.accept(dto, result);
      }
    };
  }

  // Revio Samples
  private static RevioPacBioProcessor.ProcessMetadata processSampleInformation() {
    XPathExpression[] expr =
        RunProcessor.compileXPath(
            "//ResultsFolder",
            "//SubreadSet/@UniqueId",
            "//WellSample/@Name",
            "//AutomationParameters/AutomationParameter[@Name='MovieLength']/@SimpleValue");
    return (document, dto, timeZone) -> {
      String position =
          StringUtils.substringBetween(
              (String) expr[0].evaluate(document, XPathConstants.STRING), "/", "/");
      String containerSerialNumber = (String) expr[1].evaluate(document, XPathConstants.STRING);
      String poolName = (String) expr[2].evaluate(document, XPathConstants.STRING);
      String movieLength = (String) expr[3].evaluate(document, XPathConstants.STRING);

      // From DefaultPacBio, checking to make sure string is valid
      if (isStringBlankOrNull(position)
          || isStringBlankOrNull(containerSerialNumber)
          || isStringBlankOrNull(poolName)
          || isStringBlankOrNull(movieLength)) {
        return;
      }
      // SMRTCellPosition is a Java Record and represents one SMRT Cell
      SMRTCellPosition containerInfo =
          new SMRTCellPosition(position, containerSerialNumber, poolName, movieLength);

      // Add container to SMRT cell positionList
      List<SMRTCellPosition> tempPositionList = dto.getPositionList();
      if (dto.getPositionList() == null) {
        tempPositionList = new ArrayList<>();
      }
      tempPositionList.add(containerInfo);
      dto.setPositionList(tempPositionList);
    };
  }

  /**
   * Extract a string expression from the metadata file and write it into the DTO.
   *
   * @param expression the XPath expression yielding the string
   * @param setter writer for the string
   * @return
   */
  private static RevioPacBioProcessor.ProcessMetadata processString(
      String expression, BiConsumer<PacBioNotificationDto, String> setter) {
    XPathExpression expr = RunProcessor.compileXPath(expression)[0];
    return (document, dto, timeZone) -> {
      String result = (String) expr.evaluate(document, XPathConstants.STRING);
      if (result != null) {
        setter.accept(dto, result);
      }
    };
  }

  public RevioPacBioProcessor(Builder builder, String address) {
    super(builder);
  }

  public static RunProcessor create(Builder builder, ObjectNode parameters) {
    JsonNode address = parameters.get("address");
    return address.isTextual()
        ? new RevioPacBioProcessor(builder, address.textValue().replaceAll("/+$", ""))
        : null;
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    return Arrays.stream(
        Objects.requireNonNull(
            root.listFiles(f -> f.isDirectory() && RUN_DIRECTORY.matcher(f.getName()).matches())));
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    // We create one DTO for a run, but there are going to be many wells with
    // independent and
    // duplicate metadata that will simply
    // overwrite in the shared DTO. If the data differs, the last well wins.
    PacBioNotificationDto dto = new PacBioNotificationDto();
    dto.setPairedEndRun(false);
    dto.setSequencerFolderPath(runDirectory.getAbsolutePath());

    // Get the number of SMRT Cells in the run directory
    int smrtCellCount =
        (int)
            Arrays.stream(runDirectory.listFiles())
                .filter(
                    cellDirectory ->
                        cellDirectory.isDirectory()
                            && REVIO_CELL_DIRECTORY.test(cellDirectory.getName()))
                .count();
    dto.setLaneCount(smrtCellCount);

    // Get the creation time, earliest Transfer_Test_*.txt indicates the run has started
    Instant startDate =
        getFileCreationTime(getTransferTestFile(runDirectory).orElse(null).toFile());
    dto.setStartDate(startDate);

    // Grab the .metadata.xml and begin processing
    getMetadataDirectory(runDirectory)
        .flatMap(
            metadataDirectory ->
                Stream.of(Objects.requireNonNull(metadataDirectory.listFiles()))
                    .filter(file -> file.getName().endsWith(".metadata.xml")))
        .map(RunProcessor::parseXml)
        .filter(Optional::isPresent)
        .forEach(metadata -> processMetadata(metadata.get(), dto, tz));

    // Check for .transferdone and Transfer_Test in all SMRT Cells to consider the run complete
    if (isRunComplete(runDirectory, smrtCellCount)) {
      // We have all the expected files, the run is considered finished, get the completion time
      dto.setHealthType(HealthType.COMPLETED);
      Optional<File> mostRecentDoneFile =
          getMetadataDirectory(runDirectory)
              .flatMap(
                  metadataDirectory ->
                      Stream.of(
                          Objects.requireNonNull(
                              metadataDirectory.listFiles(
                                  file -> TRANSFER_DONE.test(file.getName())))))
              .max(Comparator.comparing(RevioPacBioProcessor::getFileCreationTime));

      // Set completion time based on most recently created .transferdone file
      dto.setCompletionDate(
          getFileCreationTime(Objects.requireNonNull(mostRecentDoneFile.orElse(null))));
    } else {
      // There are some missing files, the run may not be complete
      dto.setHealthType(HealthType.RUNNING);
    }

    return dto;
  }

  /**
   * Parse a metadata XML file and put all the relevant data into the DTO.
   *
   * @param metadata the path to the XML file
   * @param dto the DTO to update
   */
  private void processMetadata(Document metadata, PacBioNotificationDto dto, TimeZone timeZone) {
    for (RevioPacBioProcessor.ProcessMetadata processor : REVIO_METADATA_PROCESSORS) {
      try {
        processor.accept(metadata, dto, timeZone);
      } catch (XPathException e) {
        log.error("Failed to extract metadata", e);
      }
    }
  }

  /**
   * Tests whether a String is blank (empty or just spaces) or null. Duplicated from MISO's
   * LimsUtils.
   *
   * @param s String to test for blank or null
   * @return true if blank or null String provided
   */
  private static boolean isStringBlankOrNull(String s) {
    return s == null || s.trim().isEmpty();
  }

  @Override
  public PathType getPathType() {
    return PathType.DIRECTORY;
  }

  /**
   * Returns a stream that includes Metadata directory level to be used for further processing
   *
   * @param runDirectory which we are currently processing
   * @return Stream from Metadata directory level
   */
  private Stream<File> getMetadataDirectory(File runDirectory) {
    return Stream.of(Objects.requireNonNull(runDirectory.listFiles()))
        .filter(
            cellDirectory ->
                cellDirectory.isDirectory() && REVIO_CELL_DIRECTORY.test(cellDirectory.getName()))
        .flatMap(
            cellDirectory ->
                Stream.of(Objects.requireNonNull(cellDirectory.listFiles()))
                    .filter(
                        metadataDirectory ->
                            metadataDirectory.isDirectory()
                                && metadataDirectory.getName().equals("metadata")));
  }

  /**
   * Returns the time a file was created
   *
   * @param file which we are currently processing
   * @return Instant point in time
   */
  private static Instant getFileCreationTime(File file) {
    try {
      FileTime fileTime =
          (FileTime) Files.getAttribute(Path.of(file.getAbsolutePath()), "creationTime");
      return fileTime.toInstant();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param runDirectory which we are currently processing
   * @param smrtCellCount total number of SMRT cells in runDirectory
   * @return true or false
   */
  private boolean isRunComplete(File runDirectory, int smrtCellCount) {
    try (Stream<Path> stream = Files.walk(runDirectory.toPath())) {
      List<Path> metadataFiles =
          stream
              .filter(Files::isRegularFile) // Filter regular files
              .filter(file -> TRANSFER_TEST.test(String.valueOf(file.getFileName())))
              .toList();

      return getMetadataDirectory(runDirectory)
                  .flatMap(
                      metadataDirectory ->
                          Arrays.stream(
                              Objects.requireNonNull(
                                  metadataDirectory.listFiles(
                                      file -> TRANSFER_DONE.test(file.getName())))))
                  .count()
              == smrtCellCount
          && metadataFiles.size() == smrtCellCount;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param runDirectory which we are currently processing
   * @return path to earliest created TransferTest
   */
  private Optional<Path> getTransferTestFile(File runDirectory) {
    try (Stream<Path> stream = Files.walk(runDirectory.toPath())) {
      List<Path> transferTestFiles =
          stream
              .filter(Files::isRegularFile) // Filter regular files
              .filter(file -> TRANSFER_TEST.test(String.valueOf(file.getFileName())))
              .toList();

      // Need to take the list of file paths and get the earliest creation time
      return transferTestFiles
          .stream()
          .min(Comparator.comparing(filePath -> getFileCreationTime(filePath.toFile())));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
