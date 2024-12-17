package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class PacBioNotificationDto extends NotificationDto {

  private Map<String, String> poolNames;

  private List<Map<String, String>> positionList;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    PacBioNotificationDto other = (PacBioNotificationDto) obj;

    return Objects.equals(this.poolNames, other.poolNames);
  }

  @Override
  public Optional<String> getLaneContents(int lane) {
    String wellName = String.format("%c%2d", 'A' + (lane % 8), lane / 8);
    return poolNames.containsKey(wellName)
        ? Optional.of(poolNames.get(wellName))
        : Optional.empty();
  }

  @Override
  public Platform getPlatformType() {
    return Platform.PACBIO;
  }

  public Map<String, String> getPoolNames() {
    return poolNames;
  }

  public List<Map<String, String>> getPositionList() {
    return positionList;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), poolNames);
  }

  public void setPoolNames(Map<String, String> poolNames) {
    this.poolNames = poolNames;
  }

  public void setPositionList(List<Map<String, String>> positionList) {
    this.positionList = positionList;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", PacBioNotificationDto [poolNames = "
        + poolNames
        + "]"
        + ", "
        + "PacBioNotificationDto [positionList = "
        + positionList
        + "]";
  }
}
