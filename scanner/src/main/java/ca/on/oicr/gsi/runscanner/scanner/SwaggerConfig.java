package ca.on.oicr.gsi.runscanner.scanner;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.servlet.ServletContext;
import java.util.Collections;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.configuration.SpringDocUIConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.MultipleOpenApiSupportConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@Import({
  SpringDocConfiguration.class,
  SpringDocConfigProperties.class,
  SpringDocSpecPropertiesConfiguration.class,
  SpringDocWebMvcConfiguration.class,
  MultipleOpenApiSupportConfiguration.class,
  org.springdoc.webmvc.ui.SwaggerConfig.class,
  SwaggerUiConfigProperties.class,
  SwaggerUiOAuthProperties.class,
  SpringDocUIConfiguration.class
})
public class SwaggerConfig {
  String projectName = "Run Scanner";

  @Value("${project.version}")
  String projectVersion;

  @Bean
  public GroupedOpenApi api() {
    return GroupedOpenApi.builder()
        .group("API")
        .packagesToScan("ca.on.oicr.gsi.runscanner")
        .build();
  }

  @Bean
  public OpenAPI openApi(
      ServletContext servletContext, @Value("${swagger.baseUrl:#{null}}") String baseUrl) {
    Server server =
        new Server()
            .url(baseUrl != null ? baseUrl : servletContext.getContextPath())
            .description("Default server URL");
    return new OpenAPI()
        .info(new Info().title(projectName).version(projectVersion))
        .servers(Collections.singletonList(server));
  }
}
