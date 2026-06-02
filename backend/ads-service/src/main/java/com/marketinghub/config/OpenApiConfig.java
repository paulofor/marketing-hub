package com.marketinghub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura a publicação da documentação OpenAPI do backend principal pelo próprio Spring Boot.
 */
@Configuration
public class OpenApiConfig {

    private static final String BACKEND_DESCRIPTION = "Documentação dinâmica dos endpoints publicados pelo backend principal do Marketing Hub.";

    /**
     * Define os metadados exibidos no Swagger UI e no documento OpenAPI dinâmico.
     */
    @Bean
    public OpenAPI marketingHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Marketing Hub Backend API")
                        .version("v1")
                        .description(BACKEND_DESCRIPTION))
                .servers(List.of(
                        new Server().url("/").description("Servidor atual do backend Spring Boot")));
    }

    /**
     * Publica todos os endpoints HTTP do backend principal no grupo padrão do Swagger UI.
     */
    @Bean
    public GroupedOpenApi marketingHubBackendApi() {
        return GroupedOpenApi.builder()
                .group("marketing-hub-backend")
                .pathsToMatch("/**")
                .pathsToExclude("/ops-mh-observability-v2/**", "/error")
                .build();
    }
}
