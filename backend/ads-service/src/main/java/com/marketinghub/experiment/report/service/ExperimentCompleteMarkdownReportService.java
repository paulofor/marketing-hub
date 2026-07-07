package com.marketinghub.experiment.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSectionDto;
import com.marketinghub.experiment.funnel.service.analytics.ExperimentLandingAnalyticsSessionDto;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsável por montar o relatório completo em Markdown para experimentos encerrados.
 */
@Service
public class ExperimentCompleteMarkdownReportService {

    private static final Set<ExperimentStatus> ALLOWED_STATUSES = Set.of(
            ExperimentStatus.VALIDATED,
            ExperimentStatus.INVALIDATED
    );

    private final ExperimentRepository experimentRepository;
    private final GeraLandingStageExecutionRepository geraLandingStageExecutionRepository;
    private final FacebookAdsCampaignRepository facebookAdsCampaignRepository;
    private final ExperimentReportMaterialService materialService;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com os repositórios e consolidadores necessários para o relatório. */
    public ExperimentCompleteMarkdownReportService(ExperimentRepository experimentRepository,
                                                   GeraLandingStageExecutionRepository geraLandingStageExecutionRepository,
                                                   FacebookAdsCampaignRepository facebookAdsCampaignRepository,
                                                   ExperimentReportMaterialService materialService,
                                                   ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.geraLandingStageExecutionRepository = geraLandingStageExecutionRepository;
        this.facebookAdsCampaignRepository = facebookAdsCampaignRepository;
        this.materialService = materialService;
        this.objectMapper = objectMapper;
    }

    /**
     * Gera o relatório completo em Markdown para um experimento validado ou invalidado.
     */
    @Transactional(readOnly = true)
    public String buildMarkdown(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experimento não encontrado: " + experimentId));
        validateReportAvailability(experiment);

        var material = materialService.build(experimentId);
        List<GeraLandingStageExecution> geraLandingExecutions = geraLandingStageExecutionRepository
                .findByExperimentIdOrderByExecutionRequestedAtAsc(experimentId);
        List<FacebookAdsCampaign> facebookCampaigns = facebookAdsCampaignRepository
                .findDetailedByExperimentId(experimentId);

        StringBuilder markdown = new StringBuilder();
        appendHeader(markdown, experiment);
        appendStrategicContext(markdown, experiment);
        appendHypothesisFramework(markdown, experiment.getHypothesisRef());
        appendRawExperimentArtifacts(markdown, experiment);
        appendCampaignMetrics(markdown, experiment.getCampaignMetric());
        appendGeraLandingExecutions(markdown, geraLandingExecutions);
        appendFacebookCampaigns(markdown, facebookCampaigns);
        appendLandingAnalytics(markdown, material.getLandingAnalytics());
        appendMaterialSnapshot(markdown, material);
        return markdown.toString();
    }

    /**
     * Monta um nome de arquivo seguro para download do relatório em Markdown.
     */
    public String buildFilename(Long experimentId) {
        return "experimento-" + experimentId + "-relatorio-completo.md";
    }

    /**
     * Bloqueia a geração quando o experimento ainda não foi concluído pelo pipeline.
     */
    private void validateReportAvailability(Experiment experiment) {
        if (!ALLOWED_STATUSES.contains(experiment.getStatus())) {
            throw new IllegalStateException(
                    "O relatório completo só fica disponível para experimentos validados ou invalidados."
            );
        }
    }

    /**
     * Adiciona o cabeçalho executivo do relatório.
     */
    private void appendHeader(StringBuilder markdown, Experiment experiment) {
        markdown.append("# Relatório completo do experimento #")
                .append(experiment.getId())
                .append(" — ")
                .append(safe(experiment.getName()))
                .append("\n\n");
        markdown.append("- Gerado em: ").append(Instant.now()).append("\n");
        markdown.append("- Status final: ").append(value(experiment.getStatus())).append("\n");
        markdown.append("- Plataforma: ").append(value(experiment.getPlatform())).append("\n");
        markdown.append("- Etapa: ").append(value(experiment.getStage())).append("\n\n");
    }

