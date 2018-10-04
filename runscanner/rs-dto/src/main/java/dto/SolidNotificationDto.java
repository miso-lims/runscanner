package dto;

import type.Platform;

public class SolidNotificationDto extends NotificationDto {
  @Override
  public Platform getPlatformType() {
    return Platform.SOLID;
  }

  @Override
  public String toString() {
    return "SolidNotificationDto [getRunAlias()=" + getRunAlias() + ", getSequencerName()=" + getSequencerName()
        + ", getContainerSerialNumber()=" + getContainerSerialNumber() + ", getLaneCount()=" + getLaneCount() + ", getHealthType()="
        + getHealthType() + ", getSequencerFolderPath()=" + getSequencerFolderPath() + ", isPairedEndRun()=" + isPairedEndRun()
        + ", getSoftware()=" + getSoftware() + ", getStartDate()=" + getStartDate() + ", getCompletionDate()=" + getCompletionDate() + "]";
  }

}
