package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;

public class OxfordNanoporeNotificationDto extends NotificationDto {
  private String runType;
  private String protocolVersion;

  @Override
  public Platform getPlatformType() {
    return Platform.OXFORDNANOPORE;
  }

  public String getRunType() {
    return runType;
  }

  public void setRunType(String newType) {
    runType = newType;
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(String protocolVersion) {
    this.protocolVersion = protocolVersion;
  }
}
