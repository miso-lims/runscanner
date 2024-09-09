package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaDragenNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public class NovaseqXProcessor extends DefaultIllumina {
  final String NUMERAL = "\\d+";
  final ObjectMapper MAPPER = new ObjectMapper();

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
                BCLConvert = MAPPER.createObjectNode(),
                BCLConvertData = MAPPER.createObjectNode(),
                BCLConvertSettings = MAPPER.createObjectNode();
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
                  // the
                  // S#
                  ObjectNode nested = MAPPER.createObjectNode();
                  nested.put("Lane", line[0]);
                  nested.put("Sample_ID", line[1]);
                  nested.put("Index", line[2]);
                  nested.put("Index2", line[3]); // Some have a line[4] like "Y27;I10;I10;Y27"
                  BCLConvertData.set(Integer.toString(s), nested);
                  break;
                case "[BCLConvert_Settings]":
                  BCLConvertSettings.put(line[0], line[1]);
                case "[Cloud_Settings]": // Discard the cloud config
                case "[Cloud_Data]":
                  break;
                default:
                  jsonSampleInfo.put(line[0], line[1]);
              }
            }
            BCLConvert.set("Data", BCLConvertData);
            BCLConvert.set("Settings", BCLConvertSettings);
            jsonSampleInfo.set("BCLConvert", BCLConvert);
            jsonAttempt.set("SampleSheet", jsonSampleInfo);
          } else {
            System.err.println("No samplesheet for " + runDirectory + ", was DRAGEN enabled?");
          }

          // Get fastqs from root Analysis/#/Data/BCLConvert/fastq/Reports/fastq_list.csv
          File fastqList =
              new File(analysisAttempt, "Data/BCLConvert/fastq/Reports/fastq_list.csv");
          if (fastqList.exists() && fastqList.isFile()) {
            List<String[]> fastq_lines =
                Files.readAllLines(fastqList.toPath())
                    .stream()
                    .map(line -> line.split(","))
                    .toList();
            ArrayNode fastq_json = MAPPER.createArrayNode();
            for (String[] fastq : fastq_lines) {
              if (fastq[0].startsWith("RGID")) continue; // Skip the column label line
              ObjectNode fastq_line = MAPPER.createObjectNode();
              fastq_line.put("RGID", fastq[0]);
              fastq_line.put("RGSM", fastq[1]);
              fastq_line.put("RGLB", fastq[2]);
              fastq_line.put("Lane", fastq[3]);
              fastq_line.put("Read1File", fastq[4]);
              fastq_line.put("Read2File", fastq[5]);
              fastq_json.add(fastq_line);
            }
            jsonAttempt.set("BCLConvert", fastq_json);
          } else {
            System.err.println("No fastq_list.csv for " + runDirectory + ", old DRAGEN version?");
          }

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
