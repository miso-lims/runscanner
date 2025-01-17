package ca.on.oicr.gsi.runscanner.rs.dto.test.dragen;

import ca.on.oicr.gsi.runscanner.dto.dragen.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenWorkflowRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.FastqAnalysisFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;

public class JacksonTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  public void testAnalysisFileSerializeDeserialize() throws Exception {
    AnalysisFile file = makeAnalysisFile();
    String serialized = mapper.writeValueAsString(file);
    AnalysisFile deserialized = mapper.readerFor(FastqAnalysisFile.class).readValue(serialized);
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
    DragenWorkflowRun dwr = makeDragenWorkflowAnalysis();

    String serialized = mapper.writeValueAsString(dwr);
    DragenWorkflowRun deserialized =
        mapper.readerFor(DragenWorkflowRun.class).readValue(serialized);
    assertDragenWorkflowAnalysisEqual(dwr, deserialized);
  }

  @Test
  public void testDragenAnalysisSerializeDeserialize() throws Exception {
    DragenPipelineRun dpr = makeDragenAnalysis();

    String serialized = mapper.writeValueAsString(dpr);
    DragenPipelineRun deserialized =
        mapper.readerFor(DragenPipelineRun.class).readValue(serialized);
    assertDragenAnalysisEqual(dpr, deserialized);
  }

  private AnalysisFile makeAnalysisFile() {
    AnalysisFile file = new FastqAnalysisFile();
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
    dau.setIndex1("AAAAAA");
    dau.setIndex2("TTTTTT");
    dau.addFile(makeAnalysisFile());
    return dau;
  }

  private DragenWorkflowRun makeDragenWorkflowAnalysis() {
    DragenWorkflowRun dwr = new DragenWorkflowRun("TEST");
    dwr.setCompletionTime(Instant.MAX);
    dwr.setStartTime(Instant.MIN);
    dwr.put(makeDragenAnalysisUnit());
    return dwr;
  }

  private DragenPipelineRun makeDragenAnalysis() {
    DragenPipelineRun dpr = new DragenPipelineRun(1);
    dpr.put(makeDragenWorkflowAnalysis());
    return dpr;
  }

  private static void assertAnalysisFileEqual(AnalysisFile one, AnalysisFile two) {
    Assert.assertEquals(one.getPath(), two.getPath());
    Assert.assertEquals(one.getCrc32Checksum(), two.getCrc32Checksum());
    Assert.assertEquals(one.getModifiedTime(), two.getModifiedTime());
    Assert.assertEquals(one.getCreatedTime(), two.getCreatedTime());
    Assert.assertEquals(one.getSize(), two.getSize());
  }

  private static void assertDragenAnalysisUnitEqual(
      DragenAnalysisUnit one, DragenAnalysisUnit two) {
    Assert.assertEquals(one.getSample(), two.getSample());
    Assert.assertEquals(one.getLane(), two.getLane());
    Assert.assertEquals(one.getIndex1(), two.getIndex1());
    Assert.assertEquals(one.getIndex2(), two.getIndex2());
    assertAnalysisFileEqual(one.getFiles().get(0), two.getFiles().get(0));
  }

  private static void assertDragenWorkflowAnalysisEqual(
      DragenWorkflowRun one, DragenWorkflowRun two) {
    Assert.assertEquals(one.getWorkflowName(), two.getWorkflowName());
    Assert.assertEquals(one.getStartTime(), two.getStartTime());
    Assert.assertEquals(one.getCompletionTime(), two.getCompletionTime());
    assertDragenAnalysisUnitEqual(one.getAnalysisOutputs().get(0), two.getAnalysisOutputs().get(0));
  }

  private static void assertDragenAnalysisEqual(DragenPipelineRun one, DragenPipelineRun two) {
    Assert.assertEquals(one.getAttempt(), two.getAttempt());
    assertDragenWorkflowAnalysisEqual(one.getWorkflowRuns().get(0), two.getWorkflowRuns().get(0));
  }
}
