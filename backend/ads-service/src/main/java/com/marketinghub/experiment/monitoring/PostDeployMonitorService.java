package com.marketinghub.experiment.monitoring;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.monitoring.dto.PostDeployFacebookLogSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployMetaAdsSummaryDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorDecision;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorResponseDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeSummaryDto;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsClient;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.facebookads.playbook.dto.ExperimentFacebookApiLogDto;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Consolida Meta Ads, analytics PDE e logs para decisão pós-deploy do experimento. */
@Service
@Slf4j
public class PostDeployMonitorService {

    private static final String DEFAULT_PDE_PRODUCT_SLUG = "metodo-musa-7-dias";
    private static final BigDecimal ZERO_INTERACTION_SPEND_THRESHOLD = BigDecimal.valueOf(25);

    private final ExperimentRepository experimentRepository;
    private final ExperimentCampaignMetricRepository campaignMetricRepository;
    private final ExperimentFacebookApiLogService apiLogService;
    private final PdeAnalyticsClient pdeAnalyticsClient;

    /** Inicializa o agregador com as fontes persistidas do Hub e o cliente do PDE. */
    public PostDeployMonitorService(
            ExperimentRepository experimentRepository,
            ExperimentCampaignMetricRepository campaignMetricRepository,
            ExperimentFacebookApiLogService apiLogService,
            PdeAnalyticsClient pdeAnalyticsClient) {
        this.experimentRepository = experimentRepository;
        this.campaignMetricRepository = campaignMetricRepository;
        this.apiLogService = apiLogService;
        this.pdeAnalyticsClient = pdeAnalyticsClient;
    }

