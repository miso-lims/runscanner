package ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet;

import ca.on.oicr.gsi.Pair;
import java.util.LinkedList;
import java.util.List;

public class SamplesheetBCLConvertSection implements SamplesheetSection {
  public class SamplesheetBCLConvertDataEntry {
    private final String lane, sampleId, index, index2;

    public SamplesheetBCLConvertDataEntry(
        String lane, String sampleId, String index, String index2) {
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
  List<Pair<String, String>> settings;

  public SamplesheetBCLConvertSection() {
    this.data = new LinkedList<>();
    this.settings = new LinkedList<>();
  }

  public List<SamplesheetBCLConvertDataEntry> getData() {
    return data;
  }

  public void addDatum(String lane, String sampleId, String index, String index2) {
    data.add(new SamplesheetBCLConvertDataEntry(lane, sampleId, index, index2));
  }

  public List<Pair<String, String>> getSettings() {
    return settings;
  }

  public void addSetting(String k, String v) {
    settings.add(new Pair<>(k, v));
  }

  @Override
  public String getName() {
    return "BCLConvert";
  }
}
