package com.marketinghub.opsmonitor.pipeline.healthcheck;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class HealthCheckProcessorTest {

    @Test
    void deveClassificarSucessoComoOnline() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"status\":\"UP\"}"));
            server.start();
            var processor = new HealthCheckProcessor(WebClient.builder().build());

            var output = processor.process(StageContext.simple("stage-1", "backend"), new HealthCheckInput("backend", server.url("/actuator/health").toString(), Duration.ofSeconds(2)));

            assertThat(output.status()).isEqualTo("ONLINE");
            assertThat(output.httpStatus()).isEqualTo(200);
        }
    }

    @Test
    void deveClassificarTimeoutComoOffline() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("late").setBodyDelay(300, java.util.concurrent.TimeUnit.MILLISECONDS));
            server.start();
            var processor = new HealthCheckProcessor(WebClient.builder().build());

            var output = processor.process(StageContext.simple("stage-2", "ai-worker"), new HealthCheckInput("ai-worker", server.url("/health").toString(), Duration.ofMillis(50)));

            assertThat(output.status()).isEqualTo("OFFLINE");
            assertThat(output.errorMessage()).isNotBlank();
        }
    }
}
