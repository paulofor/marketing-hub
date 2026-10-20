package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.marketinghub.facebookadsworker.facebooktokenrenewal.FacebookTokenRenewalClient.FacebookTokenRenewalStatus;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.reactive.function.client.WebClient;

/** Exercita a passagem real HTTP de elegibilidade e callback com Meta simulada. */
@ExtendWith(OutputCaptureExtension.class)
class FacebookTokenRenewalServiceTest {
    /** Renova duas identidades e não troca o token de campanha em curso nem o revela em logs. */
    @Test
    void renewsTwoAccountsWithoutChangingAnActiveCampaign(CapturedOutput output) throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("[{\"id\":1,\"name\":\"Conta um\",\"appId\":\"app-1\",\"appSecret\":\"secret-1\",\"accessToken\":\"old-1\"},"
                    + "{\"id\":29,\"name\":\"Conta dois\",\"appId\":\"app-29\",\"appSecret\":\"secret-29\",\"accessToken\":\"old-29\"}]"));
            backend.enqueue(json("{\"accessToken\":\"new-secret-one\"}").setResponseCode(202));
            backend.enqueue(json("{\"accessToken\":\"new-secret-two\"}").setResponseCode(202));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            when(graph.renewLongLivedToken("app-1", "secret-1", "old-1"))
                    .thenReturn(new FacebookAdsService.TokenRenewalResponse("new-secret-one", 3600));
            when(graph.renewLongLivedToken("app-29", "secret-29", "old-29"))
                    .thenReturn(new FacebookAdsService.TokenRenewalResponse("new-secret-two", 5184000));
            service(backend, graph).renewTokensIfNeeded();
            assertThat(backend.takeRequest(1, TimeUnit.SECONDS).getPath()).endsWith("/renewal/eligible");
            assertThat(backend.takeRequest(1, TimeUnit.SECONDS).getPath()).contains("/1/token/renewal");
            assertThat(backend.takeRequest(1, TimeUnit.SECONDS).getPath()).contains("/29/token/renewal");
            verify(graph, never()).updateAccessToken(anyString());
            assertThat(output.getAll()).doesNotContain("new-secret-one", "new-secret-two", "secret-1", "secret-29", "old-1", "old-29");
        }
    }

    /** Usa a validade efetiva e não a duração convencional de sessenta dias. */
    @Test
    void persistsExactLifetime() throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("{}").setResponseCode(202));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            when(graph.renewLongLivedToken("app", "secret", "old"))
                    .thenReturn(new FacebookAdsService.TokenRenewalResponse("new", 3600));
            LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);
            var result = service(backend, graph).renewTokenForAccount(4L, "app", "secret", "old", false);
            assertThat(result.status()).isEqualTo(FacebookTokenRenewalStatus.SUCCESS);
            assertThat(result.tokenExpiresAt()).isBetween(before.plusHours(1), LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
        }
    }

    /** Registra falha sem inventar validade quando a Meta não informa duração positiva. */
    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsInvalidLifetime(long seconds) throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("{}").setResponseCode(202));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            when(graph.renewLongLivedToken("app", "secret", "old"))
                    .thenReturn(new FacebookAdsService.TokenRenewalResponse("new", seconds));
            var result = service(backend, graph).renewTokenForAccount(4L, "app", "secret", "old", true);
            assertThat(result.status()).isEqualTo(FacebookTokenRenewalStatus.FAILED);
            assertThat(result.tokenExpiresAt()).isNull();
            assertThat(backend.takeRequest(1, TimeUnit.SECONDS).getBody().readUtf8()).contains("FAILED").doesNotContain("\"new\"");
            verify(graph, never()).updateAccessToken(anyString());
        }
    }

    /** Uma falha de callback não pode ser anunciada como renovação persistida nem trocar estado volátil. */
    @Test
    void failedCallbackIsNotAcknowledged() throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("{}").setResponseCode(503));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            when(graph.renewLongLivedToken("app", "secret", "old"))
                    .thenReturn(new FacebookAdsService.TokenRenewalResponse("new", 3600));
            assertThatThrownBy(() -> service(backend, graph).renewTokenForAccount(4L, "app", "secret", "old", true))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("persistir");
            verify(graph, never()).updateAccessToken(anyString());
        }
    }

    /** Conecta o serviço e o callback ao mesmo backend sintético. */
    private FacebookTokenRenewalService service(MockWebServer backend, FacebookAdsService graph) {
        String base = backend.url("/").toString();
        return new FacebookTokenRenewalService(WebClient.builder(), graph,
                new FacebookTokenRenewalClient(WebClient.builder(), base, "/api"), base, "/api");
    }

    /** Monta uma resposta JSON do backend de teste. */
    private MockResponse json(String body) {
        return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
    }
}
