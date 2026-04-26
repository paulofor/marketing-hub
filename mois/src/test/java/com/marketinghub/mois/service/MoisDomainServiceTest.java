package com.marketinghub.mois.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.mois.dto.MoisArtifactDtos;
import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisWorkspaceDtos;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoisDomainServiceTest {

    private MoisDomainService service;
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        service = new MoisDomainService();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/oferta-a", new HtmlHandler("""
                <html><head><title>Oferta A</title></head>
                <body>
                  <h1>Ganhe resultado em 21 dias</h1>
                  <p>Nosso método validado tem prova com +120 alunos e 4.9 de avaliação.</p>
                  <p>Inscreva-se agora no checkout com preço R$97.</p>
                </body></html>
                """));
        server.createContext("/oferta-b", new HtmlHandler("""
                <html><head><title>Oferta B</title></head>
                <body>
                  <h1>Transforme sua rotina</h1>
                  <p>Framework prático em 3 passos com estudos de caso e depoimentos.</p>
                  <p>Garanta a vaga hoje por R$197.</p>
                </body></html>
                """));
        server.createContext("/oferta-duplicada", new HtmlHandler("""
                <html><head><title>Oferta A</title></head>
                <body>
                  <h1>Ganhe resultado em 21 dias</h1>
                  <p>Nosso método validado tem prova com +120 alunos e 4.9 de avaliação.</p>
                  <p>Inscreva-se agora no checkout com preço R$97.</p>
                </body></html>
                """));
        server.createContext("/fraco", new HtmlHandler("""
                <html><head><title>Página fraca</title></head>
                <body><p>Texto institucional sem sinais claros.</p></body></html>
                """));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void shouldBuildConsolidatedInsightReportWithRealSignals() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Fisioterapia",
                        "Dor lombar",
                        "Alívio de dor",
                        List.of("dor lombar oferta"),
                        List.of(baseUrl + "/oferta-a", baseUrl + "/oferta-b"),
                        List.of("META_ADS"),
                        "BR",
                        "pt-BR",
                        null));

        service.runDiscoveryRequest(accepted.requestId());

        MoisInsightDtos.InsightReportResponse report = service.getInsightReport("mois-report-" + accepted.requestId()).orElseThrow();

        assertThat(report.requestSummary().requestId()).isEqualTo(accepted.requestId());
        assertThat(report.offersAnalyzed()).hasSize(2);
        assertThat(report.repeatedPromises()).isNotEmpty();
        assertThat(report.pricingPatterns()).isNotEmpty();
        assertThat(report.saturationSignals()).isNotEmpty();
        assertThat(report.frameworkRecommendation()).isNotNull();
        assertThat(report.saturationNotes()).isNotEmpty();
        assertThat(report.recommendedNextActions()).isNotEmpty();
    }

    @Test
    void shouldKeepRankingStableForRepeatedPatterns() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Fisioterapia",
                        "Dor lombar",
                        "Alívio de dor",
                        List.of("dor lombar oferta"),
                        List.of(baseUrl + "/oferta-a", baseUrl + "/oferta-duplicada", baseUrl + "/oferta-b"),
                        List.of("META_ADS"),
                        "BR",
                        "pt-BR",
                        null));

        service.runDiscoveryRequest(accepted.requestId());

        MoisInsightDtos.InsightReportResponse report = service.getInsightReport("mois-report-" + accepted.requestId()).orElseThrow();
        assertThat(report.repeatedPromises()).isNotEmpty();
        assertThat(report.repeatedPromises().getFirst().share()).isGreaterThanOrEqualTo(0.5);
        assertThat(report.gapOpportunities()).isNotEmpty();
        assertThat(report.gapOpportunities().getFirst().confidence()).isGreaterThan(0.6);
    }

    @Test
    void shouldDeduplicateOffersByCanonicalUrlAndContentSignature() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Nutrição",
                        "Perda de peso",
                        "Resultado rápido",
                        List.of(),
                        List.of(baseUrl + "/oferta-a?ref=1", baseUrl + "/oferta-a?ref=2"),
                        List.of("SEARCH"),
                        "BR",
                        "pt-BR",
                        null));

        service.runDiscoveryRequest(accepted.requestId());

        assertThat(service.listOffers(accepted.requestId(), null, null).items()).hasSize(1);
    }

    @Test
    void shouldHandleInvalidSourceAndMarkRequestAsFailed() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Nutrição",
                        "Perda de peso",
                        "Resultado rápido",
                        List.of(),
                        List.of("file:///invalid-source"),
                        List.of("SEARCH"),
                        "BR",
                        "pt-BR",
                        null));

        service.runDiscoveryRequest(accepted.requestId());

        MoisDiscoveryDtos.DiscoveryRequestDetailResponse detail = service.getDiscoveryRequest(accepted.requestId()).orElseThrow();
        assertThat(detail.status()).isEqualTo("FAILED");
        assertThat(service.listOffers(accepted.requestId(), null, null).items()).isEmpty();
    }

    @Test
    void shouldGenerateLowConfidenceWhenSignalsAreWeak() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Gestão",
                        "Produtividade",
                        "Melhorar rotina",
                        List.of(),
                        List.of(baseUrl + "/fraco"),
                        List.of("SEARCH"),
                        "BR",
                        "pt-BR",
                        null));

        service.runDiscoveryRequest(accepted.requestId());

        MoisInsightDtos.InsightReportListResponse matching = service.listInsightReports(null, null, "DIGITAL_PRODUCT");
        assertThat(matching.items()).hasSize(1);

        String offerId = service.listOffers(accepted.requestId(), null, null).items().getFirst().offerId();
        var offer = service.getOffer(offerId).orElseThrow();
        assertThat(offer.confidence()).isLessThan(0.7);
        assertThat(offer.evidenceRefs()).hasSize(1);
    }

    @Test
    void shouldBeIdempotentWhenRunningSameRequestTwice() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Nutrição",
                        "Perda de peso",
                        "Resultado rápido",
                        List.of(),
                        List.of(baseUrl + "/oferta-a"),
                        List.of("SEARCH"),
                        "BR",
                        "pt-BR",
                        null));

        MoisDiscoveryDtos.AsyncAcceptedResponse firstRun = service.runDiscoveryRequest(accepted.requestId()).orElseThrow();
        MoisDiscoveryDtos.AsyncAcceptedResponse secondRun = service.runDiscoveryRequest(accepted.requestId()).orElseThrow();

        assertThat(firstRun.correlationId()).isEqualTo(secondRun.correlationId());
        assertThat(service.listOffers(accepted.requestId(), null, null).items()).hasSize(1);
    }

    @Test
    void shouldExposeInsightReportArtifactWithCanonicalEnvelope() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Fisioterapia",
                        "Dor lombar",
                        "Alívio de dor",
                        List.of("dor lombar oferta"),
                        List.of(baseUrl + "/oferta-a"),
                        List.of("META_ADS"),
                        "BR",
                        "pt-BR",
                        null));
        service.runDiscoveryRequest(accepted.requestId());

        MoisArtifactDtos.ArtifactEnvelopeResponse artifact = service.getArtifact("mois-report-" + accepted.requestId()).orElseThrow();
        assertThat(artifact.artifactType()).isEqualTo("mois.marketOfferInsightReport.v1");
        assertThat(artifact.schemaVersion()).isEqualTo("v1");
        assertThat(artifact.createdBy()).isEqualTo("mois-system");
    }

    @Test
    void shouldExposeExecutiveSummaryForConsumers() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Fisioterapia",
                        "Dor lombar",
                        "Alívio de dor",
                        List.of("dor lombar oferta"),
                        List.of(baseUrl + "/oferta-a", baseUrl + "/oferta-b"),
                        List.of("META_ADS"),
                        "BR",
                        "pt-BR",
                        null));
        service.runDiscoveryRequest(accepted.requestId());

        MoisInsightDtos.InsightExecutiveSummaryResponse summary =
                service.getInsightExecutiveSummary("mois-report-" + accepted.requestId()).orElseThrow();
        assertThat(summary.frameworkRecommendation().dominantPain()).isEqualTo("Alívio de dor");
        assertThat(summary.decisionReadyActions()).isNotEmpty();
    }

    @Test
    void shouldCreateAndListCollectionJobsInMoisModule() {
        MoisWorkspaceDtos.CollectionJobResponse created = service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-001",
                        "nutricao",
                        "perda de gordura",
                        List.of("CLICKBANK", "JVZOO"),
                        "LAST_7_DAYS",
                        null,
                        "pt-BR",
                        "BR",
                        null
                )
        );

        MoisWorkspaceDtos.CollectionJobListResponse jobs = service.listCollectionJobs("workspace-001", "COMPLETED");

        assertThat(created.jobId()).startsWith("mois-collect-");
        assertThat(jobs.items()).hasSize(1);
        assertThat(jobs.items().getFirst().workspaceId()).isEqualTo("workspace-001");
        assertThat(created.status()).isEqualTo("COMPLETED");
    }

    @Test
    void shouldReturnCollectedReferencesForExistingJob() {
        MoisWorkspaceDtos.CollectionJobResponse created = service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-001",
                        "nutricao",
                        "perda de gordura",
                        List.of("CLICKBANK"),
                        "LAST_30_DAYS",
                        20,
                        "pt-BR",
                        "BR",
                        65
                )
        );

        MoisWorkspaceDtos.CollectedReferenceListResponse references =
                service.listCollectedReferencesByJob(created.jobId(), null, null, null, null).orElseThrow();

        assertThat(references.items()).hasSize(1);
        assertThat(references.items().getFirst().source()).isEqualTo("CLICKBANK");
        assertThat(references.items().getFirst().successScore()).isGreaterThanOrEqualTo(65);
        assertThat(references.items().getFirst().confidenceLevel()).isIn("LOW", "MEDIUM", "HIGH");
    }

    @Test
    void shouldFilterCollectedReferencesByScoreAndConfidenceAndHandleMissingEvidence() {
        MoisWorkspaceDtos.CollectionJobResponse created = service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-002",
                        "financas",
                        "renda extra",
                        List.of("CLICKBANK", "JVZOO", "HOTMART"),
                        "LAST_7_DAYS",
                        10,
                        "pt-BR",
                        "BR",
                        55
                )
        );

        MoisWorkspaceDtos.CollectedReferenceListResponse filtered =
                service.listCollectedReferencesByJob(created.jobId(), null, "financas", 70, "MEDIUM").orElseThrow();

        assertThat(filtered.items()).isNotEmpty();
        assertThat(filtered.items()).allMatch(item -> item.successScore() >= 70);
        assertThat(filtered.items()).allMatch(item -> "MEDIUM".equals(item.confidenceLevel()));
        assertThat(filtered.items()).allMatch(item -> item.rankingPosition() >= 1);
        assertThat(filtered.items()).allMatch(item -> item.evidenceScore() > 0.0);
    }

    @Test
    void shouldFavoriteDiscardAndImportCollectedReference() {
        MoisWorkspaceDtos.CollectionJobResponse created = service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-003",
                        "nutricao",
                        "hipertrofia",
                        List.of("CLICKBANK"),
                        "LAST_7_DAYS",
                        10,
                        "pt-BR",
                        "BR",
                        40
                )
        );

        MoisWorkspaceDtos.CollectedReferenceResponse reference = service
                .listCollectedReferencesByJob(created.jobId(), null, null, null, null)
                .orElseThrow()
                .items()
                .getFirst();

        var favorite = service.favoriteCollectedReference(created.jobId(), reference.referenceId()).orElseThrow();
        assertThat(favorite.action()).isEqualTo("FAVORITE");

        var imported = service.importCollectedReference(created.jobId(), reference.referenceId()).orElseThrow();
        assertThat(imported.status()).isEqualTo("IMPORTED");
        assertThat(imported.importedReferenceId()).isNotBlank();

        var discarded = service.discardCollectedReference(created.jobId(), reference.referenceId()).orElseThrow();
        assertThat(discarded.status()).isEqualTo("DISCARDED");
    }

    @Test
    void shouldImportAndStartExtractionWithLineageAndLibraryBlocks() {
        MoisWorkspaceDtos.CollectionJobResponse created = service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-004",
                        "fisioterapia",
                        "dor lombar",
                        List.of("CLICKBANK"),
                        "LAST_30_DAYS",
                        10,
                        "pt-BR",
                        "BR",
                        50
                )
        );

        MoisWorkspaceDtos.CollectedReferenceResponse reference = service
                .listCollectedReferencesByJob(created.jobId(), null, null, null, null)
                .orElseThrow()
                .items()
                .getFirst();

        MoisWorkspaceDtos.CollectedReferenceActionResponse action = service
                .importAndStartExtraction(created.jobId(), reference.referenceId())
                .orElseThrow();

        assertThat(action.action()).isEqualTo("IMPORT_AND_START_EXTRACTION");
        assertThat(action.importedReferenceId()).isNotBlank();
        assertThat(action.extractionId()).isNotBlank();
        assertThat(action.generatedLibraryBlockIds()).hasSize(2);

        MoisWorkspaceDtos.CollectedReferenceLineageResponse lineage = service
                .getCollectedReferenceLineage(created.jobId(), reference.referenceId())
                .orElseThrow();
        assertThat(lineage.importedReferenceId()).isEqualTo(action.importedReferenceId());
        assertThat(lineage.extractionId()).isEqualTo(action.extractionId());
        assertThat(lineage.generatedLibraryBlockIds()).hasSize(2);
    }

    @Test
    void shouldExposeCollectionOpsSummaryWithRetriesAndLatency() {
        service.createCollectionJob(
                new MoisWorkspaceDtos.CreateCollectionJobRequest(
                        "workspace-ops",
                        "nutricao",
                        "perda de gordura",
                        List.of("META_AD_LIBRARY", "CLICKBANK"),
                        "LAST_7_DAYS",
                        10,
                        "pt-BR",
                        "BR",
                        55
                )
        );

        MoisWorkspaceDtos.CollectionOpsSummaryResponse summary = service.getCollectionOpsSummary("workspace-ops");
        assertThat(summary.workspaceId()).isEqualTo("workspace-ops");
        assertThat(summary.totalJobs()).isEqualTo(1);
        assertThat(summary.completedJobs()).isEqualTo(1);
        assertThat(summary.totalRetries()).isGreaterThanOrEqualTo(1);
        assertThat(summary.averageJobLatencyMs()).isGreaterThan(0);
        assertThat(summary.sourceBreakdown()).isNotEmpty();
    }

    @Test
    void shouldBlockCollectionWhenRolloutDoesNotAllowWorkspace() {
        service.configureCollectionRollout(true, List.of("workspace-allowed"));

        var request = new MoisWorkspaceDtos.CreateCollectionJobRequest(
                "workspace-blocked",
                "nutricao",
                "tema",
                List.of("CLICKBANK"),
                "LAST_7_DAYS",
                10,
                "pt-BR",
                "BR",
                40
        );

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> service.createCollectionJob(request));
    }

    private static class HtmlHandler implements HttpHandler {
        private final String body;

        private HtmlHandler(String body) {
            this.body = body;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(payload);
            }
        }
    }
}
