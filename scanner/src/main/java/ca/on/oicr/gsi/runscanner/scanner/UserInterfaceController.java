package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import ca.on.oicr.gsi.status.BasePage;
import ca.on.oicr.gsi.status.ConfigurationSection;
import ca.on.oicr.gsi.status.Header;
import ca.on.oicr.gsi.status.NavigationMenu;
import ca.on.oicr.gsi.status.SectionRenderer;
import ca.on.oicr.gsi.status.ServerConfig;
import ca.on.oicr.gsi.status.StatusPage;
import ca.on.oicr.gsi.status.TablePage;
import ca.on.oicr.gsi.status.TableRowWriter;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Front-end status monitoring for run scanner */
@Controller
@Hidden
public class UserInterfaceController {
  @Value("${project.version}")
  String projectVersion;

  public static final ServerConfig SERVER_CONFIG =
      new ServerConfig() {

        @Override
        public Stream<NavigationMenu> navigation() {
          return COLLECTIONS
              .keySet()
              .stream() //
              .map(collectionName -> NavigationMenu.item("list" + collectionName, collectionName));
        }

        @Override
        public String name() {
          return "Run Scanner";
        }

        @Override
        public Stream<Header> headers() {
          return Stream.of(
              Header.cssFile("resources/styles/style.css"),
              Header.favicon("resources/favicon.ico", 32, "image/x-icon"));
        }

        @Override
        public String documentationUrl() {
          return "swagger-ui/index.html";
        }
      };

  private static final String CONTENT_TYPE = "text/html;charset=utf-8";

  private static final String SCANNED = "Scanned";
  private static final String SCHEDULED = "Scheduled";
  private static final String PROCESSING = "Processing";
  private static final String UNREADABLE = "Unreadable";
  private static final String FS_ERROR = "File System Error";

  /** These are all the collections of files that the scheduler can report. */
  private static final Map<String, Function<Scheduler, Iterable<File>>> COLLECTIONS =
      new ImmutableMap.Builder<String, Function<Scheduler, Iterable<File>>>() //
          .put(SCANNED, Scheduler::getFinishedDirectories) //
          .put(SCHEDULED, Scheduler::getScheduledWork) //
          .put(PROCESSING, Scheduler::getCurrentWork) //
          .put(UNREADABLE, Scheduler::getFailedDirectories) //
          .put(FS_ERROR, Scheduler::getFSUnreadableDirectories) //
          .build();

  @Autowired private Scheduler scheduler;

  @GetMapping(value = "/listScanned")
  public void listScannedRuns(HttpServletResponse response) throws IOException {
    response.setContentType(CONTENT_TYPE);
    try (OutputStream output = response.getOutputStream()) {
      new TablePage(SERVER_CONFIG) {

        @Override
        protected void writeRows(TableRowWriter writer) {
          AtomicReference<Boolean> empty = new AtomicReference<>(true);
          scheduler
              .finished()
              .sorted(Comparator.comparing(NotificationDto::getRunAlias))
              .forEach(
                  run -> {
                    List<Pair<String, String>> lineAttributes = new ArrayList<>();
                    lineAttributes.add(
                        new Pair<>(
                            "onclick",
                            String.format("window.location='run/%s'", run.getRunAlias())));
                    lineAttributes.add(new Pair<>("class", "link"));
                    writer.write(lineAttributes, run.getRunAlias(), run.getSequencerFolderPath());
                    empty.set(false);
                  });
          if (empty.get()) {
            writer.write(Arrays.asList(new Pair<>("colspan", "2")), "No items.");
          }
          response.setStatus(HttpServletResponse.SC_OK);
        }
      }.renderPage(output);
    }
  }

  /** List a collection of files */
  @GetMapping(value = "/list{collection}")
  public void listPaths(@PathVariable String collection, HttpServletResponse response)
      throws IOException {
    response.setContentType(CONTENT_TYPE);
    response.setStatus(HttpServletResponse.SC_OK);
    try (OutputStream output = response.getOutputStream()) {
      new TablePage(SERVER_CONFIG) {

        @Override
        protected void writeRows(TableRowWriter writer) {
          boolean empty = true;
          for (File file :
              COLLECTIONS.getOrDefault(collection, s -> Collections.emptySet()).apply(scheduler)) {
            writer.write(false, file.getName(), file.getPath());
            empty = false;
          }
          if (empty) {
            writer.write(Arrays.asList(new Pair<>("colspan", "2")), "No items.");
          }
        }
      }.renderPage(output);
    }
  }

