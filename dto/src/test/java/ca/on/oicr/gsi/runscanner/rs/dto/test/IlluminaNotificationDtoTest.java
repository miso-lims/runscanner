package ca.on.oicr.gsi.runscanner.rs.dto.test;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import java.time.LocalDateTime;

public class IlluminaNotificationDtoTest extends AbstractNotificationDtoTest {

  @Override
  public void setUp() {
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
    notificationDto.setStartDate(LocalDateTime.of(2017, 2, 23, 0, 0));
    notificationDto.setCompletionDate(LocalDateTime.of(2017, 2, 27, 0, 0));
    notificationDto.setPairedEndRun(true);
    notificationDto.setSoftware("Fido Opus SEAdog Standard Interface Layer");
    ((IlluminaNotificationDto) notificationDto).setRunBasesMask("y151,I8,y151");
    ((IlluminaNotificationDto) notificationDto).setNumCycles(20);
    ((IlluminaNotificationDto) notificationDto).setImgCycle(19);
    ((IlluminaNotificationDto) notificationDto).setScoreCycle(18);
    ((IlluminaNotificationDto) notificationDto).setCallCycle(17);
    ((IlluminaNotificationDto) notificationDto).setWorkflowType(null);
  }
}
