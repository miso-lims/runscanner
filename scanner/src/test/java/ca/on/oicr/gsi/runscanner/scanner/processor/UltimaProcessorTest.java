// package ca.on.oicr.gsi.runscanner.scanner.processor;
//
// import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
// import ca.on.oicr.gsi.runscanner.dto.UltimaNotificationDto;
// import ca.on.oicr.gsi.runscanner.dto.type.Platform;
// import ca.on.oicr.gsi.runscanner.scanner.processor.DefaultUltima.StatusResponse;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.io.File;
// import java.io.IOException;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.TimeZone;
//
// public class UltimaProcessorTest extends AbstractProcessorTest {
//  private static class TestUltima extends DefaultUltima {
//    private final Map<String, StatusResponse> statusResponses;
//    private final Map<String, String> sampleSheetResponses;
//
//    private TestUltima(
//        Map<String, StatusResponse> statusResponses, Map<String, String> sampleSheetResponses) {
//      super(new Builder(Platform.ULTIMA, "unittest", null), URL_PREFIX);
//      this.statusResponses = statusResponses;
//      this.sampleSheetResponses = sampleSheetResponses;
//    }
//
//    @Override
//    protected StatusResponse getStatus(String url) {
//      return statusResponses.get(url.substring(URL_PREFIX.length()));
//    }
//
//    @Override
//    protected String getSampleSheet(String url) {
//      return sampleSheetResponses.get(url.substring(URL_PREFIX.length()));
//    }
//  }
//
//  private static final String URL_PREFIX = "http://example.com";
//
//  public UltimaProcessorTest() {
//    super(UltimaNotificationDto.class);
//  }
//
//  @Override
//  protected NotificationDto process(File directory) throws IOException {
//    ObjectMapper mapper = new ObjectMapper();
//    Map<String, StatusResponse> statusResponses =
//        mapper.readValue(
//            new File(directory, "webrequests-status.json"),
//            mapper
//                .getTypeFactory()
//                .constructMapLikeType(HashMap.class, String.class, StatusResponse.class));
//    Map<String, String> sampleSheetResponses =
//        mapper.readValue(
//            new File(directory, "webrequests-samplesheet.json"),
//            mapper
//                .getTypeFactory()
//                .constructMapLikeType(HashMap.class, String.class, String.class));
//    return new TestUltima(statusResponses, sampleSheetResponses)
//        .process(directory, TimeZone.getTimeZone("America/Toronto"));
//  }
//
//  @Override
//  public void testGoldens() throws IOException {
//    checkDirectory("/ultima");
//  }
// }
