package com.marketinghub.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

/**
 * Valida a configuração de publicação Swagger/OpenAPI do backend principal.
 */
class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    /**
     * Verifica os metadados expostos no documento OpenAPI dinâmico do backend principal.
     */
    @Test
    void marketingHubOpenApiShouldDescribeBackendSwagger() {
        OpenAPI openAPI = config.marketingHubOpenApi();

        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Marketing Hub Backend API");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openAPI.getServers())
                .singleElement()
                .satisfies(server -> assertThat(server.getUrl()).isEqualTo("/"));
    }

    /**
     * Verifica o grupo do Swagger UI usado para publicar os endpoints do backend principal.
     */
    @Test
    void marketingHubBackendApiShouldPublishBackendGroup() {
        GroupedOpenApi groupedOpenApi = config.marketingHubBackendApi();

        assertThat(groupedOpenApi.getGroup()).isEqualTo("marketing-hub-backend");
        assertThat(groupedOpenApi.getPathsToMatch()).containsExactly("/**");
        assertThat(groupedOpenApi.getPathsToExclude()).contains("/ops-mh-observability-v2/**", "/error");
    }
}
