package ca.on.oicr.gsi.runscanner.scanner.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.OxfordNanoporeNotificationDto;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

public abstract class AbstractProcessorTest {
  private final Class<? extends NotificationDto> clazz;

  public AbstractProcessorTest(Class<? extends NotificationDto> clazz) {
    super();
    this.clazz = clazz;
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

      // For all processors
      assertEquals(reference, result);
    }
  }

  protected abstract NotificationDto process(File directory) throws IOException;

  @Test
  public abstract void testGoldens() throws IOException;
}