  /** Show the main status page */
  @GetMapping(value = "/")
  public void showStatus(HttpServletResponse response) throws IOException {
    response.setContentType(CONTENT_TYPE);
    response.setStatus(HttpServletResponse.SC_OK);
    try (OutputStream output = response.getOutputStream()) {
      new StatusPage(SERVER_CONFIG) {

        @Override
        public Stream<ConfigurationSection> sections() {
          return Stream.concat(
              Stream.of(
                  new ConfigurationSection("Processors") {

                    @Override
                    public void emit(SectionRenderer renderer) throws XMLStreamException {
                      RunProcessor.builders() //
                          .sorted(
                              Comparator.<Builder>comparingInt(
                                      builder -> builder.getPlatformType().ordinal())
                                  .thenComparing(Builder::getName)) //
                          .forEach(
                              builder ->
                                  renderer.line(
                                      builder.getName(), builder.getPlatformType().name()));
                    }
                  }),
              scheduler
                  .getConfiguration() //
                  .map(
                      configuration ->
                          new ConfigurationSection(configuration.getPath().getPath()) {

                            @Override
                            public void emit(SectionRenderer renderer) throws XMLStreamException {
                              renderer.line(
                                  "Platform",
                                  configuration.getProcessor().getPlatformType().name());
                              renderer.line("Processor", configuration.getProcessor().getName());
                              renderer.line(
                                  "Time Zone", configuration.getTimeZone().getDisplayName());
                              renderer.line(
                                  "Valid?",
                                  configuration.isValid()
                                      ? "Yes"
                                      : "No: " + configuration.validitySummary());
                              renderer.line("Ignoring subdirectories", " ");
                              // Add a new render line for each subdirectory to ignore
                              for (File directory : configuration.getIgnoreSubdirectories()) {
                                renderer.line("-", directory.toString());
                              }
                            }
                          }));
        }

        @Override
        protected void emitCore(SectionRenderer renderer) throws XMLStreamException {
          renderer.line("Run Scanner Version", projectVersion);
          if (scheduler.isConfigurationGood()) {
            renderer.line("Is Configuration Good?", "Yes");
          } else {
            renderer.line(
                Stream.of(new Pair<String, String>("class", "bad")),
                "Is Configuration Good?",
                "No");
          }
          renderer.line("Last Configuration Read", scheduler.getConfigurationLastRead());
          renderer.line("Scanning Enabled", scheduler.isScanningEnabled() ? "Yes" : "No");
          renderer.line("Currently Scanning", scheduler.isScanningNow() ? "Yes" : "No");
          if (scheduler.getScanLastStarted() == null) {
            renderer.line(
                "Time Since Last Scan", "Starting up... (may take up to 15 minutes to begin scan)");
          } else {
            renderer.lineSpan("Time Since Last Scan", scheduler.getScanLastStarted());
          }
          renderer.line("Processed Runs", scheduler.getFinishedDirectories().size());
          renderer.line("Waiting Runs", scheduler.getScheduledWork().size());
        }
      }.renderPage(output);
    }
  }

  @GetMapping("/error")
  public void showError(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType(CONTENT_TYPE);

    Object attr = request.getAttribute("jakarta.servlet.error.status_code");
    int statusCode = attr == null ? 500 : (Integer) attr;
    if (attr == null) {
      response.setStatus(500);
    }
    String message = HttpStatus.valueOf(statusCode).getReasonPhrase();

    try (OutputStream output = response.getOutputStream()) {
      new BasePage(SERVER_CONFIG, false) {

        @Override
        protected void renderContent(XMLStreamWriter writer) throws XMLStreamException {
          writer.writeStartElement("h1");
          writer.writeCharacters("%d %s".formatted(statusCode, message));
          writer.writeEndElement();
        }
      }.renderPage(output);
    }
  }
}
