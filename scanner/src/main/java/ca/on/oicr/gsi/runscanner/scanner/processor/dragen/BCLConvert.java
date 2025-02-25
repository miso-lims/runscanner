package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import ca.on.oicr.gsi.runscanner.dto.dragen.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenWorkflowRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.FastqAnalysisFile;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.Samplesheet;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.SamplesheetBCLConvertSection;
import ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet.SamplesheetBCLConvertSection.SamplesheetBCLConvertDataEntry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCLConvert {
  private static final Logger log = LoggerFactory.getLogger(BCLConvert.class);

  public static DragenWorkflowRun process(Samplesheet samplesheet, File rootDir)
      throws IOException {
    DragenWorkflowRun bclConvertWorkflowRun = new DragenWorkflowRun("BCLConvert");
    bclConvertWorkflowRun.setSoftwareVersion(
        ((SamplesheetBCLConvertSection) samplesheet.getByName("BCLConvert"))
            .getSoftwareVersion()
            .toString());

    // Get start and end time from root/Analysis/#/Data/b2c_dragen_events.csv
    File b2cEvents = new File(rootDir, "Data/b2c_dragen_events.csv");
    if (b2cEvents.exists() && b2cEvents.isFile()) {
      List<String[]> b2cLines =
          Files.readAllLines(b2cEvents.toPath()).stream().map(line -> line.split(",")).toList();
      boolean start = true;

      // Line format: 0 = time, 1 = label (we don't need the label for anything)
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      for (String[] event : b2cLines) {
        if (event[0].startsWith("time")) continue; // skip the column label line
        // Assuming the file uses timestamps in the timezone of the machine already,
        // no offset actually needed
        Instant instantDateTime =
            LocalDateTime.parse(event[0], dateTimeFormatter).atOffset(ZoneOffset.UTC).toInstant();
        if (start) {
          bclConvertWorkflowRun.setStartTime(instantDateTime);
          start = false;
        }
        bclConvertWorkflowRun.setCompletionTime(instantDateTime);
      }
    } else {
      log.info("No b2c_dragen_events.csv for {}", rootDir);
    }

    // Get fastq list
    // For gz compression, root/Analysis/#/Data/BCLConvert/fastq/Reports/fastq_list.csv
    // For ora compression, root/Analysis/#/Data/BCLConvert/ora_fastq/Reports/fastq_list.csv
    File fastqList = new File(rootDir, "Data/BCLConvert/fastq/Reports/fastq_list.csv");
    if (!(fastqList.exists() && fastqList.isFile())) {
      fastqList = new File(rootDir, "Data/BCLConvert/ora_fastq/Reports/fastq_list.csv");
    }
    if (fastqList.exists() && fastqList.isFile()) {
      List<String[]> fastqLines =
          Files.readAllLines(fastqList.toPath()).stream().map(line -> line.split(",")).toList();
      for (String[] fastq : fastqLines) {
        if (fastq[0].startsWith("RGID")) continue; // Skip the column label line
        // 0 = RGID, 1 = RGSM, 2 = RGLB, 3 = Lane, 4 = Read1File, 5 = Read2File
        // get index by teasing apart RGID, it looks to be index1.index2.lane
        String[] splitRgid = fastq[0].split("\\.");
        String index1 = splitRgid[0], index2 = splitRgid[1];
        int lane = Integer.parseInt(splitRgid[2]);

        DragenAnalysisUnit dragenAnalysisUnit =
            bclConvertWorkflowRun.get(fastq[1], lane, index1, index2);
        if (dragenAnalysisUnit == null) {
          dragenAnalysisUnit = new DragenAnalysisUnit();
        }
        dragenAnalysisUnit.setSample(fastq[1]);
        dragenAnalysisUnit.setLane(Integer.parseInt(fastq[3]));
        dragenAnalysisUnit.setIndex1(index1);
        dragenAnalysisUnit.setIndex2(index2);

        // TODO: what's it look like when there's only 1 read?
        AnalysisFile file1 = fastqFromFilename(rootDir, fastq[4], 1),
            file2 = fastqFromFilename(rootDir, fastq[5], 2);

        if (file1 != null) dragenAnalysisUnit.addFile(file1);
        if (file2 != null) dragenAnalysisUnit.addFile(file2);

        bclConvertWorkflowRun.put(dragenAnalysisUnit);
      }

      // Get file checksums from Analysis/#/Manifest.tsv
      File manifest = new File(rootDir, "Manifest.tsv");
      if (manifest.exists() && manifest.isFile()) {
        List<String[]> manifestLines =
            Files.readAllLines(manifest.toPath())
                .stream()
                .map(line -> line.split("\t"))
                .filter(line -> line[0].startsWith("Data/BCLConvert"))
                .filter(line -> (line[0].endsWith(".fastq.gz") || line[0].endsWith(".fastq.ora")))
                .toList();
        for (String[] manifestLine : manifestLines) {
          // 0 = path, 1 = crc32 checksum
          Path filename = new File(rootDir, manifestLine[0]).toPath();

          // When this is null, it's often for an Undetermined read. We do not care.
          DragenAnalysisUnit dragenAnalysisUnit = bclConvertWorkflowRun.get(filename);
          if (dragenAnalysisUnit == null) {
            log.info("Unable to map {} to an Analysis object", filename);
          } else {
            for (AnalysisFile file : dragenAnalysisUnit.getFiles()) {
              if (file.getPath().equals(filename)) {
                file.setCrc32Checksum(manifestLine[1]);
                break;
              }
            }
            bclConvertWorkflowRun.put(dragenAnalysisUnit);
          }
        }
      } else {
        log.info("No Manifest.tsv for {}", rootDir);
      }

      // Get read counts from Demultiplex Stats file
      // Many potential locations for this file based on settings and DRAGEN version
      List<File> potentialDemultiplexStatsLocations =
          List.of(
              new File(rootDir, "Data/BCLConvert/fastq/Reports/Demultiplex_Stats.csv"),
              new File(rootDir, "Data/BCLConvert/ora_fastq/Reports/Demultiplex_Stats.csv"),
              new File(rootDir, "Data/Demux/Demultiplex_Stats.csv"));
      File demulitplexStats = null;

      for (File potentialDemultiplexStats : potentialDemultiplexStatsLocations) {
        if (potentialDemultiplexStats.exists() && potentialDemultiplexStats.isFile()) {
          demulitplexStats = potentialDemultiplexStats;
          break;
        }
      }

      if (demulitplexStats != null && demulitplexStats.exists() && demulitplexStats.isFile()) {
        List<String[]> demultiplexLines =
            Files.readAllLines(demulitplexStats.toPath())
                .stream()
                .map(line -> line.split(","))
                .toList();

        for (String[] demuxLine : demultiplexLines) {
          if (demuxLine[0].startsWith("Lane")) continue; // skip the column labels
          if (demuxLine[1].equals("Undetermined")) continue; // skip Undetermined reads
          // 0 = Lane, 1 = SampleId, 2 = Index, 3= # Reads, 4 = # Perfect Index Reads,
          // 5 = # One Mismatch Index Reads, 6 = # Two Mismatch Index Reads, 7 = % Reads,
          // 8 = % Perfect Index Reads, 9 = % One Mismatch Index Reads,
          // 10 = % Two Mismatch Index Reads
          // TODO: given the wwwwwwww-wwwwwwww format of this file, what does only having index2
          // look like?
          String[] indices = demuxLine[2].split("-");
          DragenAnalysisUnit dragenAnalysisUnit;
          if (indices.length == 2) {
            dragenAnalysisUnit =
                bclConvertWorkflowRun.get(demuxLine[1], demuxLine[0], indices[0], indices[1]);
          } else if (indices.length == 1) {
            dragenAnalysisUnit =
                bclConvertWorkflowRun.get(demuxLine[1], demuxLine[0], indices[0], null);
          } else {
            throw new IllegalStateException(
                "Demux indices length for " + demuxLine[1] + " is " + indices.length);
          }
          if (dragenAnalysisUnit == null) dragenAnalysisUnit = new DragenAnalysisUnit();
          int readCount = Integer.parseInt(demuxLine[3]);
          // Set the same read count for all files in analysis
          dragenAnalysisUnit
              .getFiles()
              .forEach(file -> ((FastqAnalysisFile) file).setReadCount(readCount));
          bclConvertWorkflowRun.put(dragenAnalysisUnit);
        }
      } else {
        log.info("No Demultiplex_Stats.csv for {}", rootDir);
      }
    } else {
      log.info("No fastq_list.csv for {}", rootDir);
    }

    // Sanity check against samplesheet and that no dummy values are still present
    boolean isOk = true;

    // start and completed time should be set and
    // start time should not be later than the completion time
    if (bclConvertWorkflowRun.getStartTime() == null
        || bclConvertWorkflowRun.getCompletionTime() == null
        || bclConvertWorkflowRun.getStartTime().compareTo(bclConvertWorkflowRun.getCompletionTime())
            > 0) {
      isOk = false;
    }
    for (SamplesheetBCLConvertDataEntry item :
        ((SamplesheetBCLConvertSection) samplesheet.getByName("BCLConvert")).getData()) {
      DragenAnalysisUnit dragenAnalysisUnitItem =
          bclConvertWorkflowRun.get(
              item.getSampleId(),
              item.getLane(),
              item.getIndex(),
              reverseComplement(item.getIndex2()));
      if (dragenAnalysisUnitItem == null
          || dragenAnalysisUnitItem.isEmpty()
          || dragenAnalysisUnitItem.getFiles().isEmpty()) {
        isOk = false;
        break;
      } else {
        // Unfortunately there is no better source for OverrideCycles
        dragenAnalysisUnitItem.setOverrideCycles(item.getOverrideCycles());
        for (AnalysisFile itemFile : dragenAnalysisUnitItem.getFiles()) {
          if (itemFile.getPath() == null
              || itemFile.getPath().toString().isBlank()
              || itemFile.getCrc32Checksum() == null
              || itemFile.getCrc32Checksum().isBlank()
              || ((FastqAnalysisFile) itemFile).getReadCount() == null
              || ((FastqAnalysisFile) itemFile).getReadNumber() == null) {
            isOk = false;
            break;
          }
        }
      }
    }
    if (isOk) bclConvertWorkflowRun.complete();
    return bclConvertWorkflowRun;
  }

  private static AnalysisFile fastqFromFilename(File rootDir, String fileName, int readNumber)
      throws IOException {
    if (fileName == null || fileName.isBlank()) {
      return null;
    }
    // 4.1.7 fileName from fastq report looks like 'MY_FILE.fastq.gz'
    // 4.3 fileName looks like '../MY_FILE.fastq.gz'
    // So we have to interpret the string as a path and get the last element
    Path fullPath =
        Paths.get(
            rootDir.getPath(),
            "/Data/BCLConvert/fastq/",
            Path.of(fileName).getFileName().toString());
    if (!Files.exists(fullPath)) {
      fullPath = Paths.get(rootDir.getPath(), "/Data/BCLConvert/ora_fastq/", fileName);
    }
    if (!Files.exists(fullPath)) return null;
    FastqAnalysisFile newFile = new FastqAnalysisFile();
    newFile.setPath(fullPath);
    newFile.setSize(Files.size(fullPath));
    newFile.setCreatedTime(Files.getLastModifiedTime(fullPath).toInstant());
    newFile.setModifiedTime(
        newFile.getCreatedTime()); // Not perfect but f/s won't give us a created time
    newFile.setReadNumber(readNumber);
    return newFile;
  }

  private static char complement(char nt) {
    switch (nt) {
      case 'A':
        return 'T';
      case 'C':
        return 'G';
      case 'G':
        return 'C';
      case 'T':
      case 'U':
        return 'A';
        // Below are all the degenerate nucleotides. I hope we never need these and if we had one,
        // the index
        // mismatches calculations would have
        // to be the changed.
      case 'R': // AG
        return 'Y';
      case 'Y': // CT
        return 'R';
      case 'S': // CG
        return 'S';
      case 'W': // AT
        return 'W';
      case 'K': // GT
        return 'M';
      case 'M': // AC
        return 'K';
      case 'B': // CGT
        return 'V';
      case 'D': // AGT
        return 'H';
      case 'H': // ACT
        return 'D';
      case 'V': // ACG
        return 'B';
      case 'N':
        return 'N';
      default:
        return nt;
    }
  }

  public static String reverseComplement(String index) {
    if (index == null) {
      return null;
    }
    final StringBuilder buffer = new StringBuilder(index.length());
    for (int i = index.length() - 1; i >= 0; i--) {
      buffer.append(complement(Character.toUpperCase(index.charAt(i))));
    }
    return buffer.toString();
  }
}
