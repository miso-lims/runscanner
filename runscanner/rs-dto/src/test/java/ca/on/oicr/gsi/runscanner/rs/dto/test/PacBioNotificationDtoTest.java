package ca.on.oicr.gsi.runscanner.rs.dto.test;
 import static org.hamcrest.CoreMatchers.is;
 import static org.junit.Assert.assertThat;

 import java.text.ParseException;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;

 import org.junit.Test;

 import com.fasterxml.jackson.databind.ObjectMapper;
 import com.fasterxml.jackson.databind.SerializationFeature;
 import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
 import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ca.on.oicr.gsi.runscanner.rs.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.rs.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.rs.dto.type.HealthType;

 public class PacBioNotificationDtoTest {


 @Test
 public void testRoundTrip() throws Exception {
 PacBioNotificationDto notificationDto = new PacBioNotificationDto();
 notificationDto.setSequencerName("Coffee");
 notificationDto.setCompletionDate(LocalDateTime.of(2017, 2, 23, 0, 0));
 notificationDto.setHealthType(HealthType.RUNNING);

 ObjectMapper mapper = new ObjectMapper();
 mapper.registerModule(new JavaTimeModule())
 .setDateFormat(new ISO8601DateFormat())
 .enable(SerializationFeature.INDENT_OUTPUT);
 String serialized = mapper.writeValueAsString(notificationDto);

 NotificationDto deSerialized = mapper.readValue(serialized, NotificationDto.class);
 assertThat("Round trip of", notificationDto, is(deSerialized));
 }

 @Test
 public void testFullyPopulatedPacBioNotificationRoundTrip() throws Exception {
 PacBioNotificationDto notificationDto = fullyPopulatedPacBioNotificationDto("RUN_B");
 ObjectMapper mapper = new ObjectMapper();
 mapper.registerModule(new JavaTimeModule())
 .setDateFormat(new ISO8601DateFormat())
 .enable(SerializationFeature.INDENT_OUTPUT);
 String serialized = mapper.writeValueAsString(notificationDto);

 NotificationDto deSerialized = mapper.readValue(serialized, NotificationDto.class);
 assertThat("Round trip of", notificationDto, is(deSerialized));
 }

 static PacBioNotificationDto fullyPopulatedPacBioNotificationDto(String sequencerName) {
 PacBioNotificationDto notificationDto = new PacBioNotificationDto();
 notificationDto.setRunAlias("TEST_RUN_NAME");
 notificationDto.setSequencerFolderPath("/sequencers/TEST_RUN_FOLDER");
 notificationDto.setContainerSerialNumber("CONTAINER_ID");
 notificationDto.setSequencerName(sequencerName);
 notificationDto.setLaneCount(8);
 notificationDto.setHealthType(HealthType.RUNNING);
 notificationDto.setStartDate(LocalDateTime.of(2017, 2, 23, 0, 0));
 notificationDto.setCompletionDate(LocalDateTime.of(2017, 2, 27, 0, 0));
 notificationDto.setPairedEndRun(true);
 notificationDto.setSoftware("Fido Opus SEAdog Standard Interface Layer");
 return notificationDto;
 }

 }
