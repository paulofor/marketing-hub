package com.marketinghub.experiment.funnel;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelDiagnosticsResponseDto;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDiagnosticDto;
import com.marketinghub.experiment.funnel.dto.FunnelDiagnosticStatus;
import com.marketinghub.experiment.funnel.dto.FunnelThresholdCheckDto;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Serviço responsável por invalidar experimentos automaticamente quando há evidência operacional ruim.
 */
@Service
public class ExperimentFunnelAutoStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExperimentFunnelAutoStopService.class);
    private static final double THREE_PERCENT = 0.03d;
    private static final double AD_INTEREST_MINIMUM_RATE = 0.015d;
    private static final BigDecimal LOW_AD_INTEREST_STOP_MINIMUM_SPEND = new BigDecimal("25.00");
    private static final BigDecimal LOW_FORM_ENTRY_NO_SUBMISSION_MINIMUM_SPEND = new BigDecimal("20.00");
    private static final long LOW_FORM_ENTRY_NO_SUBMISSION_MINIMUM_IMPRESSIONS = 1500L;
    private static final double LOW_FORM_ENTRY_NO_SUBMISSION_MAX_ACCESS_RATE = 0.012d;
    private static final BigDecimal LOW_TICKET_TICKET_MULTIPLIER_STOP = new BigDecimal("3.00");
    private static final long LOW_TICKET_MINIMUM_SESSIONS = 100L;
    private static final long LOW_TICKET_MINIMUM_LINK_CLICKS = 150L;
    private static final double LOW_TICKET_STRONG_CHECKOUT_INTENT_RATE = 0.03d;
    private static final Duration LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE = Duration.ofHours(48);
    private static final long LOW_IMPRESSIONS_MINIMUM = 100L;

    private final ExperimentFunnelDiagnosticService diagnosticService;
    private final ExperimentFunnelStandbyService standbyService;
    private final ExperimentCampaignMetricRepository campaignMetricRepository;

    /**
     * Cria o serviço com diagnóstico de funil, campanhas e métricas para registrar pausas automáticas.
     */
    public ExperimentFunnelAutoStopService(ExperimentFunnelDiagnosticService diagnosticService,
                                           ExperimentFunnelStandbyService standbyService,
                                           ExperimentCampaignMetricRepository campaignMetricRepository) {
        this.diagnosticService = diagnosticService;
        this.standbyService = standbyService;
        this.campaignMetricRepository = campaignMetricRepository;
    }

    /**
     * Avalia a taxa de interesse inicial do anúncio e invalida o experimento quando a conversão para o formulário falha
     * estatisticamente depois de atingir o gasto mínimo de mídia.
     *
     * @return {@code true} quando o experimento foi parado automaticamente, {@code false} caso contrário.
     */
    public boolean stopIfAdInterestStatisticallyLow(Experiment experiment) {
        if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
            return false;
        }
        ExperimentFunnelDiagnosticsResponseDto diagnostics = diagnosticService.diagnose(experiment.getId());
        ExperimentFunnelStageDiagnosticDto leadAccessStage = diagnostics.diagnostics().stream()
                .filter(dto -> dto.stageKey() == ExperimentFunnelStage.ACESSO_FORM_LEAD)
                .findFirst()
                .orElse(null);
        if (leadAccessStage == null || leadAccessStage.minAcceptableRate() == null) {
            return false;
        }
        boolean lowInterestFailed = leadAccessStage.status() == FunnelDiagnosticStatus.STATISTICALLY_FAILED
                && Math.abs(leadAccessStage.minAcceptableRate() - AD_INTEREST_MINIMUM_RATE) < 1e-9;
        if (!lowInterestFailed) {
            return false;
        }
        BigDecimal campaignSpend = resolveCampaignSpend(experiment);
        if (campaignSpend.compareTo(LOW_AD_INTEREST_STOP_MINIMUM_SPEND) < 0) {
            LOGGER.info(
                    "Low ad interest confirmed for experiment {}, but automatic stop is waiting for minimum spend: currentSpend={}, minimumSpend={}",
                    experiment.getId(),
                    campaignSpend,
                    LOW_AD_INTEREST_STOP_MINIMUM_SPEND
            );
            return false;
        }
        LOGGER.warn(
                "Automatic stop triggered for experiment {} due to statistically low ad interest after minimum spend: attempts={}, successes={}, observedRate={}, minimumRate={}, upper95={}, spend={}, minimumSpend={}",
                experiment.getId(),
                leadAccessStage.attempts(),
                leadAccessStage.successes(),
                leadAccessStage.observedRate(),
                leadAccessStage.minAcceptableRate(),
                leadAccessStage.upper95RateIfZero(),
                campaignSpend,
                LOW_AD_INTEREST_STOP_MINIMUM_SPEND
        );
        invalidateExperimentAndRequestStops(
                experiment,
                FacebookCampaignStopReason.TARGET_AUDIENCE_LOW_INTEREST_STATISTICAL,
                "taxa de acesso ao formulário abaixo de 1,5% com confiança estatística de 95% após atingir R$ 25,00 de mídia"
        );
        return true;
    }

    /**
     * Avalia sinal composto de baixa entrada no formulário sem envio e invalida campanhas que já gastaram o piso
     * operacional sem produzir lead.
     *
     * @return {@code true} quando o experimento foi parado automaticamente, {@code false} caso contrário.
     */
    public boolean stopIfLowFormEntryAndNoSubmissionAfterSpend(Experiment experiment) {
        if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
            return false;
        }
        BigDecimal campaignSpend = resolveCampaignSpend(experiment);
        if (campaignSpend.compareTo(LOW_FORM_ENTRY_NO_SUBMISSION_MINIMUM_SPEND) < 0) {
            return false;
        }
        ExperimentFunnelDiagnosticsResponseDto diagnostics = diagnosticService.diagnose(experiment.getId());
        ExperimentFunnelStageDiagnosticDto leadAccessStage = findStageDiagnostic(
                diagnostics,
                ExperimentFunnelStage.ACESSO_FORM_LEAD
        );
        ExperimentFunnelStageDiagnosticDto submissionStage = findStageDiagnostic(
                diagnostics,
                ExperimentFunnelStage.ENVIO_FORM
        );
        if (leadAccessStage == null || submissionStage == null || leadAccessStage.attempts() <= 0) {
            return false;
        }
        double leadAccessRate = (double) leadAccessStage.successes() / leadAccessStage.attempts();
        boolean hasEnoughDistribution = leadAccessStage.attempts() >= LOW_FORM_ENTRY_NO_SUBMISSION_MINIMUM_IMPRESSIONS;
        boolean hasLowFormEntry = leadAccessRate <= LOW_FORM_ENTRY_NO_SUBMISSION_MAX_ACCESS_RATE;
        boolean hasNoSubmission = submissionStage.successes() == 0;
        if (!hasEnoughDistribution || !hasLowFormEntry || !hasNoSubmission) {
            return false;
        }
        LOGGER.warn(
                "Automatic stop triggered for experiment {} due to low form entry and no submissions after spend: impressions={}, formAccesses={}, formAccessRate={}, formViews={}, submissions={}, spend={}, minimumSpend={}",
                experiment.getId(),
                leadAccessStage.attempts(),
                leadAccessStage.successes(),
                leadAccessRate,
                submissionStage.attempts(),
                submissionStage.successes(),
                campaignSpend,
                LOW_FORM_ENTRY_NO_SUBMISSION_MINIMUM_SPEND
        );
        invalidateExperimentAndRequestStops(
                experiment,
                FacebookCampaignStopReason.LOW_FORM_ENTRY_NO_SUBMISSION_AFTER_SPEND,
                "baixa entrada no formulário após 1.500 impressões e R$ 20,00 de mídia, sem nenhum envio de formulário"
        );
        return true;
    }

    /**
     * Recupera o gasto de mídia sincronizado para decidir se a parada por baixo interesse já pode ser executada.
     */
    private BigDecimal resolveCampaignSpend(Experiment experiment) {
        if (experiment == null) {
            return BigDecimal.ZERO;
        }
        return campaignMetricRepository.findByExperiment(experiment)
                .map(ExperimentCampaignMetric::getSpend)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Avalia a etapa de envio de formulário e invalida o experimento quando a regra dos 3% falha.
     *
     * @return {@code true} quando o experimento foi parado automaticamente, {@code false} caso contrário.
     */
    public boolean stopIfFormSubmissionZeroConversions(Experiment experiment) {
        if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING) {
            return false;
        }
        ExperimentFunnelDiagnosticsResponseDto diagnostics = diagnosticService.diagnose(experiment.getId());
        ExperimentFunnelStageDiagnosticDto submissionStage = diagnostics.diagnostics().stream()
                .filter(dto -> dto.stageKey() == ExperimentFunnelStage.ENVIO_FORM)
                .findFirst()
                .orElse(null);
        if (submissionStage == null || submissionStage.thresholdChecks() == null) {
            return false;
        }
        boolean threePercentFailed = submissionStage.thresholdChecks().stream()
                .filter(Objects::nonNull)
                .anyMatch(this::isThreePercentFailure);
        if (!threePercentFailed) {
            return false;
        }
        LOGGER.warn(
                "Automatic stop triggered for experiment {} due to zero conversions after reaching the 3%% rule-of-three threshold.",
                experiment.getId()
        );
        invalidateExperimentAndRequestStops(
                experiment,
                FacebookCampaignStopReason.FORM_ZERO_CONVERSION_RULE_OF_THREE,
                "100 acessos sem envio de formulário pela regra estatística dos 3%"
        );
        return true;
    }

    /**
     * Avalia experimento low-ticket e invalida quando zero compras já têm amostra e custo incompatíveis com o ticket.
     *
     * @return {@code true} quando o experimento foi parado automaticamente, {@code false} caso contrário.
     */
    public boolean stopIfLowTicketZeroPurchasesAfterStatisticalFinancialLimit(Experiment experiment) {
        if (experiment == null
                || experiment.getStatus() != ExperimentStatus.RUNNING
                || experiment.getExperimentType() != ExperimentType.LOW_TICKET_PRODUCT) {
            return false;
        }
        ExperimentFunnelDiagnosticsResponseDto diagnostics = diagnosticService.diagnose(experiment.getId());
        ExperimentFunnelStageDiagnosticDto checkoutIntentStage = findStageDiagnostic(
                diagnostics,
                ExperimentFunnelStage.ACESSO_CHECKOUT
        );
        ExperimentFunnelStageDiagnosticDto purchaseStage = findStageDiagnostic(
                diagnostics,
                ExperimentFunnelStage.COMPRA
        );
        if (checkoutIntentStage == null || purchaseStage == null || purchaseStage.successes() > 0) {
            return false;
        }
        ExperimentCampaignMetric metric = campaignMetricRepository.findByExperiment(experiment).orElse(null);
        long sessions = checkoutIntentStage.attempts();
        long linkClicks = metric != null && metric.getClicks() != null ? metric.getClicks() : 0L;
        boolean sampleReached = sessions >= LOW_TICKET_MINIMUM_SESSIONS || linkClicks >= LOW_TICKET_MINIMUM_LINK_CLICKS;
        if (!sampleReached) {
            return false;
        }
        BigDecimal totalCost = resolveTotalExperimentCost(experiment, metric);
        BigDecimal ticketStopLimit = resolveTicketStopLimit(experiment);
        BigDecimal stopLossLimit = positiveOrNull(experiment.getStopLossCpl());
        boolean reachedTicketLimit = ticketStopLimit != null && totalCost.compareTo(ticketStopLimit) >= 0;
        boolean reachedStopLossWithoutStrongIntent = stopLossLimit != null
                && totalCost.compareTo(stopLossLimit) >= 0
                && !hasStrongCheckoutIntent(checkoutIntentStage);
        if (!reachedTicketLimit && !reachedStopLossWithoutStrongIntent) {
            return false;
        }
        LOGGER.warn(
                "Automatic stop triggered for low-ticket experiment {} due to zero purchases after statistical-financial limit: sessions={}, linkClicks={}, checkouts={}, purchases={}, checkoutRate={}, totalCost={}, ticketStopLimit={}, stopLossLimit={}",
                experiment.getId(),
                sessions,
                linkClicks,
                checkoutIntentStage.successes(),
                purchaseStage.successes(),
                checkoutIntentStage.observedRate(),
                totalCost,
                ticketStopLimit,
                stopLossLimit
        );
        invalidateExperimentAndRequestStops(
                experiment,
                FacebookCampaignStopReason.LOW_TICKET_ZERO_PURCHASE_STATISTICAL_FINANCIAL,
                "produto low-ticket com zero compras após amostra mínima e custo total acima de 3x o ticket ou stop-loss sem intenção forte de checkout"
        );
        return true;
    }

    /**
     * Avalia se a campanha rodou tempo suficiente com impressões muito baixas e invalida o experimento.
     */
    public boolean stopIfLowImpressionsAfterRunningTime(Experiment experiment, Long impressions, Instant campaignCreatedAt) {
        if (experiment == null || experiment.getStatus() != ExperimentStatus.RUNNING || campaignCreatedAt == null) {
            return false;
        }
        Instant minimumAgeInstant = Instant.now().minus(LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE);
        if (campaignCreatedAt.isAfter(minimumAgeInstant)) {
            return false;
        }
        long normalizedImpressions = impressions == null ? 0L : impressions;
        if (normalizedImpressions >= LOW_IMPRESSIONS_MINIMUM) {
            return false;
        }
        LOGGER.warn(
                "Automatic stop triggered for experiment {} due to low impressions after {} hours: impressions={}, minimum={}",
                experiment.getId(),
                LOW_IMPRESSIONS_MIN_CAMPAIGN_AGE.toHours(),
                normalizedImpressions,
                LOW_IMPRESSIONS_MINIMUM
        );
        invalidateExperimentAndRequestStops(
                experiment,
                FacebookCampaignStopReason.LOW_IMPRESSIONS_AFTER_RUNNING_TIME,
                "menos de 100 impressões após 48 horas de campanha"
        );
        return true;
    }

    /**
     * Coloca o experimento em standby no primeiro envio válido usando o serviço sem dependência circular.
     *
     * @return {@code true} quando o standby foi aplicado, {@code false} caso o experimento não esteja elegível.
     */
    public boolean standbyOnFirstValidFormSubmission(Experiment experiment) {
        return standbyService.standbyOnFirstValidFormSubmission(experiment);
    }

    /**
     * Localiza o diagnóstico de uma etapa específica dentro da resposta consolidada.
     */
    private ExperimentFunnelStageDiagnosticDto findStageDiagnostic(ExperimentFunnelDiagnosticsResponseDto diagnostics,
                                                                   ExperimentFunnelStage stage) {
        if (diagnostics == null || diagnostics.diagnostics() == null || stage == null) {
            return null;
        }
        return diagnostics.diagnostics().stream()
                .filter(dto -> dto.stageKey() == stage)
                .findFirst()
                .orElse(null);
    }

    /**
     * Confirma se a checagem estatística representa falha na regra dos 3%.
     */
    private boolean isThreePercentFailure(FunnelThresholdCheckDto check) {
        if (check == null || check.minAcceptableRate() == null) {
            return false;
        }
        return Math.abs(check.minAcceptableRate() - THREE_PERCENT) < 1e-9 && check.statisticallyFailed();
    }

    /**
     * Resolve o custo total do experimento priorizando o acumulado do experimento e usando mídia como fallback.
     */
    private BigDecimal resolveTotalExperimentCost(Experiment experiment, ExperimentCampaignMetric metric) {
        BigDecimal totalCost = positiveOrNull(experiment.getTotalCost());
        if (totalCost != null) {
            return totalCost;
        }
        BigDecimal mediaSpend = metric != null ? positiveOrNull(metric.getSpend()) : null;
        return mediaSpend != null ? mediaSpend : BigDecimal.ZERO;
    }

    /**
     * Calcula o limite financeiro de 3x o ticket do produto low-ticket quando o preço está configurado.
     */
    private BigDecimal resolveTicketStopLimit(Experiment experiment) {
        BigDecimal unitPrice = positiveOrNull(experiment.getUnitPrice());
        return unitPrice != null ? unitPrice.multiply(LOW_TICKET_TICKET_MULTIPLIER_STOP) : null;
    }

    /**
     * Confirma se a taxa de checkout é forte o bastante para aguardar mais dados antes de parar por stop-loss.
     */
    private boolean hasStrongCheckoutIntent(ExperimentFunnelStageDiagnosticDto checkoutIntentStage) {
        return checkoutIntentStage != null
                && checkoutIntentStage.observedRate() != null
                && checkoutIntentStage.observedRate() >= LOW_TICKET_STRONG_CHECKOUT_INTENT_RATE;
    }

    /**
     * Normaliza valores monetários positivos e descarta nulos, zero e negativos.
     */
    private BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
    }

    /**
     * Atualiza o status do experimento e registra o motivo de parada nas campanhas vinculadas.
     */
    private void invalidateExperimentAndRequestStops(Experiment experiment,
                                                     FacebookCampaignStopReason stopReason,
                                                     String businessReason) {
        experiment.setStatus(ExperimentStatus.INVALIDATED);
        standbyService.requestFacebookCampaignStops(experiment.getId(), stopReason, businessReason);
    }

}
