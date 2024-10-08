package ca.on.oicr.gsi.runscanner.scanner.processor.dragen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BCLConvert {
  private static final Logger log = LoggerFactory.getLogger(BCLConvert.class);

  public static DragenWorkflowAnalysis process(File rootDir) throws IOException {
    DragenWorkflowAnalysis bclConvertAnalysis = new DragenWorkflowAnalysis();
    // Get fastqs from root Analysis/#/Data/BCLConvert/fastq/Reports/fastq_list.csv
    File fastqList = new File(rootDir, "Data/BCLConvert/fastq/Reports/fastq_list.csv");
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

        Analysis analysis = bclConvertAnalysis.get(fastq[1], lane, index1, index2);
        analysis.setSample(fastq[1]);
        analysis.setLane(Integer.parseInt(fastq[3]));
        analysis.setIndex(index1, index2);

        // TODO: what's it look like when there's only 1 read?
        AnalysisFile file1 = analysisFileFromFilename(rootDir, fastq[4]),
            file2 = analysisFileFromFilename(rootDir, fastq[5]);

        file1.addInfoItem("read_number", 1);
        file2.addInfoItem("read_number", 2);

        analysis.addFile(file1);
        analysis.addFile(file2);

        bclConvertAnalysis.put(analysis);
      }

      // Get file checksums from Analysis/#/Manifest.tsv
      File manifest = new File(rootDir, "Manifest.tsv");
      if (manifest.exists() && manifest.isFile()) {
        List<String[]> manifestLines =
            Files.readAllLines(manifest.toPath())
                .stream()
                .map(line -> line.split("\t"))
                .filter(line -> line[0].startsWith("Data/BCLConvert"))
                .filter(line -> line[0].endsWith(".fastq.gz"))
                .toList();
        for (String[] manifestLine : manifestLines) {
          // 0 = path, 1 = crc32 checksum
          Path filename = new File(rootDir, manifestLine[0]).toPath();

          // When this is null, it's often for an Undetermined read. We do not care.
          try {
            Analysis analysis = bclConvertAnalysis.get(filename);
            for (AnalysisFile file : analysis.getFiles()) {
              if (file.getPath().equals(filename)) {
                file.setChecksum(manifestLine[1]);
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

      // Get read counts from root
      // Analysis/#/Data/BCLConvert/fastq/Reports/Demultiplex_Stats.csv
      File demulitplexStats =
          new File(rootDir, "Data/BCLConvert/fastq/Reports/Demultiplex_Stats.csv");

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
          Analysis analysis = bclConvertAnalysis.get(demuxLine[1], demuxLine[0], demuxLine[2]);
          long readCount = Long.parseLong(demuxLine[3]);
          // Set the same read count for all files in analysis
          analysis.getFiles().forEach(file -> file.addInfoItem("read_count", readCount));
          bclConvertAnalysis.put(analysis);
        }
      } else {
        log.info("No Demultiplex_Stats.csv for {}", rootDir);
      }
    } else {
      log.info("No fastq_list.csv for {}, old DRAGEN version?", rootDir);
    }
    return bclConvertAnalysis;
  }

  private static AnalysisFile analysisFileFromFilename(File rootDir, String fileName)
      throws IOException {
    Path fullPath = Paths.get(rootDir.getPath(), "/Data/BCLConvert/fastq/", fileName);
    AnalysisFile newFile = new AnalysisFile();
    newFile.setPath(fullPath);
    newFile.setSize(Files.size(fullPath));
    // TODO: need checksum
    return newFile;
  }
}
