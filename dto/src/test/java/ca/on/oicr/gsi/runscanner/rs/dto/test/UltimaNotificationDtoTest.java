package ca.on.oicr.gsi.runscanner.rs.dto.test;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.UltimaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ca.on.oicr.gsi.runscanner.dto.type.UltimaProcessStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class UltimaNotificationDtoTest extends AbstractNotificationDtoTest {
  private UltimaNotificationDto notificationDto;

  @Override
  public void specializedSetUp() {
    notificationDto = new UltimaNotificationDto();
  }

  @Override
  public void fullyPopulatedNotificationDto(String sequencerName) {
    notificationDto.setRunAlias("TEST_RUN_NAME");
    notificationDto.setSequencerFolderPath("/sequencers/TEST_RUN_FOLDER");
    notificationDto.setContainerSerialNumber("CONTAINER_ID");
    notificationDto.setSequencerName(sequencerName);
    notificationDto.setLaneCount(1);
    notificationDto.setSequencingStatus(UltimaProcessStatus.COMPLETE);
    notificationDto.setAnalysisStatus(UltimaProcessStatus.COMPLETE);
    notificationDto.setUploadStatus(UltimaProcessStatus.COMPLETE);
    notificationDto.setHealthType(HealthType.COMPLETED);
    notificationDto.setStartDate(
        LocalDateTime.of(2017, 2, 23, 0, 0).atZone(ZoneId.of("America/Toronto")).toInstant());
    notificationDto.setCompletionDate(
        LocalDateTime.of(2017, 2, 27, 0, 0).atZone(ZoneId.of("America/Toronto")).toInstant());
    notificationDto.setPairedEndRun(true);
    notificationDto.setSoftware("2.2.6.2");
    notificationDto.setCompletedFlows(339);
    notificationDto.setExpectedFlows(300);
    notificationDto.setReadLength(123.456);
    notificationDto.setWaferShelf(4);
  }

  @Override
  public NotificationDto getSpecializedNotificationDto() {
    return notificationDto;
  }
}
