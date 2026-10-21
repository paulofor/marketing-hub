package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

/** Reproduz credenciais curtas e impede trocas repetidas na consulta frequente de elegíveis. */
class FacebookTokenRenewalCadenceTest {
    /** Renova a credencial recém-salva de qualquer conta e confirma seu callback próprio. */
    @ParameterizedTest
    @ValueSource(longs = {1, 87})
    void newlySavedShortTokenIsRenewedForAnyAccount(long accountId) throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json(candidate(accountId, "NEVER_ATTEMPTED", null, "2026-10-21T05:00:00")));
            backend.enqueue(json("{}").setResponseCode(202));
            FacebookAdsService graph = graph();
            service(backend, graph).renewTokensIfNeeded();
            verify(graph).renewLongLivedToken("app", "secret", "synthetic-token");
            assertThat(backend.takeRequest(1, TimeUnit.SECONDS).getPath()).endsWith("/renewal/eligible");
            var callback = backend.takeRequest(1, TimeUnit.SECONDS);
            assertThat(callback.getPath()).endsWith("/" + accountId + "/token/renewal");
            ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
            var result = mapper.readTree(callback.getBody().readUtf8());
            assertThat(result.get("status").asText()).isEqualTo("SUCCESS");
            assertThat(mapper.treeToValue(result.get("tokenExpiresAt"), LocalDateTime.class))
                    .isEqualTo(LocalDateTime.parse("2026-12-20T04:00:00"));
        }
    }

    /** Mantém as seis horas e evita repetir falhas ou renovar sucessos ainda longe do vencimento. */
    @ParameterizedTest
    @CsvSource({
        "FAILED,2026-10-21T03:55:00,2026-10-21T04:30:00",
        "FAILED,2026-10-21T03:00:00,2026-10-21T04:05:00",
        "SUCCESS,2026-10-21T03:55:00,2026-10-21T04:10:00",
        "SUCCESS,2026-10-21T03:00:00,2026-10-27T04:00:00",
        "SUCCESS,2026-10-21T03:00:00,2026-10-21T04:31:00",
        "SUCCESS,2026-10-21T04:01:00,2026-10-21T04:10:00"
    })
    void recentAttemptDoesNotTriggerAnotherExchange(String status, String lastAttempt, String expiry) throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json(candidate(23, status, lastAttempt, expiry)));
            FacebookAdsService graph = graph();
            service(backend, graph).renewTokensIfNeeded();
            verifyNoInteractions(graph);
            assertThat(backend.getRequestCount()).isEqualTo(1);
        }
    }

    /** Permite a passagem normal de seis horas e a proteção final de token curto já renovado. */
    @ParameterizedTest
    @CsvSource({
        "FAILED,2026-10-20T22:00:00,2026-10-21T03:00:00",
        "SUCCESS,2026-10-20T22:00:00,2026-10-27T04:00:00",
        "SUCCESS,2026-10-21T03:45:00,2026-10-21T04:30:00",
        "SUCCESS,2026-10-21T03:40:00,2026-10-21T04:10:00"
    })
    void dueAttemptIsExchangedOnce(String status, String lastAttempt, String expiry) throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json(candidate(97, status, lastAttempt, expiry)));
            backend.enqueue(json("{}").setResponseCode(202));
            FacebookAdsService graph = graph();
            service(backend, graph).renewTokensIfNeeded();
            verify(graph).renewLongLivedToken("app", "secret", "synthetic-token");
            assertThat(backend.getRequestCount()).isEqualTo(2);
        }
    }

    /** Preserva a compatibilidade com contrato antigo sem metadados de tentativa. */
    @ParameterizedTest
    @ValueSource(longs = {46, 112})
    void legacyCandidateWithoutAttemptMetadataStillWorks(long id) throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("[{\"id\":" + id + ",\"name\":\"Teste\",\"appId\":\"app\",\"appSecret\":\"secret\",\"accessToken\":\"synthetic-token\"}]"));
            backend.enqueue(json("{}").setResponseCode(202));
            FacebookAdsService graph = graph();
            service(backend, graph).renewTokensIfNeeded();
            verify(graph).renewLongLivedToken("app", "secret", "synthetic-token");
        }
    }

    /** Produz elegibilidade sintética com datas UTC, sem credenciais reais. */
    private String candidate(long id, String status, String lastAttempt, String expiry) {
        String attempted = lastAttempt == null ? "null" : "\"" + lastAttempt + "\"";
        return "[{\"id\":" + id + ",\"name\":\"Teste\",\"appId\":\"app\",\"appSecret\":\"secret\",\"accessToken\":\"synthetic-token\",\"tokenExpiresAt\":\"" + expiry
                + "\",\"tokenRenewalStatus\":\"" + status + "\",\"tokenRenewalLastAttemptAt\":" + attempted + "}]";
    }

    /** Simula a extensão oficial por sessenta dias sem chamar a Meta. */
    private FacebookAdsService graph() {
        FacebookAdsService graph = mock(FacebookAdsService.class);
        when(graph.renewLongLivedToken("app", "secret", "synthetic-token"))
                .thenReturn(new FacebookAdsService.TokenRenewalResponse("synthetic-renewed", 5184000));
        return graph;
    }

    /** Fixa o relógio e conecta os callbacks ao backend HTTP local. */
    private FacebookTokenRenewalService service(MockWebServer backend, FacebookAdsService graph) {
        String base = backend.url("/").toString();
        FacebookTokenRenewalService service = new FacebookTokenRenewalService(WebClient.builder(), graph,
                new FacebookTokenRenewalClient(WebClient.builder(), base, "/api"), base, "/api");
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(Instant.parse("2026-10-21T04:00:00Z"), ZoneOffset.UTC));
        return service;
    }

    /** Monta uma resposta JSON com conteúdo e cabeçalhos reais de transporte. */
    private MockResponse json(String body) {
        return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
    }
}
