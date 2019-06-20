package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import java.util.Objects;

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

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    OxfordNanoporeNotificationDto other = (OxfordNanoporeNotificationDto) obj;

    return Objects.equals(this.runType, other.runType)
        && Objects.equals(this.protocolVersion, other.protocolVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), runType, protocolVersion);
  }

  @Override
  public String toString() {
    return super.toString()
        + ", OxfordNanoporeNotificationDto [runType = "
        + runType
        + ", protocolVersion = "
        + protocolVersion
        + "]";
  }
}
