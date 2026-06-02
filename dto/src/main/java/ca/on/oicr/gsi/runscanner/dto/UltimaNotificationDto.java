package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.dto.type.UltimaProcessStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UltimaNotificationDto extends NotificationDto {

  private int completedFlows;
  private int expectedFlows;
  private double readLength;
  private int waferShelf;
  private UltimaProcessStatus sequencingStatus;
  private UltimaProcessStatus analysisStatus;
  private UltimaProcessStatus uploadStatus;
  private List<Consumable> consumables;
  private List<String> poolNames;

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

  public void setConsumables(List<Consumable> consumables) {
    this.consumables = consumables;
  }

  public List<Consumable> getConsumables() {
    return consumables;
  }

  public void setPoolNames(List<String> poolNames) {
    this.poolNames = poolNames;
  }

  public List<String> getPoolNames() {
    return poolNames;
  }

  public void setUploadStatus(UltimaProcessStatus uploadStatus) {
    this.uploadStatus = uploadStatus;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), expectedFlows, waferShelf);
  }

  @Override
  public Optional<String> getLaneContents(int lane) {
    if (poolNames.size() == 1) {
      return Optional.of(poolNames.get(0));
    }
    return Optional.empty();
  }

  @Override
  public String toString() {
    return super.toString()
        + ", UltimaNotificationDto ["
        + ", expectedFlows="
        + expectedFlows
        + ", consumables="
        + consumables
        + ", wafershelf="
        + waferShelf
        + ", poolNames=["
        + poolNames
        + "]]";
  }
}
