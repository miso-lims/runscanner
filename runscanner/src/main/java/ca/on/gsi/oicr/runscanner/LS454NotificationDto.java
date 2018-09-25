package ca.on.gsi.oicr.runscanner;

public class LS454NotificationDto extends NotificationDto {
  private int cycles;

  public int getCycles() {
    return cycles;
  }

  public void setCycles(int cycles) {
    this.cycles = cycles;
  }

  @Override
  public Platform getPlatformType() {
    return Platform.LS454;
  }

  @Override
  public String toString() {
    return "LS454NotificationDto [cycles=" + cycles + ", getRunAlias()=" + getRunAlias() + ", getSequencerName()=" + getSequencerName()
        + ", getContainerSerialNumber()=" + getContainerSerialNumber() + ", getLaneCount()=" + getLaneCount() + ", getHealthType()="
        + getHealthType() + ", getSequencerFolderPath()=" + getSequencerFolderPath() + ", isPairedEndRun()=" + isPairedEndRun()
        + ", getSoftware()=" + getSoftware() + ", getStartDate()=" + getStartDate() + ", getCompletionDate()=" + getCompletionDate() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + cycles;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    LS454NotificationDto other = (LS454NotificationDto) obj;
    if (cycles != other.cycles) return false;
    return true;
  }

}
