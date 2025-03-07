package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto.SMRTCellPosition;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
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

    void accept(Document document, PacBioNotificationDto dto, TimeZone timeZone)
        throws XPathException;
  }

  private static final Predicate<String> REVIO_CELL_DIRECTORY =
      Pattern.compile("[0-9]_[A-Z][0-9]{2}").asPredicate();

  private static final Predicate<String> TRANSFER_TEST =
      Pattern.compile("Transfer_Test_[0-9]{6}_[0-9]{6}.txt").asPredicate();

  private static final Predicate<String> TRANSFER_DONE =
      Pattern.compile("[a-z][0-9]{5}_[0-9]{6}_[0-9]{6}_s[0-9].transferdone").asPredicate();

  private static final Predicate<String> PB_REPORT_LOG =
      Pattern.compile("[a-z][0-9]{5}_[0-9]{6}_[0-9]{6}_s[0-9].pbreports.log").asPredicate();

  private static final Logger log = LoggerFactory.getLogger(RevioPacBioProcessor.class);

  private static final Pattern RUN_DIRECTORY = Pattern.compile("^.+_\\d+$");

  // Run information extracted from metadata XML file
  private static final RevioPacBioProcessor.ProcessMetadata[] REVIO_METADATA_PROCESSORS =
      new RevioPacBioProcessor.ProcessMetadata[] {
        processString("//RunDetails/TimeStampedName", PacBioNotificationDto::setRunAlias),
        processString(
            "//CollectionMetadata/@InstrumentName", PacBioNotificationDto::setSequencerName),
        processDate("//Run/@WhenStarted", PacBioNotificationDto::setStartDate),
        processString(
            "//VersionInfo[@Name='smrtlink']/@Version", PacBioNotificationDto::setSoftware),
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
      String date = (String) expr.evaluate(document, XPathConstants.STRING);
      if (date != null) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        ZonedDateTime parsedDateTime = ZonedDateTime.parse(date, formatter);
        setter.accept(dto, parsedDateTime.toInstant());
      }
    };
  }

  // Revio Samples
  private static RevioPacBioProcessor.ProcessMetadata processSampleInformation() {
    XPathExpression[] expr =
        RunProcessor.compileXPath(
            "//ResultsFolder",
            "//SubreadSet/@UniqueId",
            "//CellPac/@PartNumber",
            "//WellSample/@Name",
            "//AutomationParameters/AutomationParameter[@Name='MovieLength']/@SimpleValue");
    return (document, dto, timeZone) -> {
      String position =
          StringUtils.substringBetween(
              (String) expr[0].evaluate(document, XPathConstants.STRING), "/", "/");
      String containerSerialNumber = (String) expr[1].evaluate(document, XPathConstants.STRING);
      String smrtCellContainerModel = (String) expr[2].evaluate(document, XPathConstants.STRING);
      String poolName = (String) expr[3].evaluate(document, XPathConstants.STRING);
      String movieLength = (String) expr[4].evaluate(document, XPathConstants.STRING);

      // SMRTCellPosition is a Java Record and represents one SMRT Cell
      SMRTCellPosition containerInfo =
          new SMRTCellPosition(
              position, containerSerialNumber, smrtCellContainerModel, poolName, movieLength);

      // Add container to SMRT cell positionList
      List<SMRTCellPosition> tempPositionList = dto.getSmrtCellPositionList();
      if (dto.getSmrtCellPositionList() == null) {
        tempPositionList = new ArrayList<>();
      }
      tempPositionList.add(containerInfo);
      dto.setSmrtCellPositionList(tempPositionList);
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

  public RevioPacBioProcessor(Builder builder) {
    super(builder);
  }

  public static RunProcessor create(Builder builder, ObjectNode parameters) {
    return new RevioPacBioProcessor(builder);
  }

  @Override
  public Stream<File> getRunsFromRoot(File root) {
    return Arrays.stream(
        root.listFiles(f -> f.isDirectory() && RUN_DIRECTORY.matcher(f.getName()).matches()));
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

    // Grab the .metadata.xml and begin processing
    streamSmrtCellSubdirectories(runDirectory, "metadata")
        .flatMap(
            metadataDirectory ->
                Stream.of(metadataDirectory.listFiles())
                    .filter(
                        file ->
                            file.getName()
                                .matches("^([A-Za-z0-9]+(_[A-Za-z0-9]+){3})\\.metadata\\.xml$")))
        .map(RunProcessor::parseXml)
        .filter(Optional::isPresent)
        .forEach(metadata -> processMetadata(metadata.get(), dto, tz));

    // Check if we grabbed a run alias from metadata.xml
    if (dto.getRunAlias() != null) {
      // Validate that it matches with run alias being the run directory name
      if (!runDirectory.getName().equals(dto.getRunAlias())) {
        dto.setRunAlias(null);
      }
    } else {
      // Unable to grab run alias from metadata.xml set it using run directory name
      dto.setRunAlias(runDirectory.getName());
    }

    // We don't have a start date from metadata, fallback to Transfer_Test file creation time
    if (dto.getStartDate() == null) {
      dto.setStartDate(startTimeFromTransferTest(runDirectory));
    }

    // Check for .transferdone and Transfer_Test in all SMRT Cells
    // to consider the run complete
    if (isRunComplete(runDirectory, smrtCellCount)) {
      dto.setHealthType(HealthType.COMPLETED);

      // Check if pbereport.log present and use that for completion time
      Optional<Instant> latestCompletionTime =
          streamSmrtCellSubdirectories(runDirectory, "statistics")
              .flatMap(
                  statisticsDirectory ->
                      Stream.of(
                          statisticsDirectory.listFiles(
                              file -> PB_REPORT_LOG.test(file.getName()))))
              .map(RevioPacBioProcessor::getLogCompletionTime)
              .max(Comparator.naturalOrder());
      // Set completion time based on pbereports.log file
      latestCompletionTime.ifPresent(dto::setCompletionDate);

      // If we don't have the pbereport.log, we'll have to fallback using .transferdone file
      // creation time instead
      if (dto.getCompletionDate() == null) {
        dto.setCompletionDate(completionTimeFromTransferDone(runDirectory));
      }
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

  @Override
  public PathType getPathType() {
    return PathType.DIRECTORY;
  }

  /**
   * Returns a stream that includes Metadata directory level to be used for further processing
   *
   * @param runDirectory which we are currently processing
   * @param directoryName specific sub-directory we want
   * @return Stream from Metadata directory level
   */
  private Stream<File> streamSmrtCellSubdirectories(File runDirectory, String directoryName) {
    return Stream.of(runDirectory.listFiles())
        .filter(
            cellDirectory ->
                cellDirectory.isDirectory() && REVIO_CELL_DIRECTORY.test(cellDirectory.getName()))
        .flatMap(
            cellDirectory ->
                Stream.of(cellDirectory.listFiles())
                    .filter(
                        subDirectory ->
                            subDirectory.isDirectory()
                                && subDirectory.getName().equals(directoryName)));
  }

  /**
   * Returns the time a file was created
   *
   * @param file which we are currently processing
   * @return Instant point in time
   */
  private static Instant getFileCreationTime(File file) throws IOException {
    FileTime fileTime =
        (FileTime) Files.getAttribute(Path.of(file.getAbsolutePath()), "creationTime");
    return fileTime.toInstant();
  }

  /**
   * Grab the completion time from a log file
   *
   * @param file we are checking
   * @return completion time
   */
  private static Instant getLogCompletionTime(File file) {
    try {
      Scanner myReader = new Scanner(file);
      Pattern pattern =
          Pattern.compile("^\\[INFO] (.*) \\[.*] exiting with return code \\d+" + " .*");
      while (myReader.hasNextLine()) {
        Matcher matcher = pattern.matcher(myReader.nextLine());
        if (matcher.matches()) {
          String stringDate = matcher.group(1);
          String datePattern = "yyyy-MM-dd HH:mm:ss,SSSXX";
          DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePattern);
          return LocalDateTime.parse(stringDate, dateTimeFormatter)
              .atOffset(ZoneOffset.UTC)
              .toInstant();
        }
      }
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("This file is expected to exist.");
    }
    return null;
  }

  /**
   * @param runDirectory which we are currently processing
   * @param smrtCellCount total number of SMRT cells in runDirectory
   * @return true or false
   */
  private boolean isRunComplete(File runDirectory, int smrtCellCount) throws IOException {
    try (Stream<Path> testFileStream = Files.walk(runDirectory.toPath());
        Stream<Path> testDoneStream = Files.walk(runDirectory.toPath())) {
      int transferTestFiles =
          (int)
              testFileStream
                  .filter(Files::isRegularFile)
                  .filter(file -> TRANSFER_TEST.test(String.valueOf(file.getFileName())))
                  .count();

      int transferDoneFiles =
          (int)
              testDoneStream
                  .filter(Files::isRegularFile)
                  .filter(file -> TRANSFER_DONE.test(String.valueOf(file.getFileName())))
                  .count();

      return transferDoneFiles == smrtCellCount && transferTestFiles == smrtCellCount;
    }
  }

  /**
   * Filter runDirectory for Transfer_Test files and grab earliest creation time
   *
   * @param runDirectory which we are currently processing
   * @return earliest file creation time or null if no Transfer_Test files are found
   */
  private Instant startTimeFromTransferTest(File runDirectory) throws IOException {
    List<File> TransferTestFiles =
        streamSmrtCellSubdirectories(runDirectory, "metadata")
            .flatMap(
                metadataDirectory ->
                    Stream.of(
                        metadataDirectory.listFiles(file -> TRANSFER_TEST.test(file.getName()))))
            .toList();

    Instant minInstant = null;
    for (File file : TransferTestFiles) {
      Instant creationTime = getFileCreationTime(file);
      if (minInstant == null || creationTime.isBefore(minInstant)) {
        minInstant = creationTime;
      }
    }
    return minInstant;
  }

  /**
   * Filter runDirectory for .Transferdone file and grab latest creation time
   *
   * @param runDirectory which we are currently processing
   * @return latest file creation time or null if no Transferdone files are found
   */
  private Instant completionTimeFromTransferDone(File runDirectory) throws IOException {
    List<File> TransferDoneFiles =
        streamSmrtCellSubdirectories(runDirectory, "metadata")
            .flatMap(
                metadataDirectory ->
                    Stream.of(
                        metadataDirectory.listFiles(file -> TRANSFER_DONE.test(file.getName()))))
            .toList();

    Instant maxInstant = null;
    for (File file : TransferDoneFiles) {
      Instant creationTime = getFileCreationTime(file);
      if (maxInstant == null || creationTime.isAfter(maxInstant)) {
        maxInstant = creationTime;
      }
    }
    return maxInstant;
  }
}
