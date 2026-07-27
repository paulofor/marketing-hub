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

    @Test
    void deveClassificarErroHttpComoInstavel() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(503).setBody("service unavailable"));
            server.start();
            var processor = new HealthCheckProcessor(WebClient.builder().build());

            var output = processor.process(StageContext.simple("stage-3", "lead-portal"),
                    new HealthCheckInput("lead-portal", server.url("/health").toString(), Duration.ofSeconds(2)));

            assertThat(output.status()).isEqualTo("DEGRADED");
            assertThat(output.httpStatus()).isEqualTo(503);
        }
    }

    @Test
    void deveClassificarHlsComSegmentoIndisponivelComoInstavel() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("#EXTM3U\n#EXTINF:4,\nsegment-000.ts\n"));
            server.enqueue(new MockResponse().setResponseCode(404));
            server.start();
            var processor = new HealthCheckProcessor(WebClient.builder().build());

            var output = processor.process(StageContext.simple("stage-hls", "pde-musa-v6-hls"),
                    new HealthCheckInput("pde-musa-v6-hls", server.url("/assets/hls/v6/index.m3u8").toString(), Duration.ofSeconds(2)));

            assertThat(output.status()).isEqualTo("DEGRADED");
            assertThat(output.httpStatus()).isEqualTo(200);
        }
    }
}
