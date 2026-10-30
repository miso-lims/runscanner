package ca.on.oicr.gsi.runscanner.dto.ultima;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

// Corresponds to one Barcode Folder: {runId}-{library}-{barcode}/
public class UltimaAnalysisUnit {

  private String barcode;
  private String library;
  private List<UltimaAnalysisFile> files = new LinkedList<>();

  public String getBarcode() {
    return barcode;
  }

  public void setBarcode(String barcode) {
    this.barcode = barcode;
  }

  public String getLibrary() {
    return library;
  }

  public void setLibrary(String library) {
    this.library = library;
  }

  public List<UltimaAnalysisFile> getFiles() {
    return files;
  }

  public void setFiles(List<UltimaAnalysisFile> files) {
    this.files = files;
  }

  @Override
  public String toString() {
    return "UltimaAnalysisUnit [barcode="
        + barcode
        + ", library="
        + library
        + ", files="
        + files
        + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    UltimaAnalysisUnit other = (UltimaAnalysisUnit) obj;
    return Objects.equals(barcode, other.barcode)
        && Objects.equals(library, other.library)
        && Objects.equals(files, other.files);
  }

  @Override
  public int hashCode() {
    return Objects.hash(barcode, library, files);
  }
}
