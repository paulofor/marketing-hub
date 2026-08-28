package com.marketinghub.videomanagement.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a publicação segura da prontidão operacional de Apolo. */
class ApolloExecutorHealthReporterTest {

    /** Confirma que sessão válida e versão implantada são enviadas ao contrato canônico. */
    @Test
    void reportsAuthenticatedCodexSession() throws IOException, InterruptedException {
        try (MockWebServer backend = new MockWebServer()) {
            backend.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
            ApolloExecutorHealthReporter reporter = new AuthenticatedReporter(backend.url("/").toString());

            reporter.report();

            var request = backend.takeRequest(1, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getPath()).isEqualTo("/api/internal/agents/executor-health");
            assertThat(request.getBody().readUtf8())
                    .contains("\"agentKey\":\"videomaker\"")
                    .contains("\"deployedVersion\":3")
                    .contains("\"codexAuthenticated\":true");
        }
    }

    /** Simula uma sessão Codex válida sem executar processo externo durante o teste. */
    private static final class AuthenticatedReporter extends ApolloExecutorHealthReporter {
        private AuthenticatedReporter(String backendUrl) {
            super(backendUrl, "videomaker", 3, "sha-test");
        }

        /** Informa autenticação válida para isolar o contrato HTTP. */
        @Override
        boolean codexAuthenticated() {
            return true;
        }
    }
}
