package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.HealthType;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.time.Instant;
import java.util.Objects;
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
  private String sequencingKit;
  private int laneCount;
  private HealthType healthType;
  private Instant startDate;
  private Instant completionDate;
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

  /** @return the part number OR name of the sequencing kit */
  public String getSequencingKit() {
    return sequencingKit;
  }

  public void setSequencingKit(String sequencingKit) {
    this.sequencingKit = sequencingKit;
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
  public Instant getStartDate() {
    return startDate;
  }

  public void setStartDate(Instant startDate) {
    this.startDate = startDate;
  }

  /**
   * Get the time when the sequencer run completed/failed/stopped.
   *
   * <p>This is only read if {@link HealthType#isDone()}. If the run is not done, the completion
   * date of the run is set to null regardless of the contents of this field.
   */
  public Instant getCompletionDate() {
    return completionDate;
  }

  public void setCompletionDate(Instant completionDate) {
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
        + ", sequencerPosition="
        + sequencerPosition
        + ", containerSerialNumber="
        + containerSerialNumber
        + ", containerModel="
        + containerModel
        + ", laneCount="
        + laneCount
        + ", sequencingKit="
        + sequencingKit
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
    return Objects.hash(
        completionDate,
        containerSerialNumber,
        containerModel,
        sequencingKit,
        healthType,
        laneCount,
        pairedEndRun,
        runAlias,
        sequencerFolderPath,
        sequencerName,
        sequencerPosition,
        software,
        startDate);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    NotificationDto other = (NotificationDto) obj;

    return Objects.equals(this.completionDate, other.completionDate)
        && Objects.equals(this.containerSerialNumber, other.containerSerialNumber)
        && Objects.equals(this.containerModel, other.containerModel)
        && Objects.equals(this.sequencingKit, other.sequencingKit)
        && Objects.equals(this.healthType, other.healthType)
        && Objects.equals(this.laneCount, other.laneCount)
        && Objects.equals(this.pairedEndRun, other.pairedEndRun)
        && Objects.equals(this.runAlias, other.runAlias)
        && Objects.equals(this.sequencerFolderPath, other.sequencerFolderPath)
        && Objects.equals(this.sequencerName, other.sequencerName)
        && Objects.equals(this.sequencerPosition, other.sequencerPosition)
        && Objects.equals(this.software, other.software)
        && Objects.equals(this.startDate, other.startDate);
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
