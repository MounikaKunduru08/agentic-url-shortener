package com.schwab.assignment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {
  @Bean OpenAPI urlShortenerOpenApi() { return new OpenAPI().info(new Info().title("Agentic URL Shortener API").version("1.0.0").description("Governed URL shortening and SDLC workflow orchestration.")); }
}
