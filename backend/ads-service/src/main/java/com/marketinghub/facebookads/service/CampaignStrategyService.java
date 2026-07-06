package com.marketinghub.facebookads.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.funnel.ExperimentFunnelDiagnosticService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.facebookads.CampaignStrategy;
import com.marketinghub.facebookads.CampaignStrategyDecision;
import com.marketinghub.facebookads.CampaignStrategyEvaluation;
import com.marketinghub.facebookads.CampaignStrategyObjective;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.facebookads.CampaignStrategyEvaluationRepository;
import com.marketinghub.repository.jpa.facebookads.CampaignStrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Registra a estratégia auditável da campanha sem decidir parada operacional paralela.
 */
@Service
public class CampaignStrategyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignStrategyService.class);
    private static final long DEFAULT_MINIMUM_IMPRESSIONS = 100L;

    private final CampaignStrategyRepository strategyRepository;
    private final CampaignStrategyEvaluationRepository evaluationRepository;
    private final ExperimentFunnelDiagnosticService diagnosticService;

    /**
     * Cria o serviço com repositórios e diagnóstico de funil usados pela auditoria.
     */
    public CampaignStrategyService(CampaignStrategyRepository strategyRepository,
                                   CampaignStrategyEvaluationRepository evaluationRepository,
                                   ExperimentFunnelDiagnosticService diagnosticService) {
        this.strategyRepository = strategyRepository;
        this.evaluationRepository = evaluationRepository;
        this.diagnosticService = diagnosticService;
    }

    /**
     * Garante que uma campanha publicada tenha uma estrategia padrao auditavel.
     */
    @Transactional
    public CampaignStrategy ensureDefaultStrategy(FacebookAdsCampaign campaign) {
        return strategyRepository.findByCampaign(campaign)
                .orElseGet(() -> strategyRepository.save(buildDefaultStrategy(campaign)));
    }

    /**
     * Avalia as métricas sincronizadas apenas para auditoria da estratégia.
     */
    @Transactional
    public CampaignStrategyEvaluation evaluateAfterMetrics(ExperimentCampaignMetric metric) {
        if (metric == null || metric.getCampaign() == null) {
            throw new IllegalArgumentException("Metricas de campanha sao obrigatorias para avaliar estrategia");
        }
        FacebookAdsCampaign campaign = metric.getCampaign();
        CampaignStrategy strategy = ensureDefaultStrategy(campaign);
        if (!strategy.isEnabled()) {
            return recordEvaluation(strategy, metric, CampaignStrategyDecision.KEEP_LEARNING,
                    "Estrategia desabilitada; mantendo campanha sem decisao automatica.", null, null);
        }
        FunnelSnapshot funnel = loadFunnelSnapshot(campaign);
        if (campaign.getStopRequestedAt() != null) {
            return recordEvaluation(strategy, metric, CampaignStrategyDecision.STOP_REQUESTED,
                    "Campanha ja possui solicitacao de parada pendente para o Facebook Ads Worker.",
                    funnel,
                    campaign.getStopReason());
        }
        return recordEvaluation(strategy, metric, CampaignStrategyDecision.KEEP_LEARNING,
                "Estratégia registrada apenas para auditoria; parada é decidida pela política única de campanha.",
                funnel,
                null);
    }

    /**
     * Busca a estrategia mais recente de um experimento para exibicao na tela.
     */
    @Transactional(readOnly = true)
    public CampaignStrategy findLatestByExperimentId(Long experimentId) {
        if (experimentId == null) {
            return null;
        }
        return strategyRepository.findTopByCampaign_Experiment_IdOrderByCreatedAtDesc(experimentId).orElse(null);
    }

    /**
     * Cria a estratégia padrão única para auditoria de campanha.
     */
    private CampaignStrategy buildDefaultStrategy(FacebookAdsCampaign campaign) {
        CampaignStrategy strategy = new CampaignStrategy();
        strategy.setCampaign(campaign);
        strategy.setObjective(CampaignStrategyObjective.LEAD_VALIDATION);
        strategy.setPreset("UNIQUE_CAMPAIGN_POLICY_AUDIT");
        strategy.setMaxSpendWithoutPurchase(null);
        strategy.setMinimumCheckoutRate(null);
        strategy.setMinimumLinkClicks(0L);
        strategy.setMinimumImpressions(DEFAULT_MINIMUM_IMPRESSIONS);
        return strategy;
    }

    /**
     * Carrega o retrato de funil necessario para interpretar checkout e compra.
     */
    private FunnelSnapshot loadFunnelSnapshot(FacebookAdsCampaign campaign) {
        Experiment experiment = campaign.getExperiment();
        if (experiment == null || experiment.getId() == null) {
            return null;
        }
        try {
            ExperimentFunnelDiagnosticsResponseDto diagnostics = diagnosticService.diagnose(experiment.getId());
            ExperimentFunnelStageDiagnosticDto checkout = findStage(diagnostics, ExperimentFunnelStage.ACESSO_CHECKOUT);
            ExperimentFunnelStageDiagnosticDto purchase = findStage(diagnostics, ExperimentFunnelStage.COMPRA);
            return new FunnelSnapshot(
                    checkout != null ? checkout.attempts() : 0L,
                    checkout != null ? checkout.successes() : 0L,
                    purchase != null ? purchase.successes() : 0L,
                    checkout != null && checkout.observedRate() != null
                            ? BigDecimal.valueOf(checkout.observedRate()).setScale(4, RoundingMode.HALF_UP)
                            : null
            );
        } catch (RuntimeException ex) {
            LOGGER.warn(
                    "Falha ao carregar diagnostico de funil para estrategia de campanha: campaignId={}, experimentId={}",
                    campaign.getId(),
                    experiment.getId(),
                    ex
            );
            return null;
        }
    }

    /**
     * Registra a avaliacao feita pela estrategia de campanha.
     */
    private CampaignStrategyEvaluation recordEvaluation(CampaignStrategy strategy,
                                                        ExperimentCampaignMetric metric,
                                                        CampaignStrategyDecision decision,
                                                        String reason,
                                                        FunnelSnapshot funnel,
                                                        FacebookCampaignStopReason stopReason) {
        CampaignStrategyEvaluation evaluation = new CampaignStrategyEvaluation();
        evaluation.setStrategy(strategy);
        evaluation.setCampaign(metric.getCampaign());
        evaluation.setDecision(decision);
        evaluation.setReason(reason);
        evaluation.setSpend(metric.getSpend());
        evaluation.setImpressions(metric.getImpressions());
        evaluation.setClicks(metric.getClicks());
        evaluation.setCheckoutAttempts(funnel != null ? funnel.checkoutAttempts() : null);
        evaluation.setCheckoutClicks(funnel != null ? funnel.checkoutClicks() : null);
        evaluation.setPurchases(funnel != null ? funnel.purchases() : null);
        evaluation.setCheckoutRate(funnel != null ? funnel.checkoutRate() : null);
        evaluation.setStopReason(stopReason);
        return evaluationRepository.save(evaluation);
    }

    /**
     * Localiza uma etapa no diagnostico consolidado do funil.
     */
    private ExperimentFunnelStageDiagnosticDto findStage(ExperimentFunnelDiagnosticsResponseDto diagnostics,
                                                        ExperimentFunnelStage stage) {
        if (diagnostics == null || diagnostics.diagnostics() == null) {
            return null;
        }
        return diagnostics.diagnostics().stream()
                .filter(item -> item.stageKey() == stage)
                .findFirst()
                .orElse(null);
    }

    private record FunnelSnapshot(Long checkoutAttempts,
                                  Long checkoutClicks,
                                  Long purchases,
                                  BigDecimal checkoutRate) {
    }
}
