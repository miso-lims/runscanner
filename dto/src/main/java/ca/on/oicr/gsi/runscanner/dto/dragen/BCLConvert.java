package ca.on.oicr.gsi.runscanner.dto.dragen;

import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection.SamplesheetBCLConvertDataEntry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCLConvert {
  private static final Logger log = LoggerFactory.getLogger(BCLConvert.class);
  private final Samplesheet samplesheet;
  private final File rootDir;
  private DragenWorkflowAnalysis result;
  private boolean isOk = false;

  public BCLConvert(Samplesheet sheet, File dir) throws IOException {
    this.samplesheet = sheet;
    this.rootDir = dir;
    process();
  }

  public boolean isOk() {
    return isOk;
  }

  public DragenWorkflowAnalysis getResult() throws IOException {
    if (result == null) process();
    return result;
  }

  private void process() throws IOException {
    DragenWorkflowAnalysis bclConvertAnalysis = new DragenWorkflowAnalysis();
    bclConvertAnalysis.setStartTime(samplesheet.getMtime());
    Instant max_date = Instant.MIN; // yes you read that right
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
            bclConvertAnalysis.get(fastq[1], lane, index1, index2);
        dragenAnalysisUnit.setSample(fastq[1]);
        dragenAnalysisUnit.setLane(Integer.parseInt(fastq[3]));
        dragenAnalysisUnit.setIndex(index1, index2);

        // TODO: what's it look like when there's only 1 read?
        AnalysisFile file1 = analysisFileFromFilename(rootDir, fastq[4], 1),
            file2 = analysisFileFromFilename(rootDir, fastq[5], 2);

        if (file1 != null && file1.getModified().compareTo(max_date) > 0)
          max_date = file1.getModified();

        if (file2 != null && file2.getModified().compareTo(max_date) > 0)
          max_date = file2.getModified();

        dragenAnalysisUnit.addFile(file1);
        dragenAnalysisUnit.addFile(file2);

        bclConvertAnalysis.put(dragenAnalysisUnit);
      }
      bclConvertAnalysis.setCompletionTime(max_date);

      // Get file checksums from Analysis/#/Manifest.tsv
      // TODO: Doesn't seem to exist for BCLConvert < 4.1.7 - skip?
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
          try {
            DragenAnalysisUnit dragenAnalysisUnit = bclConvertAnalysis.get(filename);
            for (AnalysisFile file : dragenAnalysisUnit.getFiles()) {
              if (file.getPath().equals(filename)) {
                file.setCrc32Checksum(manifestLine[1]);
                break;
              }
            }
          } catch (NullPointerException npe) {
            log.info("Unable to map {} to an Analysis object", filename);
          }
        }

      } else {
        log.info("No Manifest.tsv for {}", rootDir);
      }

      // Get read counts from Demultiplex Stats file
      // for gz compression: root/Analysis/#/Data/BCLConvert/fastq/Reports/Demultiplex_Stats.csv
      // for ora compression:
      // root/Analysis/#/Data/BCLConvert/ora_fastq/Reports/Demultiplex_Stats.csv
      File demulitplexStats =
          new File(rootDir, "Data/BCLConvert/fastq/Reports/Demultiplex_Stats.csv");
      if (!(demulitplexStats.exists() && demulitplexStats.isFile())) {
        demulitplexStats =
            new File(rootDir, "Data/BCLConvert/ora_fastq/Reports/Demultiplex_Stats.csv");
      }

      if (demulitplexStats.exists() && demulitplexStats.isFile()) {
        List<String[]> demultiplexLines =
            Files.readAllLines(demulitplexStats.toPath())
                .stream()
                .map(line -> line.split(","))
                .toList();

        for (String[] demuxLine : demultiplexLines) {
          if (demuxLine[0].startsWith("Lane")) continue; // skip the column labels
          // 0 = Lane, 1 = SampleId, 2 = Index, 3= # Reads, 4 = # Perfect Index Reads,
          // 5 = # One Mismatch Index Reads, 6 = # Two Mismatch Index Reads, 7 = % Reads,
          // 8 = % Perfect Index Reads, 9 = % One Mismatch Index Reads,
          // 10 = % Two Mismatch Index Reads
          DragenAnalysisUnit dragenAnalysisUnit =
              bclConvertAnalysis.get(demuxLine[1], demuxLine[0], demuxLine[2]);
          long readCount = Long.parseLong(demuxLine[3]);
          // Set the same read count for all files in analysis
          dragenAnalysisUnit.getFiles().forEach(file -> file.addInfoItem("read_count", readCount));
          bclConvertAnalysis.put(dragenAnalysisUnit);
        }
      } else {
        log.info("No Demultiplex_Stats.csv for {}", rootDir);
      }
    } else {
      log.info("No fastq_list.csv for {}", rootDir);
    }

    // Check against samplesheet for okayness
    isOk = true;
    for (SamplesheetBCLConvertDataEntry item :
        ((SamplesheetBCLConvertSection) samplesheet.getByName("BCLConvert")).getData()) {
      DragenAnalysisUnit dragenAnalysisUnitItem =
          bclConvertAnalysis.get(
              item.getSampleId(), item.getLane(), item.getIndex(), item.getIndex2());
      if (dragenAnalysisUnitItem == null || dragenAnalysisUnitItem.isEmpty()) {
        isOk = false;
        break;
      } else {
        for (AnalysisFile itemFile : dragenAnalysisUnitItem.getFiles()) {
          if (itemFile == null
              || itemFile.getPath() == null
              || itemFile.getPath().toString().isBlank()
              || itemFile.getInfo() == null
              || itemFile.getCrc32Checksum() == null
              || itemFile.getCrc32Checksum().isBlank()) {
            isOk = false;
            break;
          }
        }
      }
    }

    result = bclConvertAnalysis;
  }

  private static AnalysisFile analysisFileFromFilename(
      File rootDir, String fileName, int readNumber) throws IOException {
    Path fullPath = Paths.get(rootDir.getPath(), "/Data/BCLConvert/fastq/", fileName);
    if (!Files.exists(fullPath)) {
      fullPath = Paths.get(rootDir.getPath(), "/Data/BCLConvert/ora_fastq/", fileName);
    }
    if (!Files.exists(fullPath)) return null;
    AnalysisFile newFile = new AnalysisFile();
    newFile.setPath(fullPath);
    newFile.setSize(Files.size(fullPath));
    newFile.setCreated(Files.getLastModifiedTime(fullPath).toInstant());
    newFile.setModified(newFile.getCreated()); // Not perfect but f/s won't give us a created time
    newFile.addInfoItem("read_number", readNumber);
    return newFile;
  }
}
