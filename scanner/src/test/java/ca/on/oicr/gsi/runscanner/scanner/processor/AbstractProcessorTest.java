package ca.on.oicr.gsi.runscanner.scanner.processor;

import static org.junit.Assert.assertEquals;

import ca.on.oicr.gsi.runscanner.dto.IlluminaNotificationDto;
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

      // For all non-Illumina processors
      if (!clazz.equals(IlluminaNotificationDto.class)) {
        result.setMetrics(null); // We delete these because changes in metrics are non-critical.
      }

      // For Illumina processors
      if (clazz.equals(IlluminaNotificationDto.class)) {
        // Load the metrics into a Jackson JsonNode to test for equality
        ObjectMapper mapperResult = new ObjectMapper();
        ObjectMapper mapperReference = new ObjectMapper();
        JsonNode jsonNodeResult = mapperResult.readTree(result.getMetrics());
        JsonNode jsonNodeReference = mapperReference.readTree(reference.getMetrics());
        if (jsonNodeResult.equals(jsonNodeReference)) {
          // If Json objects are equal, set them to be the same before testing assertEquals
          reference.setMetrics(result.getMetrics());
        }
      }

      if (clazz.equals(OxfordNanoporeNotificationDto.class)) {
        ((OxfordNanoporeNotificationDto) result).setProtocolVersion(null);
      }
      assertEquals(reference, result);
    }
  }

  protected abstract NotificationDto process(File directory) throws IOException;

  @Test
  public abstract void testGoldens() throws IOException;
}
