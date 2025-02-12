package ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class SamplesheetBCLConvertSection implements SamplesheetSection {
  public static final class SamplesheetBCLConvertDataEntry {

    private final String lane;
    private final String sampleId;
    private final String index;
    private final String index2;
    private String overrideCycles;

    public SamplesheetBCLConvertDataEntry(
        String lane, String sampleId, String index, String index2, String overrideCycles) {
      this.lane = lane;
      this.sampleId = sampleId;
      this.index = index;
      this.index2 = index2;
      this.overrideCycles = overrideCycles;
    }

    public String getLane() {
      return lane;
    }

    public void setOverrideCycles(String overrideCycles) {
      this.overrideCycles = overrideCycles;
    }

    public String getOverrideCycles() {
      return overrideCycles;
    }

    public String getSampleId() {
      return sampleId;
    }

    public String getIndex() {
      return index;
    }

    public String getIndex2() {
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
          && Objects.equals(this.index2, that.index2)
          && Objects.equals(this.overrideCycles, that.overrideCycles);
    }

    @Override
    public int hashCode() {
      return Objects.hash(lane, sampleId, index, index2, overrideCycles);
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
          + ", overrideCycles="
          + overrideCycles
          + ']';
    }
  }

  private List<SamplesheetBCLConvertDataEntry> data;
  private String overrideCyclesSetting;
  private Semver softwareVersion;

  public SamplesheetBCLConvertSection() {
    this.data = new LinkedList<>();
  }

  public List<SamplesheetBCLConvertDataEntry> getData() {
    return data;
  }

  public void addDatum(
      String lane, String sampleId, String index, String index2, String overrideCycles) {
    data.add(new SamplesheetBCLConvertDataEntry(lane, sampleId, index, index2, overrideCycles));
  }

  @Override
  public String getName() {
    return "BCLConvert";
  }

  public String getOverrideCyclesSetting() {
    return overrideCyclesSetting;
  }

  public Semver getSoftwareVersion() {
    return softwareVersion;
  }

  public void setOverrideCyclesSetting(String overrideCyclesSetting) {
    this.overrideCyclesSetting = overrideCyclesSetting;
  }

  public void setSoftwareVersion(String softwareVersion) {
    String[] vers = softwareVersion.split("\\.");
    this.setSoftwareVersion(
        new Semver(
            Integer.parseInt(vers[0]), Integer.parseInt(vers[1]), Integer.parseInt(vers[2])));
  }

  public void setSoftwareVersion(Semver version) {
    this.softwareVersion = version;
  }

  public String toString() {
    return "SamplesheetBCLConvertSection [data="
        + data
        + ", softwareVersion="
        + softwareVersion
        + ", overrideCycles="
        + overrideCyclesSetting
        + "]";
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SamplesheetBCLConvertSection other = (SamplesheetBCLConvertSection) obj;

    return Objects.equals(this.data, other.data)
        && Objects.equals(this.softwareVersion, other.softwareVersion)
        && Objects.equals(this.overrideCyclesSetting, other.overrideCyclesSetting);
  }

  public int hashCode() {
    return Objects.hash(data, softwareVersion, overrideCyclesSetting);
  }
}
