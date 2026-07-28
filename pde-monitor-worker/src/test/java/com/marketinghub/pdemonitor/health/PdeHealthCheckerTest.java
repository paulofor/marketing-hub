package com.marketinghub.pdemonitor.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.pdemonitor.config.PdeMonitorProperties;
import com.marketinghub.pdemonitor.db.PdeMonitoredModule;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Valida a verificação HTTP pública feita pelo monitor dedicado de PDEs. */
class PdeHealthCheckerTest {

    @Test
    /** Garante que a URL de monitoramento tem prioridade sobre a URL base. */
    void deveUsarMonitoringUrlComoPrioridade() {
        var checker = checker(3000);
        var module =
                new PdeMonitoredModule(
                        1,
                        "pde-musa-v6",
                        "MUSA v6",
                        "https://base.test",
                        "/healthz",
                        "https://monitor.test/?mh_monitor=1",
                        120);

        assertThat(checker.targetUrl(module)).isEqualTo("https://monitor.test/?mh_monitor=1");
    }

    @Test
    /** Garante que resposta HTTP bem-sucedida vira status online. */
    void deveClassificarRespostaDoisXXComoOnline() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            var module =
                    new PdeMonitoredModule(
                            1, "pde-musa-v6", "MUSA v6", server.url("/").toString(), "/healthz", null, 120);

            var result = checker(3000).check(module);

            assertThat(result.status()).isEqualTo("ONLINE");
            assertThat(result.httpStatus()).isEqualTo(200);
        }
    }

    @Test
    /** Garante que erro HTTP vira degradação operacional. */
    void deveClassificarErroHttpComoInstavel() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(503).setBody("down"));
            server.start();
            var module =
                    new PdeMonitoredModule(
                            1, "pde-musa-v6", "MUSA v6", server.url("/").toString(), "/healthz", null, 120);

            var result = checker(3000).check(module);

            assertThat(result.status()).isEqualTo("DEGRADED");
            assertThat(result.httpStatus()).isEqualTo(503);
        }
    }

    /** Cria o verificador HTTP com limite de degradação parametrizado. */
    private PdeHealthChecker checker(long degradedResponseTimeMs) {
        return new PdeHealthChecker(
                WebClient.builder(),
                new PdeMonitorProperties(
                        new PdeMonitorProperties.Http(Duration.ofSeconds(2), degradedResponseTimeMs),
                        new PdeMonitorProperties.Incident("CRITICAL")));
    }
}
