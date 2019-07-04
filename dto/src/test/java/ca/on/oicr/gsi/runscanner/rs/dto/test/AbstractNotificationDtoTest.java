package ca.on.oicr.gsi.runscanner.rs.dto.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractNotificationDtoTest {
  private NotificationDto notificationDto;

  @Before
  public void setUp() {
    specializedSetUp();
    notificationDto = getSpecializedNotificationDto();
  }

  public abstract void specializedSetUp();

  @Test
  public void testPartiallyPopulatedNotificationRoundTrip() throws Exception {
    notificationDto.setSequencerName("Coffee");
    notificationDto.setCompletionDate(
        LocalDateTime.of(2017, 2, 23, 0, 0).atZone(ZoneId.of("America/Toronto")).toInstant());
    notificationDto.setHealthType(HealthType.RUNNING);

    ObjectMapper mapper = new ObjectMapper();
    mapper
        .registerModule(new JavaTimeModule())
        .setDateFormat(new ISO8601DateFormat())
        .enable(SerializationFeature.INDENT_OUTPUT);
    String serialized = mapper.writeValueAsString(notificationDto);

    NotificationDto deSerialized = mapper.readValue(serialized, NotificationDto.class);
    assertThat("Round trip of", notificationDto, is(deSerialized));
  }

  @Test
  public void testFullyPopulatedNotificationRoundTrip() throws Exception {
    fullyPopulatedNotificationDto("RUN_B");
    ObjectMapper mapper = new ObjectMapper();
    mapper
        .registerModule(new JavaTimeModule())
        .setDateFormat(new ISO8601DateFormat())
        .enable(SerializationFeature.INDENT_OUTPUT);
    String serialized = mapper.writeValueAsString(notificationDto);

    NotificationDto deSerialized = mapper.readValue(serialized, NotificationDto.class);
    assertThat("Round trip of", notificationDto, is(deSerialized));
  }

  public abstract void fullyPopulatedNotificationDto(String sequencerName);

  public abstract NotificationDto getSpecializedNotificationDto();
}
