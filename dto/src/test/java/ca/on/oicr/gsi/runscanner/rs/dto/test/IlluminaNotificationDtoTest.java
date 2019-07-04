package ca.on.oicr.gsi.runscanner.rs.dto.test;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class IlluminaNotificationDtoTest extends AbstractNotificationDtoTest {
  private IlluminaNotificationDto notificationDto;

  @Override
  public void specializedSetUp() {
    notificationDto = new IlluminaNotificationDto();
  }

  @Override
  public void fullyPopulatedNotificationDto(String sequencerName) {
    notificationDto.setRunAlias("TEST_RUN_NAME");
    notificationDto.setSequencerFolderPath("/sequencers/TEST_RUN_FOLDER");
    notificationDto.setContainerSerialNumber("CONTAINER_ID");
    notificationDto.setSequencerName(sequencerName);
    notificationDto.setLaneCount(8);
    notificationDto.setHealthType(HealthType.RUNNING);
    notificationDto.setStartDate(
        LocalDateTime.of(2017, 2, 23, 0, 0).atZone(ZoneId.of("America/Toronto")).toInstant());
    notificationDto.setCompletionDate(
        LocalDateTime.of(2017, 2, 27, 0, 0).atZone(ZoneId.of("America/Toronto")).toInstant());
    notificationDto.setPairedEndRun(true);
    notificationDto.setSoftware("Fido Opus SEAdog Standard Interface Layer");
    notificationDto.setRunBasesMask("y151,I8,y151");
    notificationDto.setNumCycles(20);
    notificationDto.setImgCycle(19);
    notificationDto.setScoreCycle(18);
    notificationDto.setCallCycle(17);
    notificationDto.setWorkflowType(null);
  }

  @Override
  public NotificationDto getSpecializedNotificationDto() {
    return notificationDto;
  }
}
