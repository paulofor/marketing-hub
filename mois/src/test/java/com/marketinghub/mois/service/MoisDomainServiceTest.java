package com.marketinghub.mois.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoisDomainServiceTest {

    private MoisDomainService service;

    @BeforeEach
    void setUp() {
        service = new MoisDomainService();
    }

    @Test
    void shouldBuildConsolidatedInsightReportWithPatternsAndGaps() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Fisioterapia",
                        "Dor lombar",
                        "Alívio de dor",
                        List.of("dor lombar oferta"),
                        List.of("https://example.com/oferta-a", "https://example.com/oferta-b"),
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
        assertThat(report.saturationNotes()).isNotEmpty();
        assertThat(report.recommendedNextActions()).isNotEmpty();
    }

    @Test
    void shouldFilterInsightReportsByCategory() {
        MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse accepted = service.createDiscoveryRequest(
                new MoisDiscoveryDtos.CreateDiscoveryRequest(
                        "Nutrição",
                        "Perda de peso",
                        "Resultado rápido",
                        List.of(),
                        List.of("https://example.com/oferta-c"),
                        List.of("SEARCH"),
                        "BR",
                        "pt-BR",
                        null));

        service.runDiscoveryRequest(accepted.requestId());

        MoisInsightDtos.InsightReportListResponse matching = service.listInsightReports(null, null, "DIGITAL_PRODUCT");
        MoisInsightDtos.InsightReportListResponse noMatch = service.listInsightReports(null, null, "MENTORIA");

        assertThat(matching.items()).hasSize(1);
        assertThat(noMatch.items()).isEmpty();
    }
}
