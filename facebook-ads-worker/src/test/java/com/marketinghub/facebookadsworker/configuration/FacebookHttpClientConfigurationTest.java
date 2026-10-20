package com.marketinghub.facebookadsworker.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import com.marketinghub.facebookadsworker.metaaudience.MetaAudienceBackendClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Reproduz a consulta de públicos travada e comprova recuperação do transporte sem espera infinita. */
class FacebookHttpClientConfigurationTest {
    /** Repete o caso original sem resposta e depois consulta outra fila com sucesso. */
    @Test
    void stalledAudienceQueryTimesOutAndNextCallRecovers() throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            backend.enqueue(new MockResponse().addHeader("Content-Type", "application/json").setBody("[]"));
            WebClient.Builder builder = clientBuilder();
            MetaAudienceBackendClient client = new MetaAudienceBackendClient(builder, backend.url("/").toString(), "/api");
            assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                    assertThatThrownBy(() -> client.listPending(5)).isInstanceOf(IllegalStateException.class));
            assertThat(client.listPending(7)).isEmpty();
            assertThat(backend.takeRequest(1, TimeUnit.SECONDS).getPath()).endsWith("/pending?limit=5");
            assertThat(backend.takeRequest(1, TimeUnit.SECONDS).getPath()).endsWith("/pending?limit=7");
        }
    }

    /** Confere que receber cabeçalhos não permite esperar indefinidamente pelo corpo. */
    @Test
    void stalledBodyAlsoTimesOut() throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(new MockResponse().setBody("[]").setBodyDelay(2, TimeUnit.SECONDS)
                    .addHeader("Content-Type", "application/json"));
            MetaAudienceBackendClient client = new MetaAudienceBackendClient(clientBuilder(), backend.url("/").toString(), "/api");
            assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                    assertThatThrownBy(() -> client.listPending(5)).isInstanceOf(RuntimeException.class));
        }
    }

    /** Configura o mesmo customizador da aplicação com limites curtos somente neste teste. */
    private WebClient.Builder clientBuilder() {
        WebClient.Builder builder = WebClient.builder();
        new FacebookHttpClientConfiguration().facebookHttpTimeouts(
                Duration.ofSeconds(1), Duration.ofMillis(250)).customize(builder);
        return builder;
    }
}
