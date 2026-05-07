package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.dto.type.UltimaProcessStatus;
import java.util.Objects;

public class UltimaNotificationDto extends NotificationDto {

  private int completedFlows;
  private int expectedFlows;
  private double readLength;
  private int waferShelf;
  private UltimaProcessStatus sequencingStatus;
  private UltimaProcessStatus analysisStatus;
  private UltimaProcessStatus uploadStatus;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaNotificationDto other = (UltimaNotificationDto) obj;

    return Objects.equals(expectedFlows, other.expectedFlows)
        && Objects.equals(waferShelf, other.waferShelf);
  }

  @Override
  public Platform getPlatformType() {
    return Platform.ULTIMA;
  }

  public int getCompletedFlows() {
    return completedFlows;
  }

  public void setCompletedFlows(int completedFlows) {
    this.completedFlows = completedFlows;
  }

  public int getExpectedFlows() {
    return expectedFlows;
  }

  public void setExpectedFlows(int expectedFlows) {
    this.expectedFlows = expectedFlows;
  }

  public double getReadLength() {
    return readLength;
  }

  public void setReadLength(double readLength) {
    this.readLength = readLength;
  }

  public int getWaferShelf() {
    return waferShelf;
  }

  public void setWaferShelf(int waferShelf) {
    this.waferShelf = waferShelf;
  }

  public UltimaProcessStatus getSequencingStatus() {
    return sequencingStatus;
  }

  public void setSequencingStatus(UltimaProcessStatus sequencingStatus) {
    this.sequencingStatus = sequencingStatus;
  }

  public UltimaProcessStatus getAnalysisStatus() {
    return analysisStatus;
  }

  public void setAnalysisStatus(UltimaProcessStatus analysisStatus) {
    this.analysisStatus = analysisStatus;
  }

  public UltimaProcessStatus getUploadStatus() {
    return uploadStatus;
  }

  public void setUploadStatus(UltimaProcessStatus uploadStatus) {
    this.uploadStatus = uploadStatus;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), expectedFlows, waferShelf);
  }

  @Override
  public String toString() {
    return super.toString()
        + ", UltimaNotificationDto ["
        + ", expectedFlows="
        + expectedFlows
        + ", wafershelf="
        + waferShelf
        + "]";
  }
}
