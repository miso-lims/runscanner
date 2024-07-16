package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.IlluminaChemistry;
import ca.on.oicr.gsi.runscanner.dto.type.IndexSequencing;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import java.util.List;
import java.util.Objects;

public class IlluminaNotificationDto extends NotificationDto {

  private int bclCount;
  private int callCycle;
  private IlluminaChemistry chemistry;
  private int imgCycle;
  private List<Integer> indexLengths;
  private int numCycles;
  private int numReads;
  private int readLength;
  private List<Integer> readLengths;
  private String runBasesMask;
  private int scoreCycle;
  private String workflowType;
  private IndexSequencing indexSequencing;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    IlluminaNotificationDto other = (IlluminaNotificationDto) obj;
    return Objects.equals(this.getMetrics(), other.getMetrics())
        && Objects.equals(this.runBasesMask, other.runBasesMask)
        && Objects.equals(this.bclCount, other.bclCount)
        && Objects.equals(this.callCycle, other.callCycle)
        && Objects.equals(this.chemistry, other.chemistry)
        && Objects.equals(this.imgCycle, other.imgCycle)
        && Objects.equals(this.indexLengths, other.indexLengths)
        && Objects.equals(this.numCycles, other.numCycles)
        && Objects.equals(this.numReads, other.numReads)
        && Objects.equals(this.readLength, other.readLength)
        && Objects.equals(this.readLengths, other.readLengths)
        && Objects.equals(this.scoreCycle, other.scoreCycle)
        && Objects.equals(this.workflowType, other.workflowType)
        && Objects.equals(this.indexSequencing, other.indexSequencing);
  }

  public int getBclCount() {
    return bclCount;
  }

  public int getCallCycle() {
    return callCycle;
  }

  public IlluminaChemistry getChemistry() {
    return chemistry;
  }

  public int getImgCycle() {
    return imgCycle;
  }

  /**
   * Get the lengths of the index reads in this run
   *
   * <p>This is the number of nucleotides in the index reads of this run. A single-index 6bp run
   * would be encoded as [6], while a dual-index 8bp run would be [8,8]. If no index was done, this
   * would be an empty list.
   */
  public List<Integer> getIndexLengths() {
    return indexLengths;
  }

  public int getNumCycles() {
    return numCycles;
  }

  public int getNumReads() {
    return numReads;
  }

  @Override
  public Platform getPlatformType() {
    return Platform.ILLUMINA;
  }

  public int getReadLength() {
    return readLength;
  }

  public List<Integer> getReadLengths() {
    return readLengths;
  }

  public String getRunBasesMask() {
    return runBasesMask;
  }

  public int getScoreCycle() {
    return scoreCycle;
  }

  public String getWorkflowType() {
    return workflowType;
  }

  public IndexSequencing getIndexSequencing() {
    return indexSequencing;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        runBasesMask,
        bclCount,
        callCycle,
        chemistry,
        imgCycle,
        indexLengths,
        numCycles,
        numReads,
        readLength,
        readLengths,
        scoreCycle,
        workflowType,
        indexSequencing);
  }

  public void setBclCount(int bclCount) {
    this.bclCount = bclCount;
  }

  public void setCallCycle(int callCycle) {
    this.callCycle = callCycle;
  }

  public void setChemistry(IlluminaChemistry chemistry) {
    this.chemistry = chemistry;
  }

  public void setImgCycle(int imgCycle) {
    this.imgCycle = imgCycle;
  }

  public void setIndexLengths(List<Integer> indexLengths) {
    this.indexLengths = indexLengths;
  }

  public void setNumCycles(int numCycles) {
    this.numCycles = numCycles;
  }

  public void setNumReads(int numReads) {
    this.numReads = numReads;
  }

  public void setReadLength(int readLength) {
    this.readLength = readLength;
  }

  public void setReadLengths(List<Integer> readLengths) {
    this.readLengths = readLengths;
  }

  public void setRunBasesMask(String runBasesMask) {
    this.runBasesMask = runBasesMask;
  }

  public void setScoreCycle(int scoreCycle) {
    this.scoreCycle = scoreCycle;
  }

  public void setWorkflowType(String workflowType) {
    this.workflowType = workflowType;
  }

  public void setIndexSequencing(IndexSequencing indexSequencing) {
    this.indexSequencing = indexSequencing;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", IlluminaNotificationDto [metrics="
        + getMetrics()
        + ", callCycle="
        + callCycle
        + ", chemistry="
        + chemistry
        + ", imgCycle="
        + imgCycle
        + ", indexLengths="
        + indexLengths
        + ", numCycles="
        + numCycles
        + ", readLengths="
        + readLengths
        + ", readLength="
        + readLength
        + ", scoreCycle="
        + scoreCycle
        + ", bclCount="
        + bclCount
        + ", runBasesMask="
        + runBasesMask
        + ", numReads="
        + numReads
        + ", workflowType="
        + workflowType
        + ", indexSequencing="
        + indexSequencing
        + "]";
  }
}
