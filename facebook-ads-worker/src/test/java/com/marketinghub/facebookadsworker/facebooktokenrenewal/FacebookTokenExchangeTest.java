package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.facebookadsworker.FacebookAdsService;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.reactive.function.client.WebClient;

/** Confere o contrato oficial da troca Meta e a proteção de credenciais no erro HTTP real. */
@ExtendWith(OutputCaptureExtension.class)
class FacebookTokenExchangeTest {
    /** Mantém a causa de expiração e sua pilha sem revelar credenciais na URL ou no corpo. */
    @Test
    void graphErrorPreservesCauseWithoutLeakingCredentials(CapturedOutput output) throws Exception {
        try (MockWebServer graph = new MockWebServer()) {
            graph.enqueue(new MockResponse().setResponseCode(400).addHeader("Content-Type", "application/json")
                    .setBody("{\"error\":{\"code\":190,\"message\":\"expired synthetic-old-token synthetic-app-secret\"}}"));
            FacebookAdsService service = new FacebookAdsService(WebClient.builder(), graph.url("/").toString(),
                    "v23.0", new ObjectMapper(), null);
            assertThatThrownBy(() -> service.renewLongLivedToken("test-app", "synthetic-app-secret", "synthetic-old-token"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("190", "expired")
                    .hasMessageNotContaining("synthetic-old-token")
                    .hasMessageNotContaining("synthetic-app-secret");
            assertThat(output.getAll()).doesNotContain("synthetic-old-token", "synthetic-app-secret");
            assertThat(graph.takeRequest().getRequestUrl().queryParameter("grant_type")).isEqualTo("fb_exchange_token");
        }
    }
}
