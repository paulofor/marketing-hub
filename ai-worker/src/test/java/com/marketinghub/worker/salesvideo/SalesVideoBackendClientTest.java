package com.marketinghub.worker.salesvideo;

import static org.assertj.core.api.Assertions.assertThat;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar os contratos HTTP do client de vídeo no ai-worker. */
class SalesVideoBackendClientTest {

    private MockWebServer server;

    /** Inicializa o backend simulado usado para capturar as chamadas do client. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado após cada teste. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve buscar perfil pelo endpoint interno do ai-worker, sem usar rota administrativa com tenant. */
    @Test
    void getProfileShouldUseInternalAiEndpoint() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":59,\"title\":\"Manicure em domicílio\"}"));
        SalesVideoBackendClient client = newClient();

        var profile = client.getProfile(59L);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/internal/ai/sales-videos/profiles/59");
        assertThat(profile.getId()).isEqualTo(59L);
        assertThat(profile.getTitle()).isEqualTo("Manicure em domicílio");
    }

    /** Cria o client de vídeo apontando para o backend simulado. */
    private SalesVideoBackendClient newClient() {
        return new SalesVideoBackendClient(
                WebClient.builder(),
                server.url("/").toString(),
                "/api",
                "/internal");
    }
}
