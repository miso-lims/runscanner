package ca.on.oicr.gsi.runscanner.rs.dto.test;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import java.time.LocalDateTime;

public class PacBioNotificationDtoTest extends AbstractNotificationDtoTest {
  private PacBioNotificationDto notificationDto;

  @Override
  public void specializedSetUp() {
    notificationDto = new PacBioNotificationDto();
  }

  @Override
  public void fullyPopulatedNotificationDto(String sequencerName) {
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
  }

  @Override
  public NotificationDto getSpecializedNotificationDto() {
    return notificationDto;
  }
}