    /**
     * Adiciona nicho, hipótese e parâmetros principais do experimento.
     */
    private void appendStrategicContext(StringBuilder markdown, Experiment experiment) {
        MarketNiche niche = experiment.getNiche();
        Hypothesis hypothesis = experiment.getHypothesisRef();
        markdown.append("## 1. Contexto estratégico\n\n");
        markdown.append("### Nicho\n\n");
        markdown.append("- ID: ").append(niche != null ? niche.getId() : "—").append("\n");
        markdown.append("- Nome: ").append(niche != null ? safe(niche.getName()) : "—").append("\n");
        markdown.append("- Descrição: ").append(niche != null ? safe(niche.getDescription()) : "—").append("\n");
        markdown.append("- Interesses: ").append(niche != null ? value(niche.getInterestList()) : "—").append("\n");
        markdown.append("- Funções: ").append(niche != null ? value(niche.getRoleList()) : "—").append("\n");
        markdown.append("- Comportamentos: ").append(niche != null ? value(niche.getBehaviorList()) : "—").append("\n\n");

        markdown.append("### Hipótese\n\n");
        markdown.append("- ID: ").append(hypothesis != null ? hypothesis.getId() : "—").append("\n");
        markdown.append("- Título: ").append(hypothesis != null ? safe(hypothesis.getTitle()) : "—").append("\n");
        markdown.append("- Dor/problema: ").append(hypothesis != null ? safe(hypothesis.getProblem()) : "—").append("\n");
        markdown.append("- Promessa: ").append(hypothesis != null ? safe(hypothesis.getPromise()) : "—").append("\n");
        markdown.append("- Persona: ").append(hypothesis != null ? safe(hypothesis.getPersona()) : "—").append("\n");
        markdown.append("- Mecanismo: ").append(hypothesis != null ? safe(hypothesis.getMechanism()) : "—").append("\n");
        markdown.append("- Mecanismo único: ").append(hypothesis != null ? safe(hypothesis.getUniqueMechanism()) : "—").append("\n");
        markdown.append("- Entrega/oferta: ").append(hypothesis != null ? safe(hypothesis.getEntrega()) : "—").append("\n\n");

        markdown.append("### Parâmetros do experimento\n\n");
        appendJsonBlock(markdown, "experiment_parameters", mapOf(
                "primaryVariable", experiment.getPrimaryVariable(),
                "primaryMetric", experiment.getPrimaryMetric(),
                "startDate", experiment.getStartDate(),
                "endDate", experiment.getEndDate(),
                "dailyBudget", experiment.getDailyBudget(),
                "kpiTargetCpl", experiment.getKpiTargetCpl(),
                "stopLossCpl", experiment.getStopLossCpl(),
                "sampleSize", experiment.getSampleSize(),
                "baselineCvr", experiment.getBaselineCvr(),
                "targetCvr", experiment.getTargetCvr(),
                "mdePercent", experiment.getMdePercent(),
                "unitPrice", experiment.getUnitPrice(),
                "auditableTotalCost", auditableTotalCost(experiment),
                "legacyTotalCost", experiment.getTotalCost(),
                "unreconciledLegacyCost", unreconciledLegacyCost(experiment),
                "expense", experiment.getExpense()
        ));
    }

    /**
     * Adiciona o framework canônico da hipótese em formato JSON bruto.
     */
    private void appendHypothesisFramework(StringBuilder markdown, Hypothesis hypothesis) {
        markdown.append("## 2. Framework da hipótese\n\n");
        appendCodeBlock(markdown, "json", prettyJson(hypothesis != null ? hypothesis.getFrameworkJson() : null));
    }

