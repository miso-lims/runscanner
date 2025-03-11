package ca.on.oicr.gsi.runscanner.scanner.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PipelineRun;
import ca.on.oicr.gsi.runscanner.dto.WorkflowRun;
import ca.on.oicr.gsi.runscanner.dto.dragen.AnalysisFile;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenAnalysisUnit;
import ca.on.oicr.gsi.runscanner.dto.dragen.DragenWorkflowRun;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.Test;

public abstract class AbstractProcessorTest {
  private final Class<? extends NotificationDto> clazz;

  public AbstractProcessorTest(Class<? extends NotificationDto> clazz) {
    super();
    this.clazz = clazz;
  }

  public void beforeComparison(NotificationDto reference, NotificationDto result) {
    // Not used by default, do nothing
  }

  protected final void checkDirectory(String root)
      throws JsonParseException, JsonMappingException, IOException {
    ObjectMapper mapper = RunProcessor.createObjectMapper();
    for (File directory : new File(this.getClass().getResource(root).getPath()).listFiles()) {
      NotificationDto result = process(directory);
      NotificationDto reference = mapper.readValue(new File(directory, "reference.json"), clazz);
      result.setSoftware(
          null); // We delete this because it is going to change during updates to dependencies.
      result.setSequencerFolderPath(
          null); // We delete this because it is going to be different in each environment.

      if (reference.getMetrics() == null) {
        assertNull(result.getMetrics());
      } else {
        // ensure metrics are equal
        // Load the metrics into a Jackson JsonNode to test for equality
        ObjectMapper MapperTest = RunProcessor.createObjectMapper();
        JsonNode jsonNodeResult = MapperTest.readTree(result.getMetrics());
        JsonNode jsonNodeReference = MapperTest.readTree(reference.getMetrics());
        assertEquals(jsonNodeReference, jsonNodeResult);
      }

      // For only Oxford Nanopore processors
      if (clazz.equals(OxfordNanoporeNotificationDto.class)) {
        ((OxfordNanoporeNotificationDto) result).setProtocolVersion(null);
      }

      // For only Illumina runs with DRAGEN analysis, strip out times in the DRAGEN analysis
      // Because git is liable to change each files mtime
      // This is a ratking of a for loop, but I can't get streams to cooperate
      if (clazz.equals(IlluminaNotificationDto.class)) {
        IlluminaNotificationDto illuminaResult = (IlluminaNotificationDto) result;

        if (!illuminaResult.pipelineRuns.isEmpty()) {
          for (PipelineRun<?> pr : illuminaResult.pipelineRuns) {
            for (WorkflowRun wr : pr.getWorkflowRuns()) {
              wr.setCompletionTime(Instant.EPOCH);
              wr.setStartTime(Instant.EPOCH);
              if (wr instanceof DragenWorkflowRun) {
                for (DragenAnalysisUnit dau : ((DragenWorkflowRun) wr).getAnalysisOutputs()) {
                  for (AnalysisFile af : dau.getFiles()) {
                    af.setCreatedTime(Instant.EPOCH);
                    af.setModifiedTime(Instant.EPOCH);

                    // Strip away /home/user/workspace/etc from path to match reference
                    Path pathToCut = af.getPath();
                    while (!pathToCut.getName(0).equals(Path.of("scanner"))) {
                      pathToCut = pathToCut.subpath(1, pathToCut.getNameCount());
                    }
                    af.setPath(pathToCut);
                  }
                }
              }
            }
          }
        }
      }

      // TODO call this new method before assertEquals
      beforeComparison(reference, result);

      // For all processors
      assertEquals(reference, result);
    }
  }

  protected abstract NotificationDto process(File directory) throws IOException;

  @Test
  public abstract void testGoldens() throws IOException;
}
