package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaDragenNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.AnalysisStatus;
import ca.on.oicr.gsi.runscanner.dto.type.DRAGENWorkflow;
import ca.on.oicr.gsi.runscanner.scanner.processor.NovaseqXProcessor.BCLConvertAnalysis.Analysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaseqXProcessor extends DefaultIllumina {
  static class BCLConvertAnalysis {
    List<Analysis> analyses = new LinkedList<>();

    static class Analysis {
      String sample;
      int lane;
      String read1File, read2File;
      long readCount;

      boolean isEmpty() {
        return sample == null
            && lane == 0
            && read1File == null
            && read2File == null
            && readCount == 0;
      }

      ObjectNode toJson() {
        ObjectNode analysisJson = MAPPER.createObjectNode();
        analysisJson.put("Sample", sample);
        analysisJson.put("Lane", lane);
        analysisJson.put("Read1File", read1File);
        analysisJson.put("Read2File", read2File);
        analysisJson.put("ReadCount", readCount);
        return analysisJson;
      }
    }

    Analysis get(String sample, String lane) {
      return get(sample, Integer.parseInt(lane));
    }

    Analysis get(String sample, int lane) {
      return analyses
          .stream()
          .filter(a -> a.sample.equals(sample) && a.lane == lane)
          .findFirst()
          .orElse(new Analysis());
    }

    void put(Analysis newAnalysis) {
      Analysis oldAnalysis = get(newAnalysis.sample, newAnalysis.lane);
      if (!oldAnalysis.isEmpty()) {
        analyses.remove(oldAnalysis);
      }
      analyses.add(newAnalysis);
    }

    ArrayNode toJson() {
      ArrayNode ret = MAPPER.createArrayNode();
      analyses
          .stream()
          .sorted(
              (a1, a2) -> {
                int comp = a1.sample.compareTo(a2.sample);
                if (comp == 0) { // same sample
                  return Integer.compare(a1.lane, a2.lane);
                }
                return comp;
              })
          .forEach(a -> ret.add(a.toJson()));
      return ret;
    }
  }

  private final String NUMERAL = "\\d+";
  static ObjectMapper MAPPER;
  private static final Logger log = LoggerFactory.getLogger(NovaseqXProcessor.class);

  public NovaseqXProcessor(Builder builder, boolean checkOutput) {
    super(builder, checkOutput);
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    MAPPER = new ObjectMapper().registerModule(super.setUpCustomModule(tz));
    BCLConvertAnalysis BCLConvertAnalyses = new BCLConvertAnalysis();
    IlluminaDragenNotificationDto dto = new IlluminaDragenNotificationDto();
    dto.clone((IlluminaNotificationDto) super.process(runDirectory, tz));
    dto.setAnalysisStatus(AnalysisStatus.PENDING);
    ObjectNode json = MAPPER.createObjectNode();
    File analysisDir = new File(runDirectory, "Analysis");

    // For n in Analysis/n/Data (accommodate reruns, ish. if more reruns appear, they won't be
    // scanned. Someone will need to invalidate the run with the API.)
    if (analysisDir.exists() && analysisDir.isDirectory()) {
      // Null pointer should never actually happen because of above checks
      for (File analysisAttempt : Objects.requireNonNull(analysisDir.listFiles())) {
        if (analysisAttempt.isDirectory() && analysisAttempt.getName().matches(NUMERAL)) {
          String attemptNum = analysisAttempt.getName();
          ObjectNode jsonAttempt = MAPPER.createObjectNode();
          Map<DRAGENWorkflow, Boolean> expectedWorkflows = new HashMap<>();

          // Get info from Sample Sheet
          // at root Analysis/#/Data/BCLConvert/SampleSheet.csv
          File sampleSheet = new File(analysisAttempt, "Data/BCLConvert/SampleSheet.csv");
          if (sampleSheet.exists()) {
            List<String[]> lines =
                Files.readAllLines(sampleSheet.toPath())
                    .stream()
                    .map(line -> line.split(","))
                    .filter(line -> line.length != 0)
                    .toList();

            ObjectNode jsonSampleInfo = MAPPER.createObjectNode(),
                sampleSheetBCLConvertSection = MAPPER.createObjectNode(),
                sampleSheetBCLConvertData = MAPPER.createObjectNode(),
                sampleSheetBCLConvertSettings = MAPPER.createObjectNode();
            int s = 0;
            String sectionName = "";
            for (String[] line : lines) {
              if (line[0].startsWith("[")) {
                sectionName = line[0];
                s = 0;
                continue;
              }
              switch (sectionName) {
                case "[BCLConvert_Data]":
                  if (line[0].equals("Lane")) break; // Skip column label line
                  s++; // Hopefully the lines stream in a consistent order and this will recreate
                  // the S#
                  ObjectNode nested = MAPPER.createObjectNode();
                  nested.put("Lane", line[0]);
                  nested.put("Sample_ID", line[1]);
                  nested.put("Index", line[2]);
                  nested.put("Index2", line[3]); // Some have a line[4] like "Y27;I10;I10;Y27"
                  sampleSheetBCLConvertData.set(Integer.toString(s), nested);
                  break;
                case "[BCLConvert_Settings]":
                  sampleSheetBCLConvertSettings.put(line[0], line[1]);
                  expectedWorkflows.put(DRAGENWorkflow.BCL_CONVERT, Boolean.FALSE);
                case "[Cloud_Settings]": // Discard the cloud config
                case "[Cloud_Data]":
                  break;
                default:
                  jsonSampleInfo.put(line[0], line[1]);
              }
            }
            sampleSheetBCLConvertSection.set("Data", sampleSheetBCLConvertData);
            sampleSheetBCLConvertSection.set("Settings", sampleSheetBCLConvertSettings);
            jsonSampleInfo.set("BCLConvert", sampleSheetBCLConvertSection);
            jsonAttempt.set("DRAGEN_Samplesheet", jsonSampleInfo);
          } else {
            log.info("No samplesheet for {}, was DRAGEN enabled?", runDirectory);
          }
          if (expectedWorkflows.isEmpty()) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
            return dto;
          }

          if (expectedWorkflows.containsKey(DRAGENWorkflow.BCL_CONVERT)) {
            // Get fastqs from root Analysis/#/Data/BCLConvert/fastq/Reports/fastq_list.csv
            File fastqList =
                new File(analysisAttempt, "Data/BCLConvert/fastq/Reports/fastq_list.csv");
            if (fastqList.exists() && fastqList.isFile()) {
              List<String[]> fastqLines =
                  Files.readAllLines(fastqList.toPath())
                      .stream()
                      .map(line -> line.split(","))
                      .toList();
              for (String[] fastq : fastqLines) {
                if (fastq[0].startsWith("RGID")) continue; // Skip the column label line
                // 0 = RGID, 1 = RGSM, 2 = RGLB, 3 = Lane, 4 = Read1File, 5 = Read2File
                Analysis analysis = BCLConvertAnalyses.get(fastq[1], fastq[3]);
                analysis.sample = fastq[1];
                analysis.lane = Integer.parseInt(fastq[3]);
                analysis.read1File = analysisAttempt + "/Data/BCLConvert/fastq/" + fastq[4];
                analysis.read2File = analysisAttempt + "/Data/BCLConvert/fastq/" + fastq[5];
                BCLConvertAnalyses.put(analysis);
              }

              // Get read counts from root
              // Analysis/#/Data/BCLConvert/fastq/Reports/Demultiplex_Stats.csv
              File demulitplexStats =
                  new File(analysisAttempt, "Data/BCLConvert/fastq/Reports/Demultiplex_Stats.csv");
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
                  Analysis analysis = BCLConvertAnalyses.get(demuxLine[1], demuxLine[0]);
                  analysis.readCount = Long.parseLong(demuxLine[3]);
                  BCLConvertAnalyses.put(analysis);
                }
                expectedWorkflows.put(DRAGENWorkflow.BCL_CONVERT, Boolean.TRUE);
              } else {
                log.info("No Demultiplex_Stats.csv for {}", runDirectory);
              }
            } else {
              log.info("No fastq_list.csv for {}, old DRAGEN version?", runDirectory);
            }
            jsonAttempt.set("BCLConvert", BCLConvertAnalyses.toJson());
          } // end if expectedWorkflows contains BCLConvert

          // Phase 2: more workflows go here

          if (expectedWorkflows.values().stream().allMatch(b -> b.equals(Boolean.TRUE))) {
            dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
          }
          json.set(attemptNum, jsonAttempt);
        }
      }
    } else { // TODO: Analysis dir does not exist. Does it spawn immediately or later?
      dto.setAnalysisStatus(AnalysisStatus.COMPLETED);
    }
    dto.setAnalysis(json);
    return dto;
  }

  public static NovaseqXProcessor create(Builder builder, ObjectNode parameters) {
    return new NovaseqXProcessor(builder, calculateCheckOutput(parameters));
  }
}
