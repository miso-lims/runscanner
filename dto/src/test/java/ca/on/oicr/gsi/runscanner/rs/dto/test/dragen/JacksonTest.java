package ca.on.oicr.gsi.runscanner.rs.dto.test.dragen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.on.oicr.gsi.runscanner.dto.dragen.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysis;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenWorkflowAnalysis;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.Samplesheet;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection;
import ca.on.oicr.gsi.runscanner.dto.dragen.samplesheet.SamplesheetBCLConvertSection.SamplesheetBCLConvertDataEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class JacksonTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  public void testSamplesheetBCLConvertDataEntrySerializeDeserialize() throws Exception {
    SamplesheetBCLConvertDataEntry sbde = makeSamplesheetBCLConvertDataEntry();
    String serialized = mapper.writeValueAsString(sbde);
    SamplesheetBCLConvertDataEntry deserialized =
        mapper.readerFor(SamplesheetBCLConvertDataEntry.class).readValue(serialized);
    assertSamplesheetBCLConvertDataEntryEqual(sbde, deserialized);
  }

  @Test
  public void testSamplesheetBCLConvertSectionSerializeDeserialize() throws Exception {
    SamplesheetBCLConvertSection sbs = makeSamplesheetBCLConvertSection();
    String serialized = mapper.writeValueAsString(sbs);
    SamplesheetBCLConvertSection deserialized =
        mapper.readerFor(SamplesheetBCLConvertSection.class).readValue(serialized);
    assertSamplesheetBCLConvertSectionEqual(sbs, deserialized);
  }

  @Test
  public void testSamplesheetSerializeDeserialize() throws Exception {
    Samplesheet s = makeSamplesheet();
    String serialized = mapper.writeValueAsString(s);
    Samplesheet deserialized = mapper.readerFor(Samplesheet.class).readValue(serialized);
    assertSamplesheetEqual(s, deserialized);
  }

  @Test
  public void testAnalysisFileSerializeDeserialize() throws Exception {
    AnalysisFile file = makeAnalysisFile();
    String serialized = mapper.writeValueAsString(file);
    AnalysisFile deserialized = mapper.readerFor(AnalysisFile.class).readValue(serialized);
    assertAnalysisFileEqual(file, deserialized);
  }

  @Test
  public void testDragenAnalysisUnitSerializeDeserialize() throws Exception {
    DragenAnalysisUnit dau = makeDragenAnalysisUnit();

    String serialized = mapper.writeValueAsString(dau);
    DragenAnalysisUnit deserialized =
        mapper.readerFor(DragenAnalysisUnit.class).readValue(serialized);
    assertDragenAnalysisUnitEqual(dau, deserialized);
  }

  @Test
  public void testDragenWorkflowAnalysisSerializeDeserialize() throws Exception {
    DragenWorkflowAnalysis dwa = makeDragenWorkflowAnalysis();

    String serialized = mapper.writeValueAsString(dwa);
    DragenWorkflowAnalysis deserialized =
        mapper.readerFor(DragenWorkflowAnalysis.class).readValue(serialized);
    assertDragenWorkflowAnalysisEqual(dwa, deserialized);
  }

  @Test
  public void testDragenAnalysisSerializeDeserialize() throws Exception {
    DragenAnalysis da = makeDragenAnalysis();

    String serialized = mapper.writeValueAsString(da);
    DragenAnalysis deserialized = mapper.readerFor(DragenAnalysis.class).readValue(serialized);
    assertDragenAnalysisEqual(da, deserialized);
  }

  private AnalysisFile makeAnalysisFile() {
    AnalysisFile file = new AnalysisFile();
    file.setPath(Path.of("/", "path", "to", "file"));
    file.setCrc32Checksum("abcdefg");
    file.setModifiedTime(Instant.EPOCH);
    file.setCreatedTime(Instant.EPOCH);
    file.setSize(1000L);
    return file;
  }

  private DragenAnalysisUnit makeDragenAnalysisUnit() {
    DragenAnalysisUnit dau = new DragenAnalysisUnit();
    dau.setSample("TEST_SAMPLE");
    dau.setLane(1);
    dau.setIndex("AAAAAA-TTTTTT");
    dau.addFile(makeAnalysisFile());
    return dau;
  }

  private DragenWorkflowAnalysis makeDragenWorkflowAnalysis() {
    DragenWorkflowAnalysis dwa = new DragenWorkflowAnalysis("TEST");
    dwa.setCompletionTime(Instant.MAX);
    dwa.setStartTime(Instant.MIN);
    dwa.put(makeDragenAnalysisUnit());
    return dwa;
  }

  private DragenAnalysis makeDragenAnalysis() {
    DragenAnalysis da = new DragenAnalysis(makeSamplesheet(), 1);
    da.put(makeDragenWorkflowAnalysis());
    return da;
  }

  private Samplesheet makeSamplesheet() {
    Samplesheet s = new Samplesheet();
    s.setModifiedTime(Instant.EPOCH);
    s.addToSamplesheet(makeSamplesheetBCLConvertSection());
    return s;
  }

  private SamplesheetBCLConvertSection makeSamplesheetBCLConvertSection() {
    SamplesheetBCLConvertSection sbs = new SamplesheetBCLConvertSection();
    sbs.addSetting("test", "true");
    // creating a DataEntry and inserting it directly is not supported
    sbs.addDatum(String.valueOf(1), "TEST_SAMPLE", "AAAAAAA", "TTTTTTT");
    return sbs;
  }

  private SamplesheetBCLConvertDataEntry makeSamplesheetBCLConvertDataEntry() {
    return new SamplesheetBCLConvertDataEntry(String.valueOf(1), "TEST_SAMPLE", "AAAAAA", "TTTTTT");
  }

  private static void assertSamplesheetBCLConvertDataEntryEqual(
      SamplesheetBCLConvertDataEntry one, SamplesheetBCLConvertDataEntry two) {
    assertEquals(one.lane(), two.lane());
    assertEquals(one.sampleId(), two.sampleId());
    assertEquals(one.index(), two.index());
    assertEquals(one.index2(), two.index2());
  }

  private static void assertSamplesheetBCLConvertSectionEqual(
      SamplesheetBCLConvertSection one, SamplesheetBCLConvertSection two) {
    assertEquals(one.getName(), two.getName());
    assertEquals(one.getSettings(), two.getSettings());
    assertSamplesheetBCLConvertDataEntryEqual(one.getData().get(0), two.getData().get(0));
  }

  private static void assertSamplesheetEqual(Samplesheet one, Samplesheet two) {
    assertEquals(one.getModifiedTime(), two.getModifiedTime());
    assertSamplesheetBCLConvertSectionEqual(
        (SamplesheetBCLConvertSection) one.getByName("BCLConvert"),
        (SamplesheetBCLConvertSection) two.getByName("BCLConvert"));
  }

  private static void assertAnalysisFileEqual(AnalysisFile one, AnalysisFile two) {
    assertEquals(one.getPath(), two.getPath());
    assertEquals(one.getCrc32Checksum(), two.getCrc32Checksum());
    assertEquals(one.getModifiedTime(), two.getModifiedTime());
    assertEquals(one.getCreatedTime(), two.getCreatedTime());
    assertEquals(one.getSize(), two.getSize());
  }

  private static void assertDragenAnalysisUnitEqual(
      DragenAnalysisUnit one, DragenAnalysisUnit two) {
    assertEquals(one.getSample(), two.getSample());
    assertEquals(one.getLane(), two.getLane());
    assertEquals(one.getIndex(), two.getIndex());
    assertAnalysisFileEqual(one.getFiles().get(0), two.getFiles().get(0));
  }

  private static void assertDragenWorkflowAnalysisEqual(
      DragenWorkflowAnalysis one, DragenWorkflowAnalysis two) {
    assertEquals(one.getWorkflowName(), two.getWorkflowName());
    assertEquals(one.getStartTime(), two.getStartTime());
    assertEquals(one.getCompletionTime(), two.getCompletionTime());
    assertDragenAnalysisUnitEqual(one.getAnalyses().get(0), two.getAnalyses().get(0));
  }

  private static void assertDragenAnalysisEqual(DragenAnalysis one, DragenAnalysis two) {
    assertEquals(one.getAttempt(), two.getAttempt());
    assertSamplesheetEqual(one.getSamplesheet(), two.getSamplesheet());
    assertDragenWorkflowAnalysisEqual(one.getAnalyses().get(0), two.getAnalyses().get(0));
  }
}
