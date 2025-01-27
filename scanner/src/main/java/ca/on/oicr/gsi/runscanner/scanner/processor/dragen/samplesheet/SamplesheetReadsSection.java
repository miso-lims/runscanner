package ca.on.oicr.gsi.runscanner.scanner.processor.dragen.samplesheet;

public class SamplesheetReadsSection implements SamplesheetSection {
  private int read1Cycles, read2Cycles, index1Cycles, index2Cycles;

  public int getIndex1Cycles() {
    return index1Cycles;
  }

  public int getIndex2Cycles() {
    return index2Cycles;
  }

  @Override
  public String getName() {
    return "Reads";
  }

  public int getRead1Cycles() {
    return read1Cycles;
  }

  public int getRead2Cycles() {
    return read2Cycles;
  }

  public void setIndex1Cycles(int index1Cycles) {
    this.index1Cycles = index1Cycles;
  }

  public void setIndex2Cycles(int index2Cycles) {
    this.index2Cycles = index2Cycles;
  }

  public void setRead1Cycles(int read1Cycles) {
    this.read1Cycles = read1Cycles;
  }

  public void setRead2Cycles(int read2Cycles) {
    this.read2Cycles = read2Cycles;
  }
}
