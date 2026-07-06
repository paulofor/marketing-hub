package com.marketinghub.facebookads.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
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
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Orquestra a estrategia de campanha e decide quando a campanha deixou de ser util.
 */
@Service
public class CampaignStrategyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignStrategyService.class);
    private static final BigDecimal DEFAULT_LOW_TICKET_SPEND_MULTIPLIER = new BigDecimal("3.00");
    private static final BigDecimal DEFAULT_MINIMUM_CHECKOUT_RATE = new BigDecimal("0.0300");
    private static final long DEFAULT_MINIMUM_LINK_CLICKS = 150L;
    private static final long DEFAULT_MINIMUM_IMPRESSIONS = 100L;

    private final CampaignStrategyRepository strategyRepository;
    private final CampaignStrategyEvaluationRepository evaluationRepository;
    private final ExperimentFunnelDiagnosticService diagnosticService;
    private final FacebookAdsCampaignRepository campaignRepository;

    /**
     * Cria o servico com repositorios e diagnostico de funil usados pela decisao.
     */
    public CampaignStrategyService(CampaignStrategyRepository strategyRepository,
                                   CampaignStrategyEvaluationRepository evaluationRepository,
                                   ExperimentFunnelDiagnosticService diagnosticService,
                                   FacebookAdsCampaignRepository campaignRepository) {
        this.strategyRepository = strategyRepository;
        this.evaluationRepository = evaluationRepository;
        this.diagnosticService = diagnosticService;
        this.campaignRepository = campaignRepository;
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
     * Avalia as metricas sincronizadas e solicita parada quando a estrategia perder utilidade.
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
        StrategyStopDecision stopDecision = decideStop(strategy, metric, funnel);
        if (stopDecision.shouldStop()) {
            requestCampaignStop(campaign, stopDecision.stopReason(), stopDecision.reason());
            return recordEvaluation(strategy, metric, CampaignStrategyDecision.STOP_REQUESTED,
                    stopDecision.reason(), funnel, stopDecision.stopReason());
        }
        return recordEvaluation(strategy, metric, CampaignStrategyDecision.KEEP_LEARNING,
                "Campanha ainda gera aprendizado ou nao atingiu os limites da estrategia.",
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
     * Cria a estrategia padrao de acordo com o tipo comercial do experimento.
     */
    private CampaignStrategy buildDefaultStrategy(FacebookAdsCampaign campaign) {
        Experiment experiment = campaign.getExperiment();
        CampaignStrategy strategy = new CampaignStrategy();
        strategy.setCampaign(campaign);
        if (experiment != null && experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT) {
            strategy.setObjective(CampaignStrategyObjective.FIRST_PURCHASE_LOW_TICKET);
            strategy.setPreset("LOW_TICKET_FIRST_PURCHASE");
            strategy.setMaxSpendWithoutPurchase(resolveLowTicketSpendLimit(experiment));
            strategy.setMinimumCheckoutRate(DEFAULT_MINIMUM_CHECKOUT_RATE);
            strategy.setMinimumLinkClicks(DEFAULT_MINIMUM_LINK_CLICKS);
            strategy.setMinimumImpressions(DEFAULT_MINIMUM_IMPRESSIONS);
            return strategy;
        }
        strategy.setObjective(CampaignStrategyObjective.LEAD_VALIDATION);
        strategy.setPreset("LEAD_VALIDATION");
        strategy.setMaxSpendWithoutPurchase(null);
        strategy.setMinimumCheckoutRate(null);
        strategy.setMinimumLinkClicks(0L);
        strategy.setMinimumImpressions(DEFAULT_MINIMUM_IMPRESSIONS);
        return strategy;
    }

    /**
     * Decide se a campanha deve parar por limite financeiro e ausencia de sinal de compra.
     */
    private StrategyStopDecision decideStop(CampaignStrategy strategy,
                                            ExperimentCampaignMetric metric,
                                            FunnelSnapshot funnel) {
        if (strategy.getObjective() != CampaignStrategyObjective.FIRST_PURCHASE_LOW_TICKET) {
            return StrategyStopDecision.keep();
        }
        BigDecimal spend = positiveOrZero(metric.getSpend());
        BigDecimal maxSpend = strategy.getMaxSpendWithoutPurchase();
        long clicks = metric.getClicks() != null ? metric.getClicks() : 0L;
        boolean reachedSpendLimit = maxSpend != null && spend.compareTo(maxSpend) >= 0;
        boolean reachedClickSample = clicks >= nullToZero(strategy.getMinimumLinkClicks());
        boolean hasPurchase = funnel != null && funnel.purchases() > 0;
        boolean hasWeakCheckout = funnel == null
                || funnel.checkoutRate() == null
                || funnel.checkoutRate().compareTo(nullToZero(strategy.getMinimumCheckoutRate())) < 0;
        if ((reachedSpendLimit || reachedClickSample) && !hasPurchase && hasWeakCheckout) {
            return new StrategyStopDecision(
                    true,
                    FacebookCampaignStopReason.CAMPAIGN_STRATEGY_STOPPED,
                    "Estrategia low-ticket: limite de gasto/amostra atingido sem compra e sem taxa minima de checkout."
            );
        }
        return StrategyStopDecision.keep();
    }

    /**
     * Solicita ao Facebook Ads Worker que pause a campanha na Meta.
     */
    private void requestCampaignStop(FacebookAdsCampaign campaign,
                                     FacebookCampaignStopReason stopReason,
                                     String reason) {
        Experiment experiment = campaign.getExperiment();
        if (experiment != null && experiment.getStatus() == ExperimentStatus.RUNNING) {
            experiment.setStatus(ExperimentStatus.INVALIDATED);
        }
        campaign.setStopReason(stopReason);
        campaign.setStopRequestedAt(java.time.Instant.now());
        campaign.setStopCompletedAt(null);
        campaign.setStopLastError(null);
        campaignRepository.save(campaign);
        LOGGER.warn("Estrategia solicitou parada da campanha {}: {}", campaign.getId(), reason);
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

    /**
     * Resolve o limite financeiro padrao para primeira venda low-ticket.
     */
    private BigDecimal resolveLowTicketSpendLimit(Experiment experiment) {
        BigDecimal unitPrice = experiment != null ? experiment.getUnitPrice() : null;
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return unitPrice.multiply(DEFAULT_LOW_TICKET_SPEND_MULTIPLIER).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Normaliza valores monetarios nulos para zero.
     */
    private BigDecimal positiveOrZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : BigDecimal.ZERO;
    }

    /**
     * Normaliza valores numericos nulos para zero.
     */
    private long nullToZero(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Normaliza percentuais nulos para zero.
     */
    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private record FunnelSnapshot(Long checkoutAttempts,
                                  Long checkoutClicks,
                                  Long purchases,
                                  BigDecimal checkoutRate) {
    }

    private record StrategyStopDecision(boolean shouldStop,
                                        FacebookCampaignStopReason stopReason,
                                        String reason) {
        /**
         * Representa decisao de manter a campanha em aprendizado.
         */
        private static StrategyStopDecision keep() {
            return new StrategyStopDecision(false, null, null);
        }
    }
}
