package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SamplesheetBCLConvertSection implements SamplesheetSection {
  // This could be a record, but the code formatter crashes if i try
  public static final class SamplesheetBCLConvertDataEntry {

    private final String lane;
    private final String sampleId;
    private final String index;
    private final String index2;

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

    public String lane() {
      return lane;
    }

    public String sampleId() {
      return sampleId;
    }

    public String index() {
      return index;
    }

    public String index2() {
      return index2;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || obj.getClass() != this.getClass()) {
        return false;
      }
      var that = (SamplesheetBCLConvertDataEntry) obj;
      return Objects.equals(this.lane, that.lane)
          && Objects.equals(this.sampleId, that.sampleId)
          && Objects.equals(this.index, that.index)
          && Objects.equals(this.index2, that.index2);
    }

    @Override
    public int hashCode() {
      return Objects.hash(lane, sampleId, index, index2);
    }

    @Override
    public String toString() {
      return "SamplesheetBCLConvertDataEntry["
          + "lane="
          + lane
          + ", "
          + "sampleId="
          + sampleId
          + ", "
          + "index="
          + index
          + ", "
          + "index2="
          + index2
          + ']';
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

  public String toString() {
    return "SamplesheetBCLConvertSection [data=" + data + ", settings=" + settings + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SamplesheetBCLConvertSection other = (SamplesheetBCLConvertSection) obj;

    return Objects.equals(this.data, other.data) && Objects.equals(this.settings, other.settings);
  }

  public int hashCode() {
    return Objects.hash(data, settings);
  }
}
