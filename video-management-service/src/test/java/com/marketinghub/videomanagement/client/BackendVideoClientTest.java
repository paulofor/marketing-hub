package com.marketinghub.videomanagement.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.videomanagement.client.dto.SalesVideoProfile;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.VideoJobObservabilityService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP usado pelo worker de vídeo contra o backend. */
class BackendVideoClientTest {
    private MockWebServer server;

    /** Inicializa o backend simulado antes de cada teste de contrato. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado para liberar a porta usada pelo teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve buscar perfil pela rota interna de vídeo, sem depender da rota administrativa com tenant. */
    @Test
    void shouldFetchProfileThroughInternalVideoEndpoint() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": 3,
                          "productId": 63,
                          "videoKind": "HERO",
                          "title": "Video do experimento",
                          "status": "SCRIPT_READY"
                        }
                        """));
        BackendVideoClient client = new BackendVideoClient(
                WebClient.builder(),
                properties(),
                new VideoJobObservabilityService(new SimpleMeterRegistry()));

        SalesVideoProfile profile = client.fetchProfile(3L);

        assertThat(profile.id()).isEqualTo(3L);
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/internal/video/sales-videos/profiles/3");
    }

    /** Configura o client para apontar para o backend simulado. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.setBackendBaseUrl(URI.create(server.url("/").toString()));
        return properties;
    }
}
