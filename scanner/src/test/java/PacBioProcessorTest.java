import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.dto.PacBioNotificationDto;
import ca.on.oicr.gsi.runscanner.dto.type.Platform;
import ca.on.oicr.gsi.runscanner.server.defaultprocessors.DefaultPacBio;
import ca.on.oicr.gsi.runscanner.server.defaultprocessors.DefaultPacBio.StatusResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.Test;

public class PacBioProcessorTest extends AbstractProcessorTest {
  private static class TestPacBio extends DefaultPacBio {
    private final Map<String, StatusResponse> statusResponses;
    private final Map<String, String> sampleSheetResponses;

    private TestPacBio(
        Map<String, StatusResponse> statusResponses, Map<String, String> sampleSheetResponses) {
      super(new Builder(Platform.PACBIO, "unittest", null), URL_PREFIX);
      this.statusResponses = statusResponses;
      this.sampleSheetResponses = sampleSheetResponses;
    }

    @Override
    protected StatusResponse getStatus(String url) {
      return statusResponses.get(url.substring(URL_PREFIX.length()));
    }

    @Override
    protected String getSampleSheet(String url) {
      return sampleSheetResponses.get(url.substring(URL_PREFIX.length()));
    }
  }

  private static final String URL_PREFIX = "http://example.com";

  public PacBioProcessorTest() {
    super(PacBioNotificationDto.class);
  }

  @Override
  protected NotificationDto process(File directory) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, StatusResponse> statusResponses =
        mapper.readValue(
            new File(directory, "webrequests-status.json"),
            mapper
                .getTypeFactory()
                .constructMapLikeType(HashMap.class, String.class, StatusResponse.class));
    Map<String, String> sampleSheetResponses =
        mapper.readValue(
            new File(directory, "webrequests-samplesheet.json"),
            mapper
                .getTypeFactory()
                .constructMapLikeType(HashMap.class, String.class, String.class));
    return new TestPacBio(statusResponses, sampleSheetResponses)
        .process(directory, TimeZone.getDefault());
  }

  @Test
  public void testGoldens() throws IOException {
    checkDirectory("/pacbio");
  }
}
