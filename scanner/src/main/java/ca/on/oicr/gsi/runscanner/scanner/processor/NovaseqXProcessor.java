package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaDragenNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TimeZone;

public class NovaseqXProcessor extends DefaultIllumina {
  private class BCLConvertAnalysis {
    int lane;
    String read1File, read2File;
    long readCount;

    public ObjectNode toJson() {
      ObjectNode analysisJson = MAPPER.createObjectNode();
      analysisJson.put("Lane", lane);
      analysisJson.put("Read1File", read1File);
      analysisJson.put("Read2File", read2File);
      analysisJson.put("ReadCount", readCount);
      return analysisJson;
    }
  }

  final String NUMERAL = "\\d+";
  final ObjectMapper MAPPER = new ObjectMapper();

  // Sample Name + "_L<Lane>" to BCLConvertAnalysis' object
  Map<String, BCLConvertAnalysis> BCLConvertAnalyses = new HashMap<>();

  public NovaseqXProcessor(Builder builder, boolean checkOutput) {
    super(builder, checkOutput);
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    IlluminaNotificationDto parentDto = (IlluminaNotificationDto) super.process(runDirectory, tz);
    ObjectNode json = MAPPER.createObjectNode();
    File analysisDir = new File(runDirectory, "Analysis");

    // TODO: autoclosing try?
    // For n in Analysis/n/Data (accommodate reruns)
    if (analysisDir.exists() && analysisDir.isDirectory()) {
      // Null pointer should never actually happen because of above checks
      for (File analysisAttempt : Objects.requireNonNull(analysisDir.listFiles())) {
        if (analysisAttempt.isDirectory() && analysisAttempt.getName().matches(NUMERAL)) {
          String attemptNum = analysisAttempt.getName();
          ObjectNode jsonAttempt = MAPPER.createObjectNode();

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
            // TODO: Get info about how many analysis steps to scan for
            // Currently we just assume BCLConvert and nothing else

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
            System.err.println("No samplesheet for " + runDirectory + ", was DRAGEN enabled?");
          }

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
              BCLConvertAnalysis analysis = new BCLConvertAnalysis();

              // 0 = RGID, 1 = RGSM, 2 = RGLB, 3 = Lane, 4 = Read1File, 5 = Read2File
              analysis.lane = Integer.parseInt(fastq[3]);
              analysis.read1File = analysisAttempt + "/Data/BCLConvert/fastq/" + fastq[4];
              analysis.read2File = analysisAttempt + "/Data/BCLConvert/fastq/" + fastq[5];
              BCLConvertAnalyses.put(fastq[1] + "_L" + fastq[3], analysis);
            }
          } else {
            System.err.println("No fastq_list.csv for " + runDirectory + ", old DRAGEN version?");
          }

          ObjectNode analysisJson = MAPPER.createObjectNode();
          for (Entry<String, BCLConvertAnalysis> entry : BCLConvertAnalyses.entrySet()) {
            analysisJson.set(entry.getKey(), entry.getValue().toJson());
          }
          jsonAttempt.set("BCLConvert", analysisJson);
          json.set(attemptNum, jsonAttempt);
        } // end if
      } // end For Analysis/n/
    } // end if

    return new IlluminaDragenNotificationDto(parentDto, json);
  }

  public static NovaseqXProcessor create(Builder builder, ObjectNode parameters) {
    return new NovaseqXProcessor(builder, calculateCheckOutput(parameters));
  }
}
