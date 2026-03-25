package com.marketinghub.worker.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.experiment.dto.ExperimentCampaignMetricDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.CreativeSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.ExperimentSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.HypothesisSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.InstantFormSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.LandingPageSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.LeadPortalFlowSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto.MarketNicheSnapshot;
import com.marketinghub.experiment.report.dto.ExperimentReportRequestDetailDto;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExperimentReportRendererTest {

    private final ExperimentReportRenderer renderer = new ExperimentReportRenderer(new ExperimentReportProperties());

    @Test
    void shouldRenderHtmlWithKeySections() {
        ExperimentReportRequestDetailDto request = new ExperimentReportRequestDetailDto();
        request.setId(1L);
        request.setExperimentId(9L);
        request.setRequestedAt(Instant.parse("2026-03-25T10:21:05Z"));

        ExperimentReportMaterialDto material = ExperimentReportMaterialDto.builder()
                .experiment(ExperimentSnapshot.builder()
                        .id(9L)
                        .name("Programa de Performance")
                        .status("ATIVO")
                        .startDate(LocalDate.parse("2026-03-01"))
                        .endDate(LocalDate.parse("2026-03-31"))
                        .dailyBudget(new BigDecimal("150.00"))
                        .build())
                .niche(MarketNicheSnapshot.builder()
                        .name("Fitness corporativo")
                        .description("Treinamentos premium")
                        .interestList(List.of("Academia", "Qualidade de vida"))
                        .build())
                .hypothesis(HypothesisSnapshot.builder()
                        .title("Melhorar produtividade")
                        .promise("+25% de foco")
                        .build())
                .creatives(List.of(CreativeSnapshot.builder()
                        .headline("Eleve sua equipe")
                        .primaryText("Plano semanal de alta performance")
                        .cta("Quero conhecer")
                        .destinationUrl("https://marketinghub.test/exp/9")
                        .imageUrl("https://cdn.test/creative.png")
                        .angles(List.of("Produtividade", "Bem-estar"))
                        .visualProofs(List.of("Coach certificado"))
                        .build()))
                .landingPages(List.of(LandingPageSnapshot.builder()
                        .url("https://marketinghub.test/landing")
                        .type("Lead")
                        .status("online")
                        .build()))
                .leadPortalFlows(List.of(LeadPortalFlowSnapshot.builder()
                        .name("Lead corporativo")
                        .slug("lead-corp")
                        .model("DELIGHT")
                        .approved(true)
                        .previewImageUrl("https://cdn.test/flow.png")
                        .questions(List.of())
                        .build()))
                .instantForm(InstantFormSnapshot.builder()
                        .name("Form teste")
                        .status("ACTIVE")
                        .shareLink("https://facebook.com/form")
                        .build())
                .build();

        ExperimentCampaignMetricDto metric = new ExperimentCampaignMetricDto();
        metric.setImpressions(12000L);
        metric.setClicks(450L);
        metric.setLeads(90L);
        metric.setSpend(new BigDecimal("3200.50"));
        metric.setCpl(new BigDecimal("35.55"));
        material.setCampaignMetric(metric);

        ExperimentFunnelStageDto stageDto = new ExperimentFunnelStageDto();
        stageDto.setLabel("Visitas");
        stageDto.setTotalCount(1200);
        stageDto.setLastEventAt(Instant.parse("2026-03-24T12:00:00Z"));

        material.setFunnelStages(List.of(stageDto));

        ExperimentReportRenderer.RenderedExperimentReport rendered = renderer.render(request, material);
        String html = new String(rendered.content(), StandardCharsets.UTF_8);

        assertThat(rendered.filename()).isEqualTo("experiment-report-exp9-req1.html");
        assertThat(html)
                .contains("Relatório objetivo do experimento")
                .contains("Programa de Performance")
                .contains("Fitness corporativo")
                .contains("Form teste")
                .contains("Funil do experimento");

        List<String> visualSnippets = List.of(
                "Formul&aacute;rios do Lead Portal",
                "https://cdn.test/creative.png",
                "https://cdn.test/flow.png",
                "microlink.io/?url=https%3A%2F%2Fmarketinghub.test%2Flanding&amp;screenshot=true"
        );
        List<String> missingSnippets = visualSnippets.stream()
                .filter(fragment -> !html.contains(fragment))
                .toList();
        assertThat(missingSnippets).as("trechos não encontrados no HTML").isEmpty();
    }
}
