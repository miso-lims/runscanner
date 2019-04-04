package ca.on.oicr.gsi.runscanner.server.util;

import ca.on.oicr.gsi.Pair;
import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.processor.RunProcessor;
import ca.on.oicr.gsi.runscanner.processor.RunProcessor.Builder;
import ca.on.oicr.gsi.status.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

/** Front-end status monitoring for run server */
public class UserInterfaceController {
  private static final ServerConfig SERVER_CONFIG =
      new ServerConfig() {

        @Override
        public Stream<NavigationMenu> navigation() {
          return collections() //
              .map(
                  collection ->
                      NavigationMenu.item("list" + collection.first(), collection.first()));
        }

        @Override
        public String name() {
          return "Run Scanner";
        }

        @Override
        public Stream<Header> headers() {
          return Stream.of(
              Header.cssFile("style.css"), Header.favicon("favicon.ico", 32, "image/x-icon"));
        }
      };

  private static final String SCANNED = "Scanned";
  private static final String SCHEDULED = "Scheduled";
  private static final String PROCESSING = "Processing";
  private static final String UNREADABLE = "Unreadable";
  private static final String FS_ERROR = "File System Error";
  private static final String INSTRUMENTS = "Instruments";

  /** These are all the collections of files that the scheduler can report. */
  public static Stream<Pair<String, Function<Scheduler, Iterable<File>>>> collections() {
    return Stream.of(
        new Pair<>(SCANNED, Scheduler::getFinishedDirectories),
        new Pair<>(SCHEDULED, Scheduler::getScheduledWork),
        new Pair<>(PROCESSING, Scheduler::getCurrentWork),
        new Pair<>(UNREADABLE, Scheduler::getFailedDirectories),
        new Pair<>(FS_ERROR, Scheduler::getFSUnreadableDirectories),
        new Pair<>(INSTRUMENTS, Scheduler::getRoots));
  }

  private final Scheduler scheduler;

  public UserInterfaceController(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public void listScannedRuns(HttpServerExchange exchange) throws IOException {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html;charset=utf-8");
    try (OutputStream output = exchange.getOutputStream()) {
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
            writer.write(Collections.singletonList(new Pair<>("colspan", "2")), "No items.");
          }
        }
      }.renderPage(output);
    }
  }
  /** List a collection of files */
  public void listPaths(Function<Scheduler, Iterable<File>> function, HttpServerExchange exchange)
      throws IOException {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html;charset=utf-8");
    try (OutputStream output = exchange.getOutputStream()) {
      new TablePage(SERVER_CONFIG) {

        @Override
        protected void writeRows(TableRowWriter writer) {
          boolean empty = true;
          for (File file : function.apply(scheduler)) {
            writer.write(false, file.getName(), file.getPath());
            empty = false;
          }
          if (empty) {
            writer.write(Collections.singletonList(new Pair<>("colspan", "2")), "No items.");
          }
        }
      }.renderPage(output);
    }
  }

  /** Show the main status page */
  public void showStatus(HttpServerExchange exchange) throws IOException {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html;charset=utf-8");
    try (OutputStream output = exchange.getOutputStream()) {
      new StatusPage(SERVER_CONFIG) {

        @Override
        public Stream<ConfigurationSection> sections() {
          return Stream.concat(
              Stream.of(
                  new ConfigurationSection("Processors") {

                    @Override
                    public void emit(SectionRenderer renderer) {
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
                            public void emit(SectionRenderer renderer) {
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
        protected void emitCore(SectionRenderer renderer) {
          renderer.line("Is Configuration Good?", scheduler.isConfigurationGood() ? "Yes" : "No");
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
}
