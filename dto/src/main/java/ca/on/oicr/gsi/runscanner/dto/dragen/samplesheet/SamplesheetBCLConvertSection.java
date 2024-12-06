package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SamplesheetBCLConvertSection implements SamplesheetSection {
  public static class SamplesheetBCLConvertDataEntry {
    private final String lane, sampleId, index, index2;

    public SamplesheetBCLConvertDataEntry(
        @JsonProperty("lane") String lane,
        @JsonProperty("sampleId") String sampleId,
        @JsonProperty("index") String index,
        @JsonProperty("index2") String index2) {
      this.lane = lane;
      this.sampleId = sampleId;
      this.index = index;
      this.index2 = index2;
    }

    public String getIndex() {
      return index;
    }

    public String getIndex2() {
      return index2;
    }

    public String getLane() {
      return lane;
    }

    public String getSampleId() {
      return sampleId;
    }
  }

  List<SamplesheetBCLConvertDataEntry> data;
  Map<String, String> settings;

  public SamplesheetBCLConvertSection() {
    this.data = new LinkedList<>();
    this.settings = new HashMap<>();
  }

  public List<SamplesheetBCLConvertDataEntry> getData() {
    return data;
  }

  public void addDatum(String lane, String sampleId, String index, String index2) {
    data.add(new SamplesheetBCLConvertDataEntry(lane, sampleId, index, index2));
  }

  public Map<String, String> getSettings() {
    return settings;
  }

  public void addSetting(String k, String v) {
    settings.put(k, v);
  }

  @Override
  public String getName() {
    return "BCLConvert";
  }
}
