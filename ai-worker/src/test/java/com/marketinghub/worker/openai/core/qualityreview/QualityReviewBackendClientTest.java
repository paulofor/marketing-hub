package com.marketinghub.worker.openai.core.qualityreview;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.model.StageExecution;
import java.time.Duration;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP do client quality-review. */
class QualityReviewBackendClientTest {

    private MockWebServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Inicializa o backend simulado usado para retornar jobs pendentes de quality-review. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado após cada teste do client quality-review. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve expor somente o HTML final canônico para renderização visual e prompt enxuto. */
    @Test
    void listPendingShouldExposeLeanQualityReviewInputsForScreenshotRendering() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {
                            "experimentId": 36,
                            "jobid": "job-quality-1",
                            "stageCode": "landing-page-quality-review",
                            "executionRequestedAt": "2026-06-03T03:30:00Z",
                            "experimentName": "Experimento visual",
                            "hypothesisTitle": "Hipótese visual",
                            "landingPageWireframe": {"sectionOrder": [{"sectionId": "hero", "purpose": "Promessa"}]},
                            "landingPageDesignPreset": {"presetId": "premium", "sectionPresets": [{"sectionId": "hero"}]},
                            "htmlGeraLanding": "<!doctype html><html><body><h1>Landing final</h1></body></html>"
                          }
                        ]
                        """));
        QualityReviewBackendClient client = new QualityReviewBackendClient(
                WebClient.builder(),
                properties(),
                objectMapper);

        List<StageExecution<QualityReviewInput>> pending = client.listPending(5);

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().input().landingHtml())
                .isEqualTo("<!doctype html><html><body><h1>Landing final</h1></body></html>");
        assertThat(pending.getFirst().input().promptData())
                .containsOnlyKeys("htmlGeraLanding")
                .containsEntry("htmlGeraLanding", "<!doctype html><html><body><h1>Landing final</h1></body></html>")
                .doesNotContainKeys(
                        "experimentName",
                        "hypothesisTitle",
                        "experimentId",
                        "landingPageHtml",
                        "landingPageWireframe",
                        "landingPageDesignPreset",
                        "CASE_DATA_BLOCK",
                        "landingPageImageAssets",
                        "adCopy",
                        "adImageBriefing");
    }

    /** Deve manter compatibilidade com o contrato legado aninhado quando o backend ainda enviar experiment. */
    @Test
    void listPendingShouldReadLegacyNestedExperimentWhenLeanFieldsAreAbsent() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {
                            "experimentId": 37,
                            "jobid": "job-quality-2",
                            "stageCode": "landing-page-quality-review",
                            "executionRequestedAt": "2026-06-04T22:32:02Z",
                            "experiment": {
                              "id": 37,
                              "htmlGeraLanding": "<!doctype html><html><body><h1>HTML canônico</h1></body></html>",
                              "landingPageHtml": null
                            }
                          }
                        ]
                        """));
        QualityReviewBackendClient client = new QualityReviewBackendClient(
                WebClient.builder(),
                properties(),
                objectMapper);

        List<StageExecution<QualityReviewInput>> pending = client.listPending(5);

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().input().landingHtml())
                .isEqualTo("<!doctype html><html><body><h1>HTML canônico</h1></body></html>");
        assertThat(pending.getFirst().input().promptData())
                .containsOnlyKeys("htmlGeraLanding")
                .doesNotContainKeys("landingPageHtml");
    }

    /** Monta propriedades mínimas para o client quality-review usado no teste. */
    private QualityReviewWorkerProperties properties() {
        return new QualityReviewWorkerProperties(
                true,
                5,
                server.url("/").toString(),
                "/api",
                "prompts/geralanding/landing-page-quality-review.md",
                "prompts/geralanding/landing-page-quality-review-schema.json",
                "experiment_pipeline_landing_page_quality_review",
                "gpt-5.5",
                "original",
                "default",
                Duration.ofSeconds(5),
                "/usr/bin/chromium",
                Duration.ofSeconds(5),
                52_428_800);
    }
}
