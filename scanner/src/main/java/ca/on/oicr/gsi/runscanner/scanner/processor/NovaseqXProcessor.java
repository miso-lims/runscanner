package ca.on.oicr.gsi.runscanner.scanner.processor;

import ca.on.oicr.gsi.runscanner.dto.IlluminaDragenNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

public class NovaseqXProcessor extends DefaultIllumina {

  public NovaseqXProcessor(Builder builder, boolean checkOutput) {
    super(builder, checkOutput);
  }

  @Override
  public NotificationDto process(File runDirectory, TimeZone tz) throws IOException {
    IlluminaNotificationDto parentDto = (IlluminaNotificationDto) super.process(runDirectory, tz);

    JsonNode json = new ObjectMapper().nullNode();

    // Get info from Sample Sheet
    // at root Analysis/#/Data/BCLConvert/SampleSheet.csv

    // Get fastqs from root Analysis/#/Data/BCLConvert/fastq/Reports/fastq_list.csv

    return new IlluminaDragenNotificationDto(parentDto, json);
  }

  public static NovaseqXProcessor create(Builder builder, ObjectNode parameters) {
    return new NovaseqXProcessor(builder, calculateCheckOutput(parameters));
  }
}
