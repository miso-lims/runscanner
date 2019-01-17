package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor.Builder;
import ca.on.oicr.gsi.status.ConfigurationSection;
import ca.on.oicr.gsi.status.Header;
import ca.on.oicr.gsi.status.NavigationMenu;
import ca.on.oicr.gsi.status.SectionRenderer;
import ca.on.oicr.gsi.status.ServerConfig;
import ca.on.oicr.gsi.status.StatusPage;
import ca.on.oicr.gsi.status.TablePage;
import ca.on.oicr.gsi.status.TableRowWriter;
import com.google.common.collect.ImmutableSortedMap;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Front-end status monitoring for run scanner */
@Controller
public class UserInterfaceController {
  public static final ServerConfig SERVER_CONFIG =
      new ServerConfig() {

        @Override
        public Stream<NavigationMenu> navigation() {
          return COLLECTIONS
              .keySet()
              .stream() //
              .sorted() //
              .map(collectionName -> NavigationMenu.item("list" + collectionName, collectionName));
        }

        @Override
        public String name() {
          return "Run Scanner";
        }

        @Override
        public Stream<Header> headers() {
          return Stream.empty();
        }
      };
  /** These are all the collections of files that the scheduler can report.s */
  private static final Map<String, Function<Scheduler, Iterable<File>>> COLLECTIONS =
      ImmutableSortedMap.<String, Function<Scheduler, Iterable<File>>>naturalOrder() //
          .put("Finished", Scheduler::getFinishedDirectories) //
          .put("Scheduled", Scheduler::getScheduledWork) //
          .put("Processing", Scheduler::getCurrentWork) //
          .put("Instruments", Scheduler::getRoots) //
          .put("Failed", Scheduler::getFailedDirectories) //
          .put("Unreadable", Scheduler::getUnreadableDirectories) //
          .build();

  @Autowired private Scheduler scheduler;

  /** List a collection of files */
  @GetMapping(value = "/list{collection}")
  public void listPaths(@PathVariable String collection, HttpServletResponse response)
      throws IOException {
    response.setContentType("text/html;charset=utf-8");
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
    response.setContentType("text/html;charset=utf-8");
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
                              renderer.line("Valid?", configuration.isValid() ? "Yes" : "No");
                            }
                          }));
        }

        @Override
        protected void emitCore(SectionRenderer renderer) throws XMLStreamException {
          renderer.line("Is Configuration Good?", scheduler.isConfigurationGood() ? "Yes" : "No");
          renderer.line("Last Configuration Read", scheduler.getConfigurationLastRead());
          renderer.line("Scanning Enabled", scheduler.isScanningEnabled() ? "Yes" : "No");
          renderer.line("Currently Scanning", scheduler.isScanningNow() ? "Yes" : "No");
          renderer.lineSpan("Time Since Last Scan", scheduler.getScanLastStarted());
          renderer.line("Processed Runs", scheduler.getFinishedDirectories().size());
          renderer.line("Waiting Runs", scheduler.getScheduledWork().size());
        }
      }.renderPage(output);
    }
  }
}
