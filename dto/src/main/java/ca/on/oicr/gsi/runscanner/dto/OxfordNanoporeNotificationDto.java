package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;

public class OxfordNanoporeNotificationDto extends NotificationDto {
  private String scriptPurpose;

  @Override
  public Platform getPlatformType() {
    return Platform.OXFORDNANOPORE;
  }

  public String getScriptPurpose() {
    return scriptPurpose;
  }

  public void setScriptPurpose(String newPurpose) {
    scriptPurpose = newPurpose;
  }
}
