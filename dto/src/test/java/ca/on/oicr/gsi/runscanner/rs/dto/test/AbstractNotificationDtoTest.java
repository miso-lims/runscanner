package ca.on.oicr.gsi.runscanner.rs.dto.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.Before;
import org.junit.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

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

    JsonMapper mapper = makeJsonMapper();
    String serialized = mapper.writeValueAsString(notificationDto);

    NotificationDto deSerialized = mapper.readValue(serialized, NotificationDto.class);
    assertThat("Round trip of", notificationDto, is(deSerialized));
  }

  @Test
  public void testFullyPopulatedNotificationRoundTrip() throws Exception {
    fullyPopulatedNotificationDto("RUN_B");
    JsonMapper mapper = makeJsonMapper();
    String serialized = mapper.writeValueAsString(notificationDto);

    NotificationDto deSerialized = mapper.readValue(serialized, NotificationDto.class);
    assertThat("Round trip of", notificationDto, is(deSerialized));
  }

  private JsonMapper makeJsonMapper() {
    return JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
  }

  public abstract void fullyPopulatedNotificationDto(String sequencerName);

  public abstract NotificationDto getSpecializedNotificationDto();
}