    /**
     * Calcula o custo rastreável em BRL a partir de origem, mídia e despesa operacional.
     */
    private BigDecimal auditableTotalCost(Experiment experiment) {
        if (experiment == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal paidMedia = experiment.getCampaignMetric() != null
                ? money(experiment.getCampaignMetric().getSpend())
                : BigDecimal.ZERO;
        return money(experiment.getCost())
                .add(money(experiment.getExpense()))
                .add(paidMedia)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula a diferença positiva entre o total legado e as fontes rastreáveis.
     */
    private BigDecimal unreconciledLegacyCost(Experiment experiment) {
        if (experiment == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal difference = money(experiment.getTotalCost()).subtract(auditableTotalCost(experiment));
        return difference.compareTo(BigDecimal.ZERO) > 0
                ? difference.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Normaliza valor monetário ausente para zero.
     */
    private BigDecimal money(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * Adiciona os JSONs e artefatos principais gerados antes do GeraLanding.
     */
    private void appendRawExperimentArtifacts(StringBuilder markdown, Experiment experiment) {
        markdown.append("## 3. Artefatos brutos do experimento\n\n");
        appendNamedRawBlock(markdown, "Ângulo de campanha", experiment.getCampaignAngle());
        appendNamedRawBlock(markdown, "Ad copy", experiment.getAdCopy());
        appendNamedRawBlock(markdown, "Prompt de imagem do anúncio", experiment.getCreativeImagePrompt());
        appendNamedRawBlock(markdown, "Briefing de imagem do anúncio", experiment.getAdImageBriefing());
        appendNamedRawBlock(markdown, "Prompt de texto criativo", experiment.getCreativeTextPrompt());
        appendNamedRawBlock(markdown, "Copy da landing", experiment.getLandingPageCopy());
        appendNamedRawBlock(markdown, "Wireframe da landing", experiment.getLandingPageWireframe());
        appendNamedRawBlock(markdown, "Planejamento de imagem da landing", experiment.getLandingPageImagePlanning());
        appendNamedRawBlock(markdown, "Assets de imagem da landing", experiment.getLandingPageImageAssets());
        appendNamedRawBlock(markdown, "Preset visual da landing", experiment.getLandingPageDesignPreset());
        appendNamedRawBlock(markdown, "HTML GeraLanding", experiment.getHtmlGeraLanding());
        appendNamedRawBlock(markdown, "Revisão de qualidade da landing", experiment.getLandingPageQualityReview());
        appendNamedRawBlock(markdown, "Entregáveis da landing", experiment.getLandingPageDeliverables());
        appendNamedRawBlock(markdown, "HTML final da landing", experiment.getLandingPageHtml());
    }

    /**
     * Adiciona as métricas agregadas sincronizadas para a campanha do experimento.
     */
    private void appendCampaignMetrics(StringBuilder markdown, ExperimentCampaignMetric metric) {
        markdown.append("## 4. Métricas agregadas da campanha\n\n");
        if (metric == null) {
            markdown.append("Nenhuma métrica agregada sincronizada para este experimento.\n\n");
            return;
        }
        appendJsonBlock(markdown, "campaign_metric", mapOf(
                "dateStart", metric.getDateStart(),
                "dateStop", metric.getDateStop(),
                "reach", metric.getReach(),
                "impressions", metric.getImpressions(),
                "clicks", metric.getClicks(),
                "leads", metric.getLeads(),
                "spend", metric.getSpend(),
                "cpc", metric.getCpc(),
                "cpl", metric.getCpl(),
                "metricsLastSyncedAt", metric.getCampaign() != null ? metric.getCampaign().getMetricsLastSyncedAt() : null,
                "metricsLastError", metric.getCampaign() != null ? metric.getCampaign().getMetricsLastError() : null
        ));
    }

    /**
     * Adiciona todas as execuções e payloads auditáveis do GeraLanding.
     */
    private void appendGeraLandingExecutions(StringBuilder markdown, List<GeraLandingStageExecution> executions) {
        markdown.append("## 5. GeraLanding — todas as execuções e JSONs\n\n");
        if (executions == null || executions.isEmpty()) {
            markdown.append("Nenhuma execução de GeraLanding encontrada para este experimento.\n\n");
            return;
        }
        for (GeraLandingStageExecution execution : executions) {
            markdown.append("### Etapa ")
                    .append(safe(execution.getStageCode()))
                    .append(" — job ")
                    .append(fromDatabaseIdJob(execution.getIdJob()))
                    .append("\n\n");
            appendJsonBlock(markdown, "geralanding_execution_metadata", mapOf(
                    "idJob", fromDatabaseIdJob(execution.getIdJob()),
                    "stageCode", execution.getStageCode(),
                    "status", execution.getStatus(),
                    "promptTemplateId", execution.getPromptTemplateId(),
                    "openAiModel", execution.getOpenAiModel(),
                    "openAiJobId", execution.getOpenAiJobId(),
                    "executionRequestedAt", execution.getExecutionRequestedAt(),
                    "processingStartedAt", execution.getProcessingStartedAt(),
                    "completedAt", execution.getCompletedAt(),
                    "inputTokens", execution.getInputTokens(),
                    "outputTokens", execution.getOutputTokens(),
                    "costUsd", execution.getCostUsd(),
                    "errorMessage", execution.getErrorMessage(),
                    "errorDetail", execution.getErrorDetail()
            ));
            appendNamedRawBlock(markdown, "Prompt content", execution.getPromptContent());
            appendNamedRawBlock(markdown, "Prompt final", execution.getPrompt());
            appendNamedRawBlock(markdown, "OpenAI request body", execution.getOpenAiRequestBody());
            appendNamedRawBlock(markdown, "Schema JSON", execution.getSchemaJson());
            appendNamedRawBlock(markdown, "Prompt markdown", execution.getPromptMarkdownContent());
            appendNamedRawBlock(markdown, "Resposta do modelo", execution.getModelResponse());
            appendNamedRawBlock(markdown, "HTML provisório", execution.getProvisionalHtml());
            appendNamedRawBlock(markdown, "Auditoria da revisão de qualidade", execution.getQualityReviewAudit());
        }
    }

    /**
     * Adiciona campanhas, conjuntos de anúncios, anúncios, criativos e métricas do Facebook Ads.
     */
    private void appendFacebookCampaigns(StringBuilder markdown, List<FacebookAdsCampaign> campaigns) {
        markdown.append("## 6. Facebook Ads — campanhas e métricas\n\n");
        if (campaigns == null || campaigns.isEmpty()) {
            markdown.append("Nenhuma campanha do Facebook Ads encontrada para este experimento.\n\n");
            return;
        }
        for (FacebookAdsCampaign campaign : campaigns) {
            markdown.append("### Campanha ").append(safe(campaign.getName())).append("\n\n");
            appendJsonBlock(markdown, "facebook_campaign", mapOf(
                    "id", campaign.getId(),
                    "externalId", campaign.getExternalId(),
                    "adAccountId", campaign.getAdAccountId(),
                    "name", campaign.getName(),
                    "objective", campaign.getObjective(),
                    "status", campaign.getStatus(),
                    "budgetMode", campaign.getBudgetMode(),
                    "dailyBudgetMinor", campaign.getDailyBudgetMinor(),
                    "lifetimeBudgetMinor", campaign.getLifetimeBudgetMinor(),
                    "apiVersion", campaign.getApiVersion(),
                    "specialAdCategories", campaign.getSpecialAdCategories(),
                    "specialAdCountries", campaign.getSpecialAdCountries(),
                    "metricsLastSyncedAt", campaign.getMetricsLastSyncedAt(),
                    "metricsLastError", campaign.getMetricsLastError(),
                    "stopReason", campaign.getStopReason(),
                    "stopRequestedAt", campaign.getStopRequestedAt(),
                    "stopCompletedAt", campaign.getStopCompletedAt(),
                    "stopLastError", campaign.getStopLastError(),
                    "createdAt", campaign.getCreatedAt(),
                    "updatedAt", campaign.getUpdatedAt()
            ));
            appendAdSets(markdown, campaign.getAdSets());
        }
    }

    /**
     * Adiciona o detalhamento dos conjuntos de anúncios de uma campanha.
     */
    private void appendAdSets(StringBuilder markdown, List<FacebookAdsAdSet> adSets) {
        if (adSets == null || adSets.isEmpty()) {
            markdown.append("Nenhum conjunto de anúncios persistido nesta campanha.\n\n");
            return;
        }
        for (FacebookAdsAdSet adSet : adSets) {
            markdown.append("#### Conjunto de anúncios ").append(safe(adSet.getName())).append("\n\n");
            appendJsonBlock(markdown, "facebook_ad_set", mapOf(
                    "id", adSet.getId(),
                    "externalId", adSet.getExternalId(),
                    "name", adSet.getName(),
                    "status", adSet.getStatus(),
                    "dailyBudgetMinor", adSet.getDailyBudgetMinor(),
                    "lifetimeBudgetMinor", adSet.getLifetimeBudgetMinor(),
                    "startTime", adSet.getStartTime(),
                    "endTime", adSet.getEndTime(),
                    "billingEvent", adSet.getBillingEvent(),
                    "optimizationGoal", adSet.getOptimizationGoal(),
                    "bidStrategy", adSet.getBidStrategy(),
                    "bidAmountMinor", adSet.getBidAmountMinor(),
                    "promotedObjectJson", parseMaybeJson(adSet.getPromotedObjectJson()),
                    "targetingJson", parseMaybeJson(adSet.getTargetingJson()),
                    "createdAt", adSet.getCreatedAt(),
                    "updatedAt", adSet.getUpdatedAt()
            ));
            appendAds(markdown, adSet.getAds());
        }
    }

    /**
     * Adiciona anúncios e seus criativos publicados no Facebook Ads.
     */
    private void appendAds(StringBuilder markdown, List<FacebookAdsAd> ads) {
        if (ads == null || ads.isEmpty()) {
            markdown.append("Nenhum anúncio persistido neste conjunto.\n\n");
            return;
        }
        for (FacebookAdsAd ad : ads) {
            FacebookAdsAdCreative creative = ad.getCreative();
            markdown.append("##### Anúncio ").append(safe(ad.getName())).append("\n\n");
            appendJsonBlock(markdown, "facebook_ad", mapOf(
                    "id", ad.getId(),
                    "externalId", ad.getExternalId(),
                    "name", ad.getName(),
                    "status", ad.getStatus(),
                    "creativeId", creative != null ? creative.getId() : null,
                    "trackingUtm", ad.getTrackingUtm() != null ? mapOf(
                            "utmSource", ad.getTrackingUtm().getUtmSource(),
                            "utmMedium", ad.getTrackingUtm().getUtmMedium(),
                            "utmCampaign", ad.getTrackingUtm().getUtmCampaign(),
                            "utmContent", ad.getTrackingUtm().getUtmContent(),
                            "utmTerm", ad.getTrackingUtm().getUtmTerm()
                    ) : null,
                    "createdAt", ad.getCreatedAt(),
                    "updatedAt", ad.getUpdatedAt()
            ));
            if (creative != null) {
                appendJsonBlock(markdown, "facebook_ad_creative", mapOf(
                        "id", creative.getId(),
                        "externalId", creative.getExternalId(),
                        "pageId", creative.getPageId(),
                        "instagramUserId", creative.getInstagramUserId(),
                        "kind", creative.getKind(),
                        "linkDataJson", parseMaybeJson(creative.getLinkDataJson()),
                        "videoDataJson", parseMaybeJson(creative.getVideoDataJson()),
                        "carouselDataJson", parseMaybeJson(creative.getCarouselDataJson()),
                        "lastPreviewUrl", creative.getLastPreviewUrl(),
                        "createdAt", creative.getCreatedAt(),
                        "updatedAt", creative.getUpdatedAt()
                ));
            }
        }
    }


    /**
     * Adiciona os dados de analytics da landing com tempos de página, sessões e trechos mais vistos.
     */
    private void appendLandingAnalytics(StringBuilder markdown, ExperimentLandingAnalyticsDto analytics) {
        markdown.append("## 7. Analytics da landing — tempos em página e trechos\n\n");
        if (analytics == null || analytics.totalEvents() == 0) {
            markdown.append("Nenhum evento de analytics da landing encontrado para este experimento.\n\n");
            return;
        }
        appendJsonBlock(markdown, "landing_analytics_summary", mapOf(
                "totalEvents", analytics.totalEvents(),
                "totalSessions", analytics.totalSessions(),
                "pageViews", analytics.pageViews(),
                "sectionViewEvents", analytics.sectionViewEvents(),
                "totalVisibleMs", analytics.totalVisibleMs(),
                "averageVisibleMsPerSession", analytics.averageVisibleMsPerSession(),
                "lastEventAt", analytics.lastEventAt(),
                "deviceBreakdown", analytics.deviceBreakdown(),
                "mobileOperatingSystemBreakdown", analytics.mobileOperatingSystemBreakdown(),
                "screenSizeBreakdown", analytics.screenSizeBreakdown(),
                "visitors", analytics.visitors()
        ));
        appendJsonBlock(markdown, "landing_analytics_top_sections", aggregateLandingAnalyticsSections(analytics.sessions()));
        appendJsonBlock(markdown, "landing_analytics_sessions", analytics.sessions());
    }

    /**
     * Consolida os trechos da landing por tempo visível acumulado em todas as sessões do relatório.
     */
    private List<Map<String, Object>> aggregateLandingAnalyticsSections(List<ExperimentLandingAnalyticsSessionDto> sessions) {
        Map<String, LandingAnalyticsSectionAccumulator> sections = new LinkedHashMap<>();
        if (sessions == null) {
            return List.of();
        }
        for (ExperimentLandingAnalyticsSessionDto session : sessions) {
            if (session.topSections() == null) {
                continue;
            }
            for (ExperimentLandingAnalyticsSectionDto section : session.topSections()) {
                sections.computeIfAbsent(section.sectionId(), LandingAnalyticsSectionAccumulator::new)
                        .record(section.visibleMs(), section.events());
            }
        }
        return sections.values().stream()
                .sorted((left, right) -> Long.compare(right.visibleMs(), left.visibleMs()))
                .map(section -> mapOf(
                        "sectionId", section.sectionId(),
                        "visibleMs", section.visibleMs(),
                        "events", section.events()
                ))
                .toList();
    }

    /**
     * Adiciona um snapshot JSON consolidado usado para auditoria completa do relatório.
     */
    private void appendMaterialSnapshot(StringBuilder markdown, Object material) {
        markdown.append("## 8. Snapshot consolidado do backend\n\n");
        appendJsonBlock(markdown, "experiment_report_material", material);
    }

    /**
     * Adiciona um bloco bruto nomeado, formatando JSON quando possível.
     */
    private void appendNamedRawBlock(StringBuilder markdown, String title, String rawValue) {
        markdown.append("### ").append(title).append("\n\n");
        appendCodeBlock(markdown, detectLanguage(rawValue), prettyJson(rawValue));
    }

    /**
     * Adiciona um objeto serializado como JSON em bloco Markdown.
     */
    private void appendJsonBlock(StringBuilder markdown, String title, Object value) {
        markdown.append("#### ").append(title).append("\n\n");
        appendCodeBlock(markdown, "json", toPrettyJson(value));
    }

    /**
     * Adiciona um bloco de código Markdown.
     */
    private void appendCodeBlock(StringBuilder markdown, String language, String value) {
        markdown.append("```").append(language == null ? "" : language).append("\n")
                .append(value == null || value.isBlank() ? "—" : value.trim())
                .append("\n```\n\n");
    }

    /**
     * Detecta se um texto bruto deve ser exibido como JSON ou texto simples.
     */
    private String detectLanguage(String rawValue) {
        if (rawValue == null) {
            return "";
        }
        String trimmed = rawValue.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]")) ? "json" : "";
    }

    /**
     * Formata JSON textual quando o conteúdo for válido.
     */
    private String prettyJson(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "—";
        }
        try {
            Object parsed = objectMapper.readValue(rawValue, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
        } catch (JsonProcessingException ex) {
            return rawValue;
        }
    }

    /**
     * Serializa qualquer objeto como JSON identado.
     */
    private String toPrettyJson(Object value) {
        if (value == null) {
            return "—";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar seção do relatório completo", ex);
        }
    }

    /**
     * Tenta converter texto JSON em estrutura para evitar JSON escapado dentro de JSON.
     */
    private Object parseMaybeJson(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawValue, Object.class);
        } catch (JsonProcessingException ex) {
            return rawValue;
        }
    }

    /**
     * Cria um mapa ordenado ignorando pares com quantidade inválida.
     */
    private Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }

    /**
     * Converte o identificador do job gravado como bytes para texto.
     */
    private String fromDatabaseIdJob(byte[] idJob) {
        return idJob == null ? "—" : new String(idJob, StandardCharsets.UTF_8);
    }

    /**
     * Retorna texto seguro para campos livres.
     */
    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    /**
     * Retorna a representação textual de valores possivelmente nulos.
     */
    private String value(Object value) {
        return Objects.toString(value, "—");
    }

    /**
     * Acumula o tempo visível e eventos de um trecho da landing para o relatório completo.
     */
    private static final class LandingAnalyticsSectionAccumulator {
        private final String sectionId;
        private long visibleMs;
        private long events;

        /** Cria o acumulador para um trecho identificado da landing. */
        private LandingAnalyticsSectionAccumulator(String sectionId) {
            this.sectionId = sectionId;
        }

        /** Soma tempo visível e eventos ao trecho agregado. */
        private void record(long visibleMs, long events) {
            this.visibleMs += visibleMs;
            this.events += events;
        }

        /** Retorna o identificador do trecho da landing. */
        private String sectionId() {
            return sectionId;
        }

        /** Retorna o tempo visível acumulado do trecho. */
        private long visibleMs() {
            return visibleMs;
        }

        /** Retorna a quantidade de eventos registrados no trecho. */
        private long events() {
            return events;
        }
    }

}
