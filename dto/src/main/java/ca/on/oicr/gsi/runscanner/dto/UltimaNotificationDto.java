package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import java.util.*;

public class UltimaNotificationDto extends NotificationDto {

  private int completedFlowNum;
  private int indelLength;
  private int numBeads;
  private int outputReads;
  private int passFilterPercent;
  private Map<String, String> poolNames;
  private int readLength;
  private int wafershelf;

  public List<PipelineRun> pipelineRuns = new LinkedList<>();

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaNotificationDto other = (UltimaNotificationDto) obj;

    return Objects.equals(completedFlowNum, other.completedFlowNum)
        && Objects.equals(indelLength, other.indelLength)
        && Objects.equals(numBeads, other.numBeads)
        && Objects.equals(outputReads, other.outputReads)
        && Objects.equals(passFilterPercent, other.passFilterPercent)
        && Objects.equals(readLength, other.readLength)
        && Objects.equals(wafershelf, other.wafershelf)
        && Objects.equals(poolNames, other.poolNames);
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
    return Platform.ULTIMA;
  }

  public int getCompletedFlowNum() {
    return completedFlowNum;
  }

  public void setCompletedFlowNum(int completedFlowNum) {
    this.completedFlowNum = completedFlowNum;
  }

  public int getIndelLength() {
    return indelLength;
  }

  public void setIndelLength(int indelLength) {
    this.indelLength = indelLength;
  }

  public int getNumBeads() {
    return numBeads;
  }

  public void setNumBeads(int numBeads) {
    this.numBeads = numBeads;
  }

  public int getOutputReads() {
    return outputReads;
  }

  public void setOutputReads(int outputReads) {
    this.outputReads = outputReads;
  }

  public int getPassFilterPercent() {
    return passFilterPercent;
  }

  public void setPassFilterPercent(int passFilterPrecent) {
    this.passFilterPercent = passFilterPrecent;
  }

  public Map<String, String> getPoolNames() {
    return poolNames;
  }

  public void setPoolNames(Map<String, String> poolNames) {
    this.poolNames = poolNames;
  }

  public int getReadLength() {
    return readLength;
  }

  public void setReadLength(int readLength) {
    this.readLength = readLength;
  }

  public int getWafershelf() {
    return wafershelf;
  }

  public void setWafershelf(int wafershelf) {
    this.wafershelf = wafershelf;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(),
        completedFlowNum,
        indelLength,
        numBeads,
        outputReads,
        passFilterPercent,
        poolNames,
        readLength,
        wafershelf);
  }

  @Override
  public String toString() {
    return super.toString()
        + ", UltimaNotificationDto ["
        + "completedFlowNum="
        + completedFlowNum
        + ", indelLength="
        + indelLength
        + ", numBeads="
        + numBeads
        + ", outputReads="
        + outputReads
        + ", passFilterPrecent="
        + passFilterPercent
        + ", poolNames="
        + poolNames
        + ", readLength="
        + readLength
        + ", wafershelf="
        + wafershelf
        + "]";
  }
}