    /** Monta o painel pós-deploy para o experimento e produto PDE informados. */
    public PostDeployMonitorResponseDto summarize(Long experimentId, String productSlug) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new EntityNotFoundException("Experimento %d não encontrado".formatted(experimentId)));
        String resolvedProductSlug = resolveProductSlug(productSlug);
        ExperimentCampaignMetric metric = campaignMetricRepository.findByExperiment(experiment).orElse(null);
        PostDeployMetaAdsSummaryDto metaAds = toMetaAdsSummary(metric);
        PostDeployPdeSummaryDto pde = fetchPdeSummary(resolvedProductSlug);
        PostDeployFacebookLogSummaryDto logs = summarizeLogs(experimentId);
        List<String> alerts = buildAlerts(metaAds, pde, logs);
        PostDeployMonitorDecision decision = decide(metaAds, pde, logs);
        return new PostDeployMonitorResponseDto(
                experimentId,
                resolvedProductSlug,
                Instant.now(),
                decision,
                decisionLabel(decision),
                recommendation(decision, pde),
                metaAds,
                pde,
                logs,
                alerts);
    }

    /** Resolve o produto PDE padrão quando a tela não informa um slug específico. */
    private String resolveProductSlug(String productSlug) {
        return StringUtils.hasText(productSlug) ? productSlug.trim() : DEFAULT_PDE_PRODUCT_SLUG;
    }

    /** Converte a métrica persistida da campanha em resumo do painel. */
    private PostDeployMetaAdsSummaryDto toMetaAdsSummary(ExperimentCampaignMetric metric) {
        if (metric == null) {
            return new PostDeployMetaAdsSummaryDto(null, null, null, null, null, null, null, null, null, null, null, null);
        }
        return new PostDeployMetaAdsSummaryDto(
                metric.getDateStart(),
                metric.getDateStop(),
                metric.getReach(),
                metric.getImpressions(),
                metric.getClicks(),
                metric.getLeads(),
                metric.getSpend(),
                metric.getCpc(),
                metric.getCpl(),
                calculateCtrPercent(metric),
                metric.getCampaign() != null ? metric.getCampaign().getMetricsLastSyncedAt() : null,
                metric.getCampaign() != null ? metric.getCampaign().getMetricsLastError() : null);
    }

    /** Calcula CTR percentual com arredondamento estável para exibição administrativa. */
    private BigDecimal calculateCtrPercent(ExperimentCampaignMetric metric) {
        if (metric.getClicks() == null || metric.getImpressions() == null || metric.getImpressions() == 0) {
            return null;
        }
        return BigDecimal.valueOf(metric.getClicks())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(metric.getImpressions()), 2, RoundingMode.HALF_UP);
    }

    /** Consulta o PDE e retorna um resumo indisponível quando houver falha técnica. */
    private PostDeployPdeSummaryDto fetchPdeSummary(String productSlug) {
        try {
            PdeAnalyticsSummary summary = pdeAnalyticsClient.fetchSummary(productSlug);
            return toPdeSummary(summary);
        } catch (Exception ex) {
            log.error("Falha ao consultar analytics PDE no monitor pós-deploy; productSlug={}", productSlug, ex);
            return new PostDeployPdeSummaryDto(
                    false,
                    "UNAVAILABLE",
                    ex.getMessage(),
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    Map.<String, Long>of());
        }
    }

    /** Converte o contrato do PDE em métricas comerciais específicas do painel. */
    private PostDeployPdeSummaryDto toPdeSummary(PdeAnalyticsSummary summary) {
        Map<String, Long> events = new LinkedHashMap<>();
        if (summary.events() != null) {
            summary.events().forEach(event -> events.put(event.eventType(), event.total()));
        }
        return new PostDeployPdeSummaryDto(
                true,
                "AVAILABLE",
                null,
                summary.totalEvents(),
                summary.uniqueVisitors(),
                summary.sessions(),
                summary.pedEntries(),
                summary.pageViews(),
                eventTotal(events, "PRESENCE_MAP_CHOICE_SELECTED"),
                eventTotal(events, "DIAGNOSTIC_CHOICE_SELECTED"),
                eventTotal(events, "FIELD_FILLED"),
                summary.loginStarted(),
                summary.loginCompleted(),
                summary.paywallViewed(),
                summary.subscriptionClicked(),
                summary.checkoutStarted(),
                summary.subscriptionApproved(),
                summary.totalVisibleMs(),
                null,
                events);
    }

    /** Busca a contagem do evento considerando aliases de caixa alta e baixa. */
    private long eventTotal(Map<String, Long> events, String eventType) {
        Long direct = events.get(eventType);
        if (direct != null) {
            return direct;
        }
        String normalized = eventType.toLowerCase(Locale.ROOT);
        return events.entrySet().stream()
                .filter(entry -> normalized.equals(entry.getKey().toLowerCase(Locale.ROOT)))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    /** Resume os logs recentes da API Meta vinculados ao experimento. */
    private PostDeployFacebookLogSummaryDto summarizeLogs(Long experimentId) {
        List<ExperimentFacebookApiLogDto> logs = apiLogService.findLogs(experimentId, 50);
        int errorLogs = (int) logs.stream().filter(this::isErrorLog).count();
        List<String> recentErrors = logs.stream()
                .filter(this::isErrorLog)
                .map(this::formatLogError)
                .limit(5)
                .toList();
        Instant lastLogAt = logs.stream()
                .map(this::logInstant)
                .filter(java.util.Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null);
        return new PostDeployFacebookLogSummaryDto(logs.size(), errorLogs, lastLogAt, recentErrors);
    }

    /** Identifica falhas de integração por status HTTP ou mensagem de erro. */
    private boolean isErrorLog(ExperimentFacebookApiLogDto log) {
        return (log.statusCode() != null && log.statusCode() >= 400)
                || StringUtils.hasText(log.errorMessage());
    }

    /** Formata um erro recente sem expor payloads sensíveis. */
    private String formatLogError(ExperimentFacebookApiLogDto log) {
        String endpoint = StringUtils.hasText(log.endpoint()) ? log.endpoint() : "endpoint não informado";
        String status = log.statusCode() != null ? "HTTP " + log.statusCode() : "sem status";
        String message = StringUtils.hasText(log.errorMessage()) ? " - " + log.errorMessage() : "";
        return endpoint + " (" + status + ")" + message;
    }

    /** Resolve o horário operacional mais útil do log. */
    private Instant logInstant(ExperimentFacebookApiLogDto log) {
        if (log.requestedAt() != null) {
            return log.requestedAt();
        }
        if (log.respondedAt() != null) {
            return log.respondedAt();
        }
        return log.createdAt();
    }

    /** Monta alertas objetivos para separar falha técnica de leitura comercial. */
    private List<String> buildAlerts(PostDeployMetaAdsSummaryDto metaAds,
                                     PostDeployPdeSummaryDto pde,
                                     PostDeployFacebookLogSummaryDto logs) {
        List<String> alerts = new ArrayList<>();
        if (!pde.available()) {
            alerts.add("Analytics PDE indisponível; validar saúde do pde-platform antes de concluir resultado comercial.");
        }
        if (StringUtils.hasText(metaAds.lastSyncError())) {
            alerts.add("Sincronização de métricas Meta Ads possui erro recente.");
        }
        if (logs.errorLogs() > 0) {
            alerts.add("Há erros recentes nos logs da API Meta Ads.");
        }
        if (spendAtLeast(metaAds, ZERO_INTERACTION_SPEND_THRESHOLD) && pde.available() && firstInteractionCount(pde) == 0) {
            alerts.add("Gasto atingiu o limite de atenção sem clique no Mapa/Diagnóstico.");
        }
        return alerts;
    }

    /** Decide a ação operacional recomendada com base em gasto, interação e saúde técnica. */
    private PostDeployMonitorDecision decide(PostDeployMetaAdsSummaryDto metaAds,
                                             PostDeployPdeSummaryDto pde,
                                             PostDeployFacebookLogSummaryDto logs) {
        if (!pde.available() || StringUtils.hasText(metaAds.lastSyncError()) || logs.errorLogs() > 0) {
            return PostDeployMonitorDecision.TECHNICAL_ATTENTION;
        }
        if (pde.subscriptionApproved() > 0) {
            return PostDeployMonitorDecision.SCALE_GRADUALLY;
        }
        if (spendAtLeast(metaAds, ZERO_INTERACTION_SPEND_THRESHOLD) && firstInteractionCount(pde) == 0) {
            return PostDeployMonitorDecision.PAUSE_AND_FIX;
        }
        if ((metaAds.spend() == null || BigDecimal.ZERO.compareTo(metaAds.spend()) == 0) && pde.sessions() == 0) {
            return PostDeployMonitorDecision.WAITING_DATA;
        }
        return PostDeployMonitorDecision.KEEP_MONITORING;
    }

    /** Verifica se o gasto rastreado atingiu o limite informado. */
    private boolean spendAtLeast(PostDeployMetaAdsSummaryDto metaAds, BigDecimal threshold) {
        return metaAds.spend() != null && metaAds.spend().compareTo(threshold) >= 0;
    }

    /** Conta a primeira interação comercial relevante dentro do PDE. */
    private long firstInteractionCount(PostDeployPdeSummaryDto pde) {
        return pde.presenceMapClicks() + pde.diagnosticClicks();
    }

    /** Retorna o rótulo em português da decisão sugerida. */
    private String decisionLabel(PostDeployMonitorDecision decision) {
        return switch (decision) {
            case WAITING_DATA -> "Aguardando dados";
            case KEEP_MONITORING -> "Manter monitoramento";
            case PAUSE_AND_FIX -> "Pausar e corrigir";
            case SCALE_GRADUALLY -> "Escalar gradualmente";
            case TECHNICAL_ATTENTION -> "Corrigir medição";
        };
    }

    /** Retorna a recomendação executiva para a próxima decisão comercial. */
    private String recommendation(PostDeployMonitorDecision decision, PostDeployPdeSummaryDto pde) {
        return switch (decision) {
            case WAITING_DATA -> "Ainda não há volume suficiente para leitura. Aguarde tráfego real pós-deploy.";
            case KEEP_MONITORING -> buildKeepMonitoringRecommendation(pde);
            case PAUSE_AND_FIX -> "Pausar a campanha e ajustar a primeira dobra/CTA do PDE antes de gastar mais mídia.";
            case SCALE_GRADUALLY -> "Compra aprovada detectada. Manter a versão e escalar orçamento em passos pequenos.";
            case TECHNICAL_ATTENTION -> "Corrigir integração, logs ou sincronização antes de tomar decisão comercial.";
        };
    }

    /** Detalha a recomendação quando ainda existe sinal intermediário no funil. */
    private String buildKeepMonitoringRecommendation(PostDeployPdeSummaryDto pde) {
        if (firstInteractionCount(pde) > 0 && pde.fieldFilled() == 0) {
            return "Há clique inicial no PDE, mas o e-mail ainda não avançou. Monitorar e preparar ajuste na captura.";
        }
        if (pde.paywallViewed() > 0 && pde.subscriptionApproved() == 0) {
            return "O funil chegou ao paywall, mas ainda não comprou. Monitorar oferta, preço e objeções finais.";
        }
        return "Continuar coletando dados até atingir volume mínimo ou sinal de checkout/compra.";
    }
}
