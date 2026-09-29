package com.marketinghub.facebookadsworker.facebookrecommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient.FacebookWorkerConfiguration;
import com.marketinghub.facebookadsworker.facebooktargeting.TargetingResolverProperties;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Valida a coleta agendada de sugestões oficiais da Meta pelo worker.
 */
class FacebookCampaignRecommendationServiceTest {
    private MockWebServer backend;
    private MockWebServer graph;
    private FacebookCampaignRecommendationService service;

    @BeforeEach
    // Prepara servidores HTTP locais para simular backend e Graph API.
    void setUp() throws Exception {
        backend = new MockWebServer();
        backend.start();
        graph = new MockWebServer();
        graph.start();
        FacebookAdsService facebookAdsService = new FacebookAdsService(
                WebClient.builder(),
                graph.url("/").toString(),
                "v23.0",
                new ObjectMapper(),
                new TargetingResolverProperties());
        service = new FacebookCampaignRecommendationService(
                facebookAdsService,
                null,
                new StaticConfigurationClient(configuration()),
                WebClient.builder(),
                backend.url("/").toString(),
                "/api",
                true);
    }

    @AfterEach
    // Encerra os servidores HTTP locais usados no teste.
    void tearDown() throws Exception {
        backend.shutdown();
        graph.shutdown();
    }

    @Test
    // Garante que o worker busca alvos ativos, consulta a Meta e reporta sugestões ao backend.
    void shouldFetchActiveTargetsAndSendRecommendationsToBackend() throws Exception {
        backend.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"campaignId\":\"camp-1\",\"externalCampaignId\":\"meta-camp-1\",\"experimentId\":42,\"adAccountId\":\"12345\",\"lastSyncedAt\":\"2026-06-10T10:00:00Z\"}]"));
        graph.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"recommendations\":{\"data\":[{\"code\":\"1\",\"title\":\"Ajuste orçamento\"}]}}"));
        backend.enqueue(new MockResponse().setResponseCode(204));

        service.syncActiveCampaignRecommendations();

        var targetRequest = backend.takeRequest();
        assertThat(targetRequest.getMethod()).isEqualTo("GET");
        assertThat(targetRequest.getPath()).isEqualTo("/api/facebook-campaigns/recommendations/sync-targets");
        var graphRequest = graph.takeRequest();
        assertThat(graphRequest.getPath()).contains("/v23.0/meta-camp-1");
        assertThat(graphRequest.getPath()).contains("fields=recommendations");
        var ingestionRequest = backend.takeRequest();
        assertThat(ingestionRequest.getMethod()).isEqualTo("POST");
        assertThat(ingestionRequest.getPath()).isEqualTo("/api/facebook-campaigns/camp-1/recommendations");
        assertThat(ingestionRequest.getBody().readUtf8()).contains("Ajuste orçamento");
    }

    // Cria a configuração operacional mínima do worker para a coleta.
    private FacebookWorkerConfiguration configuration() {
        return new FacebookWorkerConfiguration(
                7L,
                "act_12345",
                "token-123",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Cliente de configuração fixo para evitar chamadas externas durante o teste.
     */
    private static class StaticConfigurationClient extends FacebookWorkerConfigurationClient {
        private final FacebookWorkerConfiguration configuration;

        // Inicializa o cliente fixo com a configuração informada.
        StaticConfigurationClient(FacebookWorkerConfiguration configuration) {
            super(WebClient.builder(), "http://localhost", "/api");
            this.configuration = configuration;
        }

        @Override
        // Retorna sempre a configuração fixa do cenário.
        public Optional<FacebookWorkerConfiguration> fetchConfiguration() {
            return Optional.of(configuration);
        }
    }
}
