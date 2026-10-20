package com.marketinghub.facebookadsworker.facebooktokenrenewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.marketinghub.facebookadsworker.FacebookAdsService;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.test.util.ReflectionTestUtils;

/** Reproduz indisponibilidade transitória do backend durante a inicialização da renovação. */
class FacebookTokenRenewalRecoveryTest {
    /** Aguarda a recuperação da consulta e troca a credencial uma única vez. */
    @Test
    void backendRecoversBeforeNextSixHourCycle() throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("{}").setResponseCode(503));
            backend.enqueue(json("[{\"id\":82,\"name\":\"Conta teste\",\"appId\":\"app\",\"appSecret\":\"secret\",\"accessToken\":\"old\"}]"));
            backend.enqueue(json("{}").setResponseCode(202));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            when(graph.renewLongLivedToken("app", "secret", "old"))
                    .thenReturn(new FacebookAdsService.TokenRenewalResponse("new", 3600));
            service(backend, graph).renewTokensIfNeeded();
            verify(graph).renewLongLivedToken("app", "secret", "old");
            assertThat(backend.getRequestCount()).isEqualTo(3);
        }
    }

    /** Reproduz conexão recusada: o backend nasce após a primeira falha e a mesma passagem continua. */
    @Test
    void connectionRefusedRecoversWhenBackendStarts() throws Exception {
        int port;
        try (ServerSocket reservation = new ServerSocket(0)) {
            port = reservation.getLocalPort();
        }
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("[{\"id\":203,\"name\":\"Outra conta\",\"appId\":\"app\",\"appSecret\":\"secret\",\"accessToken\":\"old\"}]"));
            backend.enqueue(json("{}").setResponseCode(202));
            AtomicInteger refused = new AtomicInteger();
            WebClient.Builder builder = WebClient.builder().filter((request, next) -> next.exchange(request)
                    .doOnError(error -> {
                        refused.incrementAndGet();
                        assertDoesNotThrow(() -> backend.start(port));
                    }));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            when(graph.renewLongLivedToken("app", "secret", "old"))
                    .thenReturn(new FacebookAdsService.TokenRenewalResponse("new", 3600));
            String base = "http://127.0.0.1:" + port;
            FacebookTokenRenewalService service = new FacebookTokenRenewalService(builder, graph,
                    new FacebookTokenRenewalClient(WebClient.builder(), base, "/api"), base, "/api");
            ReflectionTestUtils.setField(service, "backendRetryDelayMs", 10L);
            service.renewTokensIfNeeded();
            assertThat(refused.get()).isEqualTo(1);
            assertThat(backend.getRequestCount()).isEqualTo(2);
            verify(graph).renewLongLivedToken("app", "secret", "old");
        }
    }

    /** Uma recusa permanente de acesso não permite repetição cega. */
    @Test
    void permanentBackendRefusalIsNotRetried() throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(json("{}").setResponseCode(403));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            service(backend, graph).renewTokensIfNeeded();
            assertThat(backend.getRequestCount()).isEqualTo(1);
            verifyNoInteractions(graph);
        }
    }

    /** Mesmo a falha transitória persistente tem limite e não inicia troca Meta. */
    @Test
    void transientRetriesAreBounded() throws Exception {
        try (MockWebServer backend = new MockWebServer()) {
            for (int i = 0; i < 6; i++) backend.enqueue(json("{}").setResponseCode(503));
            FacebookAdsService graph = mock(FacebookAdsService.class);
            service(backend, graph).renewTokensIfNeeded();
            assertThat(backend.getRequestCount()).isEqualTo(6);
            verifyNoInteractions(graph);
        }
    }

    /** Conecta o serviço ao backend sintético sem alterar o contrato de produção. */
    private FacebookTokenRenewalService service(MockWebServer backend, FacebookAdsService graph) {
        String base = backend.url("/").toString();
        FacebookTokenRenewalService service = new FacebookTokenRenewalService(WebClient.builder(), graph,
                new FacebookTokenRenewalClient(WebClient.builder(), base, "/api"), base, "/api");
        ReflectionTestUtils.setField(service, "backendRetryDelayMs", 10L);
        return service;
    }

    /** Monta uma resposta JSON do backend de teste. */
    private MockResponse json(String body) {
        return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
    }
}
