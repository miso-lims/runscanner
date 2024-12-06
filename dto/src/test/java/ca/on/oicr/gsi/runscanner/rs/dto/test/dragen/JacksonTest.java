package ca.on.oicr.gsi.runscanner.rs.dto.test.dragen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.on.oicr.gsi.runscanner.dto.dragen.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenWorkflowAnalysis;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class JacksonTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
    dau.setIndex("AAAAAA-TTTTT");
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
    assertEquals(one.getFiles(), two.getFiles());
  }

  private static void assertDragenWorkflowAnalysisEqual(
      DragenWorkflowAnalysis one, DragenWorkflowAnalysis two) {
    assertEquals(one.getWorkflowName(), two.getWorkflowName());
    assertEquals(one.getStartTime(), two.getStartTime());
    assertEquals(one.getCompletionTime(), two.getCompletionTime());
    assertEquals(one.getAnalyses(), two.getAnalyses());
  }
}
