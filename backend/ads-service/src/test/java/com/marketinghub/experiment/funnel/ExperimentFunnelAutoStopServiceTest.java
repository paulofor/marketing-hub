package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticReasonCode;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.experiment.funnel.dto.FunnelThresholdCheckDto;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Testa as regras de parada automática do funil de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentFunnelAutoStopServiceTest {

    @Mock
    private ExperimentFunnelDiagnosticService diagnosticService;
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;
    @Mock
    private ExperimentCampaignMetricRepository campaignMetricRepository;

    private ExperimentFunnelAutoStopService service;

    private Experiment experiment;

    @BeforeEach
    void setUp() {
        service = new ExperimentFunnelAutoStopService(
                diagnosticService,
                new ExperimentFunnelStandbyService(campaignRepository),
                campaignMetricRepository
        );
        experiment = new Experiment();
        experiment.setId(99L);
        experiment.setStatus(ExperimentStatus.RUNNING);
    }

    /**
     * Garante parada quando o interesse no anúncio falha com confiança estatística após R$ 25,00 de mídia.
     */
    @Test
    void stopsExperimentWhenAdInterestStatisticallyFailsAfterMinimumSpend() {
        ExperimentFunnelStageDiagnosticDto stage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_FORM_LEAD,
                "Acesso ao formulário",
                1199,
                6,
                0.005,
                0.015,
                0.0108,
                List.of(new FunnelThresholdCheckDto(0.015, 200, 0.0025, false, true)),
                FunnelDiagnosticStatus.STATISTICALLY_FAILED,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(stage), null));
        when(campaignMetricRepository.findByExperiment(experiment))
                .thenReturn(Optional.of(metricWithSpend("25.00")));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-low-interest");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfAdInterestStatisticallyLow(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason())
                .isEqualTo(FacebookCampaignStopReason.TARGET_AUDIENCE_LOW_INTEREST_STATISTICAL);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    /**
     * Garante que a reprovação estatística aguarde o piso de R$ 25,00 antes de desativar a campanha.
     */
    @Test
    void keepsExperimentRunningWhenAdInterestFailsBeforeMinimumSpend() {
        ExperimentFunnelStageDiagnosticDto stage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_FORM_LEAD,
                "Acesso ao formulário",
                1476,
                8,
                0.0054,
                0.015,
                0.011,
                List.of(new FunnelThresholdCheckDto(0.015, 200, 0.002, false, true)),
                FunnelDiagnosticStatus.STATISTICALLY_FAILED,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(stage), null));
        when(campaignMetricRepository.findByExperiment(experiment))
                .thenReturn(Optional.of(metricWithSpend("24.99")));

        boolean stopped = service.stopIfAdInterestStatisticallyLow(experiment);

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }


    /**
     * Garante parada quando a campanha tem distribuição suficiente, pouca entrada no formulário e nenhum envio após R$ 20,00.
     */
    @Test
    void stopsExperimentWhenFormEntryIsLowAndNoSubmissionAfterMinimumSpend() {
        ExperimentFunnelStageDiagnosticDto leadAccessStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_FORM_LEAD,
                "Acesso ao formulário",
                1521,
                15,
                0.0099,
                0.015,
                0.016,
                List.of(new FunnelThresholdCheckDto(0.015, 200, 0.002, false, true)),
                FunnelDiagnosticStatus.WEAK_SIGNAL,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
        ExperimentFunnelStageDiagnosticDto submissionStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ENVIO_FORM,
                "Envio",
                8,
                0,
                0.0,
                0.10,
                0.375,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.375, false, false)),
                FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                "",
                false
        );
        when(campaignMetricRepository.findByExperiment(experiment))
                .thenReturn(Optional.of(metricWithSpend("20.80")));
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(leadAccessStage, submissionStage), null));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-low-form-entry");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfLowFormEntryAndNoSubmissionAfterSpend(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason())
                .isEqualTo(FacebookCampaignStopReason.LOW_FORM_ENTRY_NO_SUBMISSION_AFTER_SPEND);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    /**
     * Garante que a nova regra não invalida antes de atingir o piso financeiro.
     */
    @Test
    void keepsExperimentRunningWhenLowFormEntryHasNotReachedMinimumSpend() {
        when(campaignMetricRepository.findByExperiment(experiment))
                .thenReturn(Optional.of(metricWithSpend("19.99")));

        boolean stopped = service.stopIfLowFormEntryAndNoSubmissionAfterSpend(experiment);

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    /**
     * Garante standby e pausa da campanha no primeiro envio válido do formulário.
     */
    @Test
    void putsRunningExperimentInStandbyAfterFirstValidFormSubmission() {
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-first-submission");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.standbyOnFirstValidFormSubmission(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.STANDBY);
        assertThat(campaign.getStopReason()).isEqualTo(FacebookCampaignStopReason.FIRST_FORM_SUBMISSION_STANDBY);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    /**
     * Garante que submissões duplicadas ou tardias não alterem experimento já pausado ou finalizado.
     */
    @Test
    void doesNotStandbyExperimentWhenStatusIsNotRunning() {
        experiment.setStatus(ExperimentStatus.STANDBY);

        boolean stopped = service.standbyOnFirstValidFormSubmission(experiment);

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.STANDBY);
    }

    @Test
    void stopsExperimentWhenThreePercentThresholdFails() {
        ExperimentFunnelStageDiagnosticDto stage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ENVIO_FORM,
                "Envio",
                120,
                0,
                0.0,
                0.10,
                0.025,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.025, true, true)),
                FunnelDiagnosticStatus.STATISTICALLY_FAILED,
                FunnelDiagnosticReasonCode.RULE_OF_THREE_FAILED,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(stage), null));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-1");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfFormSubmissionZeroConversions(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason()).isEqualTo(FacebookCampaignStopReason.FORM_ZERO_CONVERSION_RULE_OF_THREE);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    /**
     * Garante invalidação de produto low-ticket quando zero compras já têm amostra e custo acima de 3x o ticket.
     */
    @Test
    void stopsLowTicketExperimentWhenZeroPurchasesReachTicketFinancialLimit() {
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setUnitPrice(new BigDecimal("27.00"));
        experiment.setTotalCost(new BigDecimal("129.04"));
        ExperimentFunnelStageDiagnosticDto checkoutStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_CHECKOUT,
                "Acesso checkout",
                212,
                1,
                0.0047,
                0.03,
                null,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.0142, false, true)),
                FunnelDiagnosticStatus.WEAK_SIGNAL,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
        ExperimentFunnelStageDiagnosticDto purchaseStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.COMPRA,
                "Compra",
                1,
                0,
                0.0,
                0.03,
                3.0,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 3.0, false, false)),
                FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(checkoutStage, purchaseStage), null));
        when(campaignMetricRepository.findByExperiment(experiment))
                .thenReturn(Optional.of(metricWithSpendAndClicks("47.99", 262L)));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-low-ticket-zero-sale");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfLowTicketZeroPurchasesAfterStatisticalFinancialLimit(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason())
                .isEqualTo(FacebookCampaignStopReason.LOW_TICKET_ZERO_PURCHASE_STATISTICAL_FINANCIAL);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    /**
     * Garante que produto low-ticket com amostra, mas custo abaixo do limite financeiro, continue em execução.
     */
    @Test
    void keepsLowTicketExperimentRunningBeforeFinancialLimit() {
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setUnitPrice(new BigDecimal("37.00"));
        experiment.setTotalCost(new BigDecimal("66.88"));
        ExperimentFunnelStageDiagnosticDto checkoutStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_CHECKOUT,
                "Acesso checkout",
                145,
                1,
                0.0069,
                0.03,
                null,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.0207, false, true)),
                FunnelDiagnosticStatus.WEAK_SIGNAL,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
        ExperimentFunnelStageDiagnosticDto purchaseStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.COMPRA,
                "Compra",
                1,
                0,
                0.0,
                0.03,
                3.0,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 3.0, false, false)),
                FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(checkoutStage, purchaseStage), null));
        when(campaignMetricRepository.findByExperiment(experiment))
                .thenReturn(Optional.of(metricWithSpendAndClicks("33.45", 182L)));

        boolean stopped = service.stopIfLowTicketZeroPurchasesAfterStatisticalFinancialLimit(experiment);

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    /**
     * Garante que uma compra registrada impede a invalidação automática por zero vendas.
     */
    @Test
    void keepsLowTicketExperimentRunningWhenPurchaseExists() {
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setUnitPrice(new BigDecimal("27.00"));
        experiment.setTotalCost(new BigDecimal("129.04"));
        ExperimentFunnelStageDiagnosticDto checkoutStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_CHECKOUT,
                "Acesso checkout",
                212,
                5,
                0.0236,
                0.03,
                null,
                List.of(),
                FunnelDiagnosticStatus.WEAK_SIGNAL,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
        ExperimentFunnelStageDiagnosticDto purchaseStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.COMPRA,
                "Compra",
                5,
                1,
                0.20,
                0.03,
                null,
                List.of(),
                FunnelDiagnosticStatus.HEALTHY_OR_INCONCLUSIVE,
                FunnelDiagnosticReasonCode.HEALTHY_OR_INCONCLUSIVE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(checkoutStage, purchaseStage), null));

        boolean stopped = service.stopIfLowTicketZeroPurchasesAfterStatisticalFinancialLimit(experiment);

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    /**
     * Garante parada quando o tráfego low-ticket já fica economicamente inviável antes da amostra completa.
     */
    @Test
    void stopsLowTicketExperimentWhenTrafficCostIsEconomicallyUnviable() {
        experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
        experiment.setUnitPrice(new BigDecimal("27.00"));
        experiment.setStopLossCpl(new BigDecimal("54.00"));
        experiment.setTotalCost(new BigDecimal("58.02"));
        ExperimentFunnelStageDiagnosticDto checkoutStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ACESSO_CHECKOUT,
                "Acesso checkout",
                22,
                0,
                0.0,
                0.03,
                null,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.1364, false, false)),
                FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                "",
                false
        );
        ExperimentFunnelStageDiagnosticDto purchaseStage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.COMPRA,
                "Compra",
                0,
                0,
                0.0,
                0.03,
                null,
                List.of(),
                FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(checkoutStage, purchaseStage), null));
        ExperimentCampaignMetric metric = metricWithSpendAndClicks("29.06", 8L);
        metric.setCpc(new BigDecimal("3.63"));
        when(campaignMetricRepository.findByExperiment(experiment)).thenReturn(Optional.of(metric));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-low-ticket-expensive-traffic");
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfLowTicketTrafficCostEconomicallyUnviable(experiment);

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason())
                .isEqualTo(FacebookCampaignStopReason.LOW_TICKET_TRAFFIC_COST_ECONOMICALLY_UNVIABLE);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    @Test
    void doesNothingWhenThresholdNotReached() {
        ExperimentFunnelStageDiagnosticDto stage = new ExperimentFunnelStageDiagnosticDto(
                ExperimentFunnelStage.ENVIO_FORM,
                "Envio",
                20,
                0,
                0.0,
                0.10,
                0.15,
                List.of(new FunnelThresholdCheckDto(0.03, 100, 0.15, false, false)),
                FunnelDiagnosticStatus.INSUFFICIENT_DATA,
                FunnelDiagnosticReasonCode.LOW_SAMPLE_SIZE,
                "",
                false
        );
        when(diagnosticService.diagnose(99L))
                .thenReturn(new ExperimentFunnelDiagnosticsResponseDto(List.of(stage), null));

        boolean stopped = service.stopIfFormSubmissionZeroConversions(experiment);

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    @Test
    void stopsExperimentWhenCampaignHasLowImpressionsAfterTwoDays() {
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-low-impressions");
        campaign.setCreatedAt(Instant.now().minus(49, ChronoUnit.HOURS));
        when(campaignRepository.findByExperimentId(99L)).thenReturn(List.of(campaign));

        boolean stopped = service.stopIfLowImpressionsAfterRunningTime(experiment, 42L, campaign.getCreatedAt());

        assertThat(stopped).isTrue();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INVALIDATED);
        assertThat(campaign.getStopReason()).isEqualTo(FacebookCampaignStopReason.LOW_IMPRESSIONS_AFTER_RUNNING_TIME);
        assertThat(campaign.getStopRequestedAt()).isNotNull();
        assertThat(campaign.getStopLastError()).isNull();
    }

    @Test
    void keepsExperimentRunningWhenLowImpressionsCampaignIsStillRecent() {
        boolean stopped = service.stopIfLowImpressionsAfterRunningTime(
                experiment,
                42L,
                Instant.now().minus(2, ChronoUnit.HOURS));

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    @Test
    void keepsExperimentRunningWhenImpressionsReachMinimumAfterTwoDays() {
        boolean stopped = service.stopIfLowImpressionsAfterRunningTime(
                experiment,
                100L,
                Instant.now().minus(49, ChronoUnit.HOURS));

        assertThat(stopped).isFalse();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    /**
     * Cria uma métrica de campanha com gasto de mídia para os cenários de parada automática.
     */
    private ExperimentCampaignMetric metricWithSpend(String spend) {
        ExperimentCampaignMetric metric = new ExperimentCampaignMetric();
        metric.setExperiment(experiment);
        metric.setSpend(new BigDecimal(spend));
        return metric;
    }

    /**
     * Cria uma métrica de campanha com gasto e cliques para os cenários de produto low-ticket.
     */
    private ExperimentCampaignMetric metricWithSpendAndClicks(String spend, Long clicks) {
        ExperimentCampaignMetric metric = metricWithSpend(spend);
        metric.setClicks(clicks);
        return metric;
    }

}
