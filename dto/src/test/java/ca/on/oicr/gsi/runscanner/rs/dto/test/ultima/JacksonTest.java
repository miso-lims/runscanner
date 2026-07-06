package ca.on.oicr.gsi.runscanner.rs.dto.test.ultima;

import ca.on.oicr.gsi.runscanner.dto.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.type.PipelineStatus;
import ca.on.oicr.gsi.runscanner.dto.type.WorkflowRunStatus;
import ca.on.oicr.gsi.runscanner.dto.ultima.CramAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.MetadataAnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaPipelineRun;
import ca.on.oicr.gsi.runscanner.dto.ultima.UltimaWorkflowRun;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class JacksonTest {
  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  public void testCramAnalysisFileSerializeDeserialize() throws Exception {
    AnalysisFile file = makeCramFile();
    String serialized = mapper.writeValueAsString(file);
    // Deserialize via the base type to exercise the "format" discriminator
    AnalysisFile deserialized = mapper.readerFor(CramAnalysisFile.class).readValue(serialized);
    Assert.assertTrue(deserialized instanceof CramAnalysisFile);
    assertAnalysisFileEqual(file, deserialized);
  }

  @Test
  public void testMetadataAnalysisFileSerializeDeserialize() throws Exception {
    AnalysisFile file = makeMetadataFile();
    String serialized = mapper.writeValueAsString(file);
    AnalysisFile deserialized = mapper.readerFor(MetadataAnalysisFile.class).readValue(serialized);
    Assert.assertTrue(deserialized instanceof MetadataAnalysisFile);
    assertAnalysisFileEqual(file, deserialized);
  }

  @Test
  public void testUltimaAnalysisUnitSerializeDeserialize() throws Exception {
    UltimaAnalysisUnit unit = makeAnalysisUnit();
    String serialized = mapper.writeValueAsString(unit);
    UltimaAnalysisUnit deserialized =
        mapper.readerFor(UltimaAnalysisUnit.class).readValue(serialized);
    assertAnalysisUnitEqual(unit, deserialized);
  }

  @Test
  public void testUltimaWorkflowRunSerializeDeserialize() throws Exception {
    UltimaWorkflowRun workflowRun = makeUltimaWorkflowRun();
    String serialized = mapper.writeValueAsString(workflowRun);
    // Deserialize via the base type to exercise the "suite" discriminator
    UltimaWorkflowRun deserialized =
        mapper.readerFor(UltimaWorkflowRun.class).readValue(serialized);
    assertWorkflowRunEqual(workflowRun, deserialized);
  }

  @Test
  public void testUltimaPipelineRunSerializeDeserialize() throws Exception {
    UltimaPipelineRun pipelineRun = makeUltimaPipelineRun();
    String serialized = mapper.writeValueAsString(pipelineRun);
    // Deserialize via the base type to exercise the "suite" discriminator
    UltimaPipelineRun deserialized =
        mapper.readerFor(UltimaPipelineRun.class).readValue(serialized);
    assertPipelineRunEqual(pipelineRun, deserialized);
  }

  private AnalysisFile makeCramFile() {
    AnalysisFile file = new CramAnalysisFile();
    file.setPath(Path.of("/", "test-bucket", "run", "barcode", "sample.cram"));
    file.setCrc32Checksum("AAAAAA==");
    file.setSize(1000L);
    file.setCreatedTime(Instant.EPOCH);
    file.setModifiedTime(Instant.EPOCH);
    return file;
  }

  private AnalysisFile makeMetadataFile() {
    AnalysisFile file = new MetadataAnalysisFile();
    file.setPath(Path.of("/", "test-bucket", "run", "barcode", "sample.csv"));
    file.setCrc32Checksum("BBBBBB==");
    file.setSize(100L);
    file.setCreatedTime(Instant.EPOCH);
    file.setModifiedTime(Instant.EPOCH);
    return file;
  }

  private UltimaAnalysisUnit makeAnalysisUnit() {
    UltimaAnalysisUnit unit = new UltimaAnalysisUnit();
    unit.setBarcode("BC001");
    unit.setLibrary("lib1-lib2");
    unit.setFiles(List.of(makeCramFile(), makeMetadataFile()));
    return unit;
  }

  private UltimaWorkflowRun makeUltimaWorkflowRun() {
    UltimaWorkflowRun workflowRun = new UltimaWorkflowRun();
    workflowRun.setStartTime(Instant.MIN);
    workflowRun.setSoftwareVersion("TestRecipe_1.0");
    workflowRun.complete();
    workflowRun.setAnalysisOutputs(List.of(makeAnalysisUnit()));
    return workflowRun;
  }

  private UltimaPipelineRun makeUltimaPipelineRun() {
    UltimaPipelineRun pipelineRun = new UltimaPipelineRun(1);
    pipelineRun.setPipelineStatus(PipelineStatus.COMPLETE);
    pipelineRun.put(makeUltimaWorkflowRun());
    return pipelineRun;
  }

  private static void assertAnalysisFileEqual(AnalysisFile one, AnalysisFile two) {
    Assert.assertEquals(one.getPath(), two.getPath());
    Assert.assertEquals(one.getCrc32Checksum(), two.getCrc32Checksum());
    Assert.assertEquals(one.getSize(), two.getSize());
    Assert.assertEquals(one.getCreatedTime(), two.getCreatedTime());
    Assert.assertEquals(one.getModifiedTime(), two.getModifiedTime());
  }

  private static void assertAnalysisUnitEqual(UltimaAnalysisUnit one, UltimaAnalysisUnit two) {
    Assert.assertEquals(one.getBarcode(), two.getBarcode());
    Assert.assertEquals(one.getLibrary(), two.getLibrary());
    Assert.assertEquals(one.getFiles().size(), two.getFiles().size());
    assertAnalysisFileEqual(one.getFiles().get(0), two.getFiles().get(0));
    assertAnalysisFileEqual(one.getFiles().get(1), two.getFiles().get(1));
  }

  private static void assertWorkflowRunEqual(UltimaWorkflowRun one, UltimaWorkflowRun two) {
    Assert.assertEquals(one.getWorkflowName(), two.getWorkflowName());
    Assert.assertEquals(WorkflowRunStatus.COMPLETE, two.getWorkflowRunStatus());
    Assert.assertEquals(one.getStartTime(), two.getStartTime());
    Assert.assertEquals(one.getSoftwareVersion(), two.getSoftwareVersion());
    Assert.assertEquals(one.getAnalysisOutputs().size(), two.getAnalysisOutputs().size());
    assertAnalysisUnitEqual(one.getAnalysisOutputs().get(0), two.getAnalysisOutputs().get(0));
  }

  private static void assertPipelineRunEqual(UltimaPipelineRun one, UltimaPipelineRun two) {
    Assert.assertEquals(one.getAttempt(), two.getAttempt());
    Assert.assertEquals(one.getPipelineStatus(), two.getPipelineStatus());
    assertWorkflowRunEqual(one.getWorkflowRuns().get(0), two.getWorkflowRuns().get(0));
  }
}
