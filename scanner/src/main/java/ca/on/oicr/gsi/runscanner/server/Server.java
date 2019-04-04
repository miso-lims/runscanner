package ca.on.oicr.gsi.runscanner.server;

import ca.on.oicr.gsi.runscanner.server.util.RestController;
import ca.on.oicr.gsi.runscanner.server.util.Scheduler;
import ca.on.oicr.gsi.runscanner.server.util.UserInterfaceController;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import java.io.PrintWriter;

public class Server {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println(
          "Usage: java -m ca.on.oicr.gsi.runscanner.server/ca.on.oicr.gsi.runscanner.server.Server /path/to/config/file.json port");
      System.exit(1);
      return;
    }
    DefaultExports.initialize();
    final var scheduler = new Scheduler();
    scheduler.setConfigurationFile(args[0]);
    final var uiController = new UserInterfaceController(scheduler);
    final var restContoller = new RestController(scheduler);

    final var path = Handlers.routing();
    path.get(
        "/metrics",
        t -> {
          t.getResponseHeaders().put(Headers.CONTENT_TYPE, TextFormat.CONTENT_TYPE_004);
          try (final var os = t.getOutputStream();
              final var writer = new PrintWriter(os)) {
            TextFormat.write004(writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
          }
        });

    path.get("/favicon.ico", resource("image/x-icon", "favicon.ico"));
    path.get("/style.css", resource("text/css", "style.css"));

    path.get("/", uiController::showStatus);
    path.get("/listScanned", uiController::listScannedRuns);
    UserInterfaceController.collections()
        .skip(1)
        .forEach(
            collection ->
                path.get(
                    "/list" + collection.first(),
                    exchange -> uiController.listPaths(collection.second(), exchange)));

    path.get("/runs/all", restContoller::list);
    path.get(
        "/run/{name}",
        exchange -> {
          final var pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
          restContoller.getByName(pathMatch.getParameters().get("name"), exchange);
        });
    path.get(
        "/run/{name}/metrics",
        exchange -> {
          final var pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
          restContoller.getMetricsByName(pathMatch.getParameters().get("name"), exchange);
        });
    path.post("/runs/progressive", new BlockingHandler(restContoller::progressive));

    final var server =
        Undertow.builder()
            .addHttpListener(Integer.parseInt(args[1]), "localhost")
            .setHandler(path)
            .build();
    scheduler.start();
    server.start();
  }

  private static HttpHandler resource(String type, String resouceName) {
    return exchange -> {
      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, type);
      try (final var output = exchange.getOutputStream();
          final var input = Server.class.getResourceAsStream(resouceName)) {
        final var buffer = new byte[4096];
        int len;
        while ((len = input.read(buffer)) > 0) {
          output.write(buffer, 0, len);
        }
      }
    };
  }
}
