package ca.on.oicr.gsi.runscanner.scanner;

import java.text.SimpleDateFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class WebConfig {

  @Bean(name = "jsonMapper")
  public JsonMapper jsonMapper() {
    return JsonMapper.builder()
        .defaultDateFormat(new SimpleDateFormat("hh:MM:ss'T'HH:mm:ssXXX"))
        .enable(SerializationFeature.INDENT_OUTPUT)
        .build();
  }
}
