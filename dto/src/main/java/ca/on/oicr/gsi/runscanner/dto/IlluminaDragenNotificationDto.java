package ca.on.oicr.gsi.runscanner.dto;

import ca.on.oicr.gsi.runscanner.dto.type.AnalysisStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public class IlluminaDragenNotificationDto extends IlluminaNotificationDto {
  private JsonNode analysis;
  private AnalysisStatus analysisStatus;

  @Override
  public boolean isDone() {
    return getHealthType().isDone() && analysisStatus.equals(AnalysisStatus.COMPLETED);
  }

  public void clone(IlluminaNotificationDto parent) {
    this.setBclCount(parent.getBclCount());
    this.setCallCycle(parent.getCallCycle());
    this.setChemistry(parent.getChemistry());
    this.setImgCycle(parent.getImgCycle());
    this.setIndexLengths(parent.getIndexLengths());
    this.setNumCycles(parent.getNumCycles());
    this.setReadLength(parent.getReadLength());
    this.setReadLengths(parent.getReadLengths());
    this.setRunBasesMask(parent.getRunBasesMask());
    this.setScoreCycle(parent.getScoreCycle());
    this.setWorkflowType(parent.getWorkflowType());
    this.setIndexSequencing(parent.getIndexSequencing());
    this.setRunAlias(parent.getRunAlias());
    this.setSequencerFolderPath(parent.getSequencerFolderPath());
    this.setSequencerName(parent.getSequencerName());
    this.setSequencerPosition(parent.getSequencerPosition());
    this.setContainerSerialNumber(parent.getContainerSerialNumber());
    this.setContainerModel(parent.getContainerModel());
    this.setSequencingKit(parent.getSequencingKit());
    this.setLaneCount(parent.getLaneCount());
    this.setHealthType(parent.getHealthType());
    this.setStartDate(parent.getStartDate());
    this.setCompletionDate(parent.getCompletionDate());
    this.setPairedEndRun(parent.isPairedEndRun());
    this.setSoftware(parent.getSoftware());
    this.setMetrics(parent.getMetrics());
  }

  public JsonNode getAnalysis() {
    return analysis;
  }

  public AnalysisStatus getAnalysisStatus() {
    return analysisStatus;
  }

  public void setAnalysis(JsonNode analysis) {
    this.analysis = analysis;
  }

  public boolean equals(Object obj) {
    return super.equals(obj)
        && (obj instanceof IlluminaDragenNotificationDto)
        && this.analysis.equals(((IlluminaDragenNotificationDto) obj).analysis)
        && this.analysisStatus.equals(((IlluminaDragenNotificationDto) obj).analysisStatus);
  }

  public int hashCode() {
    return Objects.hash(super.hashCode(), analysis, analysisStatus);
  }

  public void setAnalysisStatus(AnalysisStatus analysisStatus) {
    this.analysisStatus = analysisStatus;
  }
}
