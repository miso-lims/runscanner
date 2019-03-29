package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * A "run" as seen by Run Scanner
 *
 * <p>Run Scanner collects information about runs and reports them to other tools for use for
 * incorporation.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "platform")
@JsonSubTypes({ //
  @Type(value = PacBioNotificationDto.class, name = "PacBio"), //
  @Type(value = IlluminaNotificationDto.class, name = "Illumina"), //
  @Type(value = OxfordNanoporeNotificationDto.class, name = "OxfordNanopore") //
}) //
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class NotificationDto {

  private String runAlias;
  private String sequencerFolderPath;
  private String sequencerName;
  private String sequencerPosition;
  private String containerSerialNumber;
  private String containerModel;
  private int laneCount;
  private HealthType healthType;
  private LocalDateTime startDate;
  private LocalDateTime completionDate;
  private boolean pairedEndRun;
  private String software;
  private String metrics;

  /**
   * Get the alias of the run.
   *
   * <p>This is usually a derivative of the directory name the run output is stored in.
   */
  public String getRunAlias() {
    return runAlias;
  }

  public void setRunAlias(String runAlias) {
    this.runAlias = runAlias;
  }

  /**
   * Get the name of the sequencer/instrument
   *
   * <p>This must match to the names of sequencer. If a notification has no matching sequencer, it
   * will be discarded during processing. How the name of the sequencer is detected varies by the
   * instrument/platform used in Run Scanner.
   */
  public String getSequencerName() {
    return sequencerName;
  }

  public void setSequencerName(String sequencerName) {
    this.sequencerName = sequencerName;
  }

  public String getSequencerPosition() {
    return sequencerPosition;
  }

  public void setSequencerPosition(String sequencerPosition) {
    this.sequencerPosition = sequencerPosition;
  }

  /**
   * Get the unique name of the container.
   *
   * <p>This is usually the unique barcode of the container. Run Scanner currently does not handle
   * multiple containers per run. If there is no logical definition of container for the platform,
   * this should be the same as the run's alias.
   */
  public String getContainerSerialNumber() {
    return containerSerialNumber;
  }

  public void setContainerSerialNumber(String containerId) {
    this.containerSerialNumber = containerId;
  }

  /** @return the part number OR model name of the container */
  public String getContainerModel() {
    return containerModel;
  }

  public void setContainerModel(String containerModel) {
    this.containerModel = containerModel;
  }

  /**
   * Gets the number of partitions in the run.
   *
   * @return the number of partitions in this run.
   */
  public int getLaneCount() {
    return laneCount;
  }

  public void setLaneCount(int laneCount) {
    this.laneCount = laneCount;
  }

  /**
   * Get the status of the run.
   *
   * <p>This the current status of the run. This may be impossible to correctly detect depending on
   * the platform.
   *
   * @see HealthType#isDone()
   */
  public HealthType getHealthType() {
    return healthType;
  }

  public void setHealthType(HealthType healthType) {
    this.healthType = healthType;
  }

  /** Get the file path to the sequencing output */
  public String getSequencerFolderPath() {
    return sequencerFolderPath;
  }

  public void setSequencerFolderPath(String sequencerFolderPath) {
    this.sequencerFolderPath = sequencerFolderPath;
  }

  /**
   * Check if the run is paired end
   *
   * <p>This is recorded for some platform types. If the target platform type does not support
   * paired end runs, this field will be ignored.
   */
  public boolean isPairedEndRun() {
    return pairedEndRun;
  }

  public void setPairedEndRun(boolean pairedEndRun) {
    this.pairedEndRun = pairedEndRun;
  }

  /**
   * Get the platform-specific software name/version of the sequencer or the library used to read
   * the on-disk output.
   */
  public String getSoftware() {
    return software;
  }

  public void setSoftware(String software) {
    this.software = software;
  }

  /** Get the time when the sequencer run started, if known. */
  public LocalDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDateTime startDate) {
    this.startDate = startDate;
  }

  /**
   * Get the time when the sequencer run completed/failed/stopped.
   *
   * <p>This is only read if {@link HealthType#isDone()}. If the run is not done, the completion
   * date of the run is set to null regardless of the contents of this field.
   */
  public LocalDateTime getCompletionDate() {
    return completionDate;
  }

  public void setCompletionDate(LocalDateTime completionDate) {
    this.completionDate = completionDate;
  }

  /**
   * Gets a JSON-encoded array of objects containing information for display purposes.
   *
   * <p>This provides the data that can be used to generate metrics on the front end. Each metric
   * object has a "type" property that determines how the front end will display the information, if
   * at all. The rest of the object's properties are determined on a "type" by "type" basis and the
   * format expected is determined by the consumer, which is assumed to be a JavaScript front-end.
   */
  public String getMetrics() {
    return metrics;
  }

  public void setMetrics(String metrics) {
    this.metrics = metrics;
  }

  @JsonIgnore
  public abstract Platform getPlatformType();

  @Override
  public String toString() {
    return "NotificationDto [runAlias="
        + runAlias
        + ", sequencerFolderPath="
        + sequencerFolderPath
        + ", sequencerName="
        + sequencerName
        + ", containerSerialNumber="
        + containerSerialNumber
        + ", containerModel="
        + containerModel
        + ", laneCount="
        + laneCount
        + ", healthType="
        + healthType
        + ", startDate="
        + startDate
        + ", completionDate="
        + completionDate
        + ", pairedEndRun="
        + pairedEndRun
        + ", software="
        + software
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((completionDate == null) ? 0 : completionDate.hashCode());
    result =
        prime * result + ((containerSerialNumber == null) ? 0 : containerSerialNumber.hashCode());
    result = prime * result + ((containerModel == null) ? 0 : containerModel.hashCode());
    result = prime * result + ((healthType == null) ? 0 : healthType.hashCode());
    result = prime * result + laneCount;
    result = prime * result + ((metrics == null) ? 0 : metrics.hashCode());
    result = prime * result + (pairedEndRun ? 1231 : 1237);
    result = prime * result + ((runAlias == null) ? 0 : runAlias.hashCode());
    result = prime * result + ((sequencerFolderPath == null) ? 0 : sequencerFolderPath.hashCode());
    result = prime * result + ((sequencerName == null) ? 0 : sequencerName.hashCode());
    result = prime * result + ((software == null) ? 0 : software.hashCode());
    result = prime * result + ((startDate == null) ? 0 : startDate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    NotificationDto other = (NotificationDto) obj;
    if (completionDate == null) {
      if (other.completionDate != null) return false;
    } else if (!completionDate.equals(other.completionDate)) return false;
    if (containerSerialNumber == null) {
      if (other.containerSerialNumber != null) return false;
    } else if (!containerSerialNumber.equals(other.containerSerialNumber)) return false;
    if (containerModel == null) {
      if (other.containerModel != null) return false;
    } else if (!containerModel.equals(other.containerModel)) return false;
    if (healthType != other.healthType) return false;
    if (laneCount != other.laneCount) return false;
    if (metrics == null) {
      if (other.metrics != null) return false;
    } else if (!metrics.equals(other.metrics)) return false;
    if (pairedEndRun != other.pairedEndRun) return false;
    if (runAlias == null) {
      if (other.runAlias != null) return false;
    } else if (!runAlias.equals(other.runAlias)) return false;
    if (sequencerFolderPath == null) {
      if (other.sequencerFolderPath != null) return false;
    } else if (!sequencerFolderPath.equals(other.sequencerFolderPath)) return false;
    if (sequencerName == null) {
      if (other.sequencerName != null) return false;
    } else if (!sequencerName.equals(other.sequencerName)) return false;
    if (software == null) {
      if (other.software != null) return false;
    } else if (!software.equals(other.software)) return false;
    if (startDate == null) {
      if (other.startDate != null) return false;
    } else if (!startDate.equals(other.startDate)) return false;
    return true;
  }

  /**
   * Determine the identification barcode of a pool in a lane
   *
   * <p>For some instruments, a sample sheet is provided. If multiple pools can be provided for a
   * single partition, the correct behaviour is to return empty.
   *
   * @param lane, the lane of interest, [0, {{@link #getLaneCount()}
   */
  public Optional<String> getLaneContents(int lane) {
    return Optional.empty();
  }
}
