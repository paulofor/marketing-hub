package com.marketinghub.experiment.monitoring.pde;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Consulta o backend PDE por HTTP para obter métricas administrativas do funil. */
@Component
public class PdeAnalyticsHttpClient implements PdeAnalyticsClient {

    private final RestClient restClient;

    /** Inicializa o cliente HTTP com timeouts curtos para não travar o painel do Hub. */
    public PdeAnalyticsHttpClient(
            @Value("${integrations.pde-platform.base-url:http://191.252.181.168:8096}") String baseUrl) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(4));
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .requestFactory(requestFactory)
                .build();
    }

    /** Busca o resumo consolidado de analytics do produto no backend PDE. */
    @Override
    public PdeAnalyticsSummary fetchSummary(String productSlug) {
        return restClient.get()
                .uri("/api/pde/access/analytics/{productSlug}/summary", productSlug)
                .retrieve()
                .body(PdeAnalyticsSummary.class);
    }

    /** Remove barra final para montar rotas internas de forma previsível. */
    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://191.252.181.168:8096";
        }
        return value.replaceAll("/+$", "");
    }
}
