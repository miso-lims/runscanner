package ca.on.oicr.gsi.runscanner.scanner;

import ca.on.oicr.gsi.runscanner.dto.NotificationDto;
import ca.on.oicr.gsi.runscanner.scanner.processor.RunProcessor;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import tools.jackson.databind.json.JsonMapper;

/**
 * Parses a notification DTO stored in a file and outputs it to the console, for debugging purposes.
 *
 * <p>Suppress warnings: "squid:S4823" warns whenever command line arguments are used. "squid:S106"
 * warns whenever System.out or System.err is used (requests a logging engine is used instead).
 * "squid:S1148" warns whenever Throwable.printStackTrace() is called.
 */
@SuppressWarnings({"squid:S4823", "squid:S106", "squid:S1148"})
public class ParseNotificationJson {

  public static void main(String[] args) throws IOException {
    JsonMapper mapper = RunProcessor.createJsonMapper();

    List<NotificationDto> dtos =
        Arrays.stream(args)
            .map(File::new)
            .map(f -> mapper.readValue(f, NotificationDto.class))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    mapper.writeValue(System.out, dtos);
  }
}
