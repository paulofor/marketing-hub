package com.marketinghub.facebookads.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.ExperimentFunnelDiagnosticService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticReasonCode;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.facebookads.CampaignStrategy;
import com.marketinghub.facebookads.CampaignStrategyDecision;
import com.marketinghub.facebookads.CampaignStrategyEvaluation;
import com.marketinghub.facebookads.CampaignStrategyObjective;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.CampaignStrategyEvaluationRepository;
import com.marketinghub.repository.jpa.facebookads.CampaignStrategyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Testa as decisoes automaticas da estrategia de campanha. */
@ExtendWith(MockitoExtension.class)
class CampaignStrategyServiceTest {
    @Mock
    private CampaignStrategyRepository strategyRepository;
    @Mock
    private CampaignStrategyEvaluationRepository evaluationRepository;
    @Mock
    private ExperimentFunnelDiagnosticService diagnosticService;
    private CampaignStrategyService service;

    /** Cria o servico com dependencias controladas por mock. */
    @BeforeEach
    void setUp() {
        service = new CampaignStrategyService(
                strategyRepository,
                evaluationRepository,
                diagnosticService
        );
    }

    /** Confirma que a estratégia apenas audita métricas e não solicita parada paralela. */
    @Test
    void evaluateAfterMetricsKeepsCampaignDecisionAsAuditOnly() {
        Experiment experiment = new Experiment();
        experiment.setId(58L);
        experiment.setStatus(ExperimentStatus.RUNNING);

        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("campaign-58");
        campaign.setExperiment(experiment);

        ExperimentCampaignMetric metric = ExperimentCampaignMetric.builder()
                .campaign(campaign)
                .experiment(experiment)
                .spend(new BigDecimal("60.00"))
                .clicks(160L)
                .impressions(2400L)
                .build();

        CampaignStrategy strategy = new CampaignStrategy();
        strategy.setId(10L);
        strategy.setCampaign(campaign);
        strategy.setObjective(CampaignStrategyObjective.LEAD_VALIDATION);
        strategy.setPreset("UNIQUE_CAMPAIGN_POLICY_AUDIT");
        strategy.setMaxSpendWithoutPurchase(null);
        strategy.setMinimumCheckoutRate(null);
        strategy.setMinimumLinkClicks(0L);
        strategy.setMinimumImpressions(100L);
        strategy.setEnabled(true);

        when(strategyRepository.findByCampaign(campaign)).thenReturn(Optional.of(strategy));
        when(diagnosticService.diagnose(58L)).thenReturn(new ExperimentFunnelDiagnosticsResponseDto(
                List.of(
                        stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 160L, 1L, 0.00625),
                        stage(ExperimentFunnelStage.COMPRA, 1L, 0L, 0.0)
                ),
                null
        ));
        when(evaluationRepository.save(any(CampaignStrategyEvaluation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CampaignStrategyEvaluation evaluation = service.evaluateAfterMetrics(metric);

        assertThat(evaluation.getDecision()).isEqualTo(CampaignStrategyDecision.KEEP_LEARNING);
        assertThat(evaluation.getStopReason()).isNull();
        assertThat(campaign.getStopReason()).isNull();
        assertThat(campaign.getStopRequestedAt()).isNull();
        assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    }

    /** Cria diagnostico sintetico de etapa para avaliacao de estrategia. */
    private ExperimentFunnelStageDiagnosticDto stage(ExperimentFunnelStage stage,
                                                    long attempts,
                                                    long successes,
                                                    double rate) {
        return new ExperimentFunnelStageDiagnosticDto(
                stage,
                stage.getLabel(),
                attempts,
                successes,
                rate,
                0.03,
                null,
                List.of(),
                FunnelDiagnosticStatus.WEAK_SIGNAL,
                FunnelDiagnosticReasonCode.BELOW_MIN_RATE,
                "",
                false
        );
    }
}
