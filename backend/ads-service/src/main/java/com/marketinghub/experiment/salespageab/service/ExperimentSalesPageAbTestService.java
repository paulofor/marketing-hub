package com.marketinghub.experiment.salespageab.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTest;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbTestStatus;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariant;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantStatus;
import com.marketinghub.experiment.salespageab.ExperimentSalesPageAbVariantType;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbTestDto;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbTestResultDto;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbVariantDto;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbVariantResultDto;
import com.marketinghub.experiment.salespageab.dto.UpdateExperimentSalesPageAbVariantRequest;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.salespageab.ExperimentSalesPageAbTestRepository;
import com.marketinghub.repository.jpa.experiment.salespageab.ExperimentSalesPageAbVariantRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: criar e manter testes A/B de pagina de venda ligados ao experimento. */
@Service
public class ExperimentSalesPageAbTestService {
    private static final BigDecimal HALF_TRAFFIC = new BigDecimal("50.00");
    private static final int RATE_SCALE = 4;
    private static final List<ExperimentSalesPageAbTestStatus> ACTIVE_STATUSES = List.of(
            ExperimentSalesPageAbTestStatus.DRAFT,
            ExperimentSalesPageAbTestStatus.READY,
            ExperimentSalesPageAbTestStatus.RUNNING);

    private final ExperimentSalesPageAbTestRepository testRepository;
    private final ExperimentSalesPageAbVariantRepository variantRepository;
    private final ExperimentRepository experimentRepository;
    private final GeraSalesPagePublicationAuditRepository publicationAuditRepository;
    private final ExperimentVideoAssetRepository videoAssetRepository;
    private final JdbcTemplate jdbcTemplate;

    /** Inicializa o servico com as fontes canonicas de experimento, publicacao e video. */
    public ExperimentSalesPageAbTestService(ExperimentSalesPageAbTestRepository testRepository,
                                            ExperimentSalesPageAbVariantRepository variantRepository,
                                            ExperimentRepository experimentRepository,
                                            GeraSalesPagePublicationAuditRepository publicationAuditRepository,
                                            ExperimentVideoAssetRepository videoAssetRepository,
                                            JdbcTemplate jdbcTemplate) {
        this.testRepository = testRepository;
        this.variantRepository = variantRepository;
        this.experimentRepository = experimentRepository;
        this.publicationAuditRepository = publicationAuditRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Cria o plano padrao recomendado para Meta: pagina tradicional contra pagina com video humano. */
    @Transactional
    public ExperimentSalesPageAbTestDto createMetaVideoVsTraditional(Long experimentId) {
        Experiment experiment = findExperiment(experimentId);
        ExperimentSalesPageAbTest test = ExperimentSalesPageAbTest.builder()
                .experiment(experiment)
                .name("A/B Meta - pagina tradicional vs video humano")
                .status(ExperimentSalesPageAbTestStatus.DRAFT)
                .hypothesis("Video humano curto antes da prova visual aumenta confianca e clique no checkout sem alterar oferta, preco, publico ou criativo.")
                .primaryMetric("checkout_click_rate")
                .secondaryMetrics("purchase_rate,cost_per_checkout_click,cost_per_purchase,video_50_percent")
                .winnerRule("Vencer a variante com melhor custo por checkout_click; confirmar por purchase quando houver volume suficiente.")
                .minimumRuntimeDays(7)
                .minimumSampleSize(100)
                .metaSplitTestRecommended(true)
                .notes("Manter oferta, preco, checkout, publico, criativos e orcamento equivalentes. Testar apenas a presenca do video humano.")
                .build();
        test.getVariants().add(buildVariant(test, "A", "Pagina tradicional", ExperimentSalesPageAbVariantType.TRADITIONAL));
        test.getVariants().add(buildVariant(test, "B", "Pagina com video humano", ExperimentSalesPageAbVariantType.HUMAN_VIDEO));
        return toDto(testRepository.save(test));
    }

    /** Lista os planos A/B cadastrados para um experimento. */
    @Transactional(readOnly = true)
    public List<ExperimentSalesPageAbTestDto> list(Long experimentId) {
        findExperiment(experimentId);
        return testRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Lista os testes A/B com desempenho por variante calculado a partir do analytics da landing. */
    @Transactional(readOnly = true)
    public List<ExperimentSalesPageAbTestResultDto> results(Long experimentId) {
        Experiment experiment = findExperiment(experimentId);
        Instant baseline = resolveBaseline(experiment);
        return testRepository.findByExperimentIdOrderByCreatedAtDesc(experimentId).stream()
                .map(test -> toResultDto(toDto(test), baseline))
                .toList();
    }

    /** Retorna o teste aberto mais recente para validar prontidao e informar workers de campanha. */
    @Transactional(readOnly = true)
    public Optional<ExperimentSalesPageAbTestDto> findActiveForCampaign(Long experimentId) {
        return testRepository.findTopByExperimentIdAndStatusInOrderByUpdatedAtDesc(experimentId, ACTIVE_STATUSES)
                .map(this::toDto);
    }

    /** Atualiza uma variante com URLs, vinculos de auditoria e status de prontidao. */
    @Transactional
    public ExperimentSalesPageAbTestDto updateVariant(
            Long experimentId,
            Long variantId,
            UpdateExperimentSalesPageAbVariantRequest request) {
        findExperiment(experimentId);
        ExperimentSalesPageAbVariant variant = variantRepository.findByIdAndTestExperimentId(variantId, experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ab test variant not found"));
        applyVariantUpdate(variant, request, experimentId);
        normalizeTestStatus(variant.getTest());
        variantRepository.save(variant);
        return toDto(variant.getTest());
    }

    /** Verifica se o teste A/B ativo possui duas variantes prontas e equivalentes para campanha. */
    @Transactional(readOnly = true)
    public boolean hasReadyActiveTest(Long experimentId) {
        return testRepository.findTopByExperimentIdAndStatusInOrderByUpdatedAtDesc(experimentId, ACTIVE_STATUSES)
                .map(test -> test.getVariants().size() == 2
                        && test.getVariants().stream().allMatch(this::isVariantReadyForTraffic))
                .orElse(true);
    }

    /** Verifica se existe variante ativa de pagina com vídeo humano no teste A/B. */
    @Transactional(readOnly = true)
    public boolean hasHumanVideoVariant(Long experimentId) {
        return testRepository.findTopByExperimentIdAndStatusInOrderByUpdatedAtDesc(experimentId, ACTIVE_STATUSES)
                .map(test -> test.getVariants().stream()
                        .anyMatch(variant -> variant.getVariantType() == ExperimentSalesPageAbVariantType.HUMAN_VIDEO))
                .orElse(false);
    }

    /** Cria uma variante inicial com divisao de trafego 50/50. */
    private ExperimentSalesPageAbVariant buildVariant(
            ExperimentSalesPageAbTest test,
            String key,
            String name,
            ExperimentSalesPageAbVariantType type) {
        return ExperimentSalesPageAbVariant.builder()
                .test(test)
                .variantKey(key)
                .name(name)
                .variantType(type)
                .status(ExperimentSalesPageAbVariantStatus.DRAFT)
                .trafficWeight(HALF_TRAFFIC)
                .analyticsVariantParam("ab=" + key.toLowerCase())
                .requiredCollectorsPresent(false)
                .build();
    }

    /** Aplica somente os campos enviados na variante. */
    private void applyVariantUpdate(
            ExperimentSalesPageAbVariant variant,
            UpdateExperimentSalesPageAbVariantRequest request,
            Long experimentId) {
        if (request == null) {
            return;
        }
        if (request.status() != null) {
            variant.setStatus(request.status());
        }
        if (request.trafficWeight() != null) {
            variant.setTrafficWeight(request.trafficWeight());
        }
        if (request.salesPageUrl() != null) {
            variant.setSalesPageUrl(request.salesPageUrl());
        }
        if (request.checkoutUrl() != null) {
            variant.setCheckoutUrl(request.checkoutUrl());
        }
        if (request.adDestinationUrl() != null) {
            variant.setAdDestinationUrl(request.adDestinationUrl());
        }
        if (request.analyticsVariantParam() != null) {
            variant.setAnalyticsVariantParam(request.analyticsVariantParam());
        }
        if (request.requiredCollectorsPresent() != null) {
            variant.setRequiredCollectorsPresent(request.requiredCollectorsPresent());
        }
        if (request.publicationAuditId() != null) {
            variant.setPublicationAudit(resolvePublicationAudit(request.publicationAuditId(), experimentId));
        }
        if (request.experimentVideoAssetId() != null) {
            variant.setExperimentVideoAsset(resolveVideoAsset(request.experimentVideoAssetId(), experimentId));
        }
    }

    /** Promove o teste para READY apenas quando as duas variantes estao prontas para trafego. */
    private void normalizeTestStatus(ExperimentSalesPageAbTest test) {
        boolean ready = test.getVariants() != null
                && test.getVariants().size() == 2
                && test.getVariants().stream().allMatch(this::isVariantReadyForTraffic);
        if (ready && test.getStatus() == ExperimentSalesPageAbTestStatus.DRAFT) {
            test.setStatus(ExperimentSalesPageAbTestStatus.READY);
        }
    }

    /** Busca o experimento alvo ou retorna erro HTTP claro. */
    private Experiment findExperiment(Long experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
    }

    /** Busca a auditoria de publicacao e impede vinculo com outro experimento. */
    private GeraSalesPagePublicationAudit resolvePublicationAudit(Long auditId, Long experimentId) {
        GeraSalesPagePublicationAudit audit = publicationAuditRepository.findById(auditId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "publication audit not found"));
        if (!experimentId.equals(audit.getExperimentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "publication audit belongs to another experiment");
        }
        return audit;
    }

    /** Busca o video e impede vinculo com outro experimento. */
    private ExperimentVideoAsset resolveVideoAsset(Long videoAssetId, Long experimentId) {
        ExperimentVideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "video asset not found"));
        Long assetExperimentId = asset.getExperiment() != null ? asset.getExperiment().getId() : null;
        if (!experimentId.equals(assetExperimentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "video asset belongs to another experiment");
        }
        return asset;
    }

    /** Confirma se a variante pode receber trafego pago. */
    private boolean isVariantReadyForTraffic(ExperimentSalesPageAbVariantDto variant) {
        return variant.status() == ExperimentSalesPageAbVariantStatus.READY
                && StringUtils.hasText(variant.salesPageUrl())
                && StringUtils.hasText(variant.adDestinationUrl())
                && variant.salesPageUrl().equals(variant.adDestinationUrl())
                && StringUtils.hasText(variant.checkoutUrl())
                && variant.requiredCollectorsPresent()
                && variant.trafficWeight() != null
                && variant.trafficWeight().signum() > 0;
    }

    /** Confirma se a entidade da variante pode receber trafego, incluindo vídeo quando exigido. */
    private boolean isVariantReadyForTraffic(ExperimentSalesPageAbVariant variant) {
        return isVariantReadyForTraffic(toVariantDto(variant))
                && (!requiresApprovedVideo(variant) || hasReadyApprovedVideo(variant.getExperimentVideoAsset()));
    }

    /** Identifica variantes cuja promessa comercial depende de vídeo humano. */
    private boolean requiresApprovedVideo(ExperimentSalesPageAbVariant variant) {
        return variant != null && variant.getVariantType() == ExperimentSalesPageAbVariantType.HUMAN_VIDEO;
    }

    /** Verifica se o vídeo vinculado está pronto e aprovado para tráfego pago. */
    private boolean hasReadyApprovedVideo(ExperimentVideoAsset videoAsset) {
        return videoAsset != null
                && videoAsset.getStatus() == ExperimentVideoStatus.READY
                && videoAsset.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED;
    }

    /** Monta o resultado de um teste usando as URLs parametrizadas das variantes. */
    private ExperimentSalesPageAbTestResultDto toResultDto(ExperimentSalesPageAbTestDto test, Instant baseline) {
        List<ExperimentSalesPageAbVariantResultDto> variantResults = test.variants().stream()
                .map(variant -> toVariantResultDto(test.experimentId(), variant, baseline))
                .toList();
        String winnerVariantKey = resolveWinnerVariantKey(test, variantResults);
        String status = resolveResultStatus(test, variantResults, winnerVariantKey);
        return new ExperimentSalesPageAbTestResultDto(
                test,
                variantResults,
                winnerVariantKey,
                status,
                buildResultRecommendation(test, variantResults, status, winnerVariantKey));
    }

    /** Calcula os contadores de uma variante a partir dos eventos normalizados de analytics. */
    private ExperimentSalesPageAbVariantResultDto toVariantResultDto(Long experimentId,
                                                                    ExperimentSalesPageAbVariantDto variant,
                                                                    Instant baseline) {
        VariantAnalyticsAggregation aggregation = aggregateVariantAnalytics(experimentId, variant, baseline);
        return new ExperimentSalesPageAbVariantResultDto(
                variant,
                aggregation.pageViews(),
                aggregation.sessions(),
                aggregation.averageVisibleMsPerSession(),
                aggregation.checkoutClicks(),
                aggregation.purchases(),
                rate(aggregation.checkoutClicks(), aggregation.pageViews()),
                rate(aggregation.purchases(), aggregation.pageViews()),
                aggregation.lastEventAt());
    }

    /** Agrega eventos por parametro A/B presente na URL rastreada da pagina. */
    private VariantAnalyticsAggregation aggregateVariantAnalytics(Long experimentId,
                                                                 ExperimentSalesPageAbVariantDto variant,
                                                                 Instant baseline) {
        String variantPattern = "%" + normalizeVariantParam(variant) + "%";
        String sql = """
                SELECT
                    SUM(CASE WHEN LOWER(e.event_type) = 'page_view' THEN 1 ELSE 0 END) AS page_views,
                    COUNT(DISTINCT CASE WHEN session_id IS NOT NULL AND session_id <> '' THEN session_id ELSE NULL END) AS sessions,
                    COALESCE(
                        CAST(
                            SUM(CASE
                                WHEN LOWER(e.event_type) = 'section_view_time'
                                THEN CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(fe.payload, 'elapsedMs=', -1), ';', 1) AS UNSIGNED)
                                ELSE 0
                            END)
                            / NULLIF(COUNT(DISTINCT CASE WHEN e.session_id IS NOT NULL AND e.session_id <> '' THEN e.session_id ELSE NULL END), 0)
                            AS UNSIGNED
                        ),
                        0
                    ) AS average_visible_ms_per_session,
                    SUM(CASE WHEN LOWER(e.event_type) = 'checkout_click' THEN 1 ELSE 0 END) AS checkout_clicks,
                    SUM(CASE WHEN LOWER(e.event_type) = 'purchase' THEN 1 ELSE 0 END) AS purchases,
                    MAX(e.occurred_at) AS last_event_at
                FROM experiment_landing_analytics_event e
                JOIN experiment_funnel_event fe ON fe.id = e.funnel_event_id
                WHERE e.experiment_id = ?
                  AND LOWER(COALESCE(e.page_url, '')) LIKE ?
                  AND (? IS NULL OR e.occurred_at > ?)
                """;
        return jdbcTemplate.query(
                        sql,
                        new VariantAnalyticsAggregationRowMapper(),
                        experimentId,
                        variantPattern,
                        baseline,
                        baseline)
                .stream()
                .findFirst()
                .orElse(VariantAnalyticsAggregation.empty());
    }

    /** Normaliza o parametro configurado da variante para comparacao case-insensitive em URL. */
    private String normalizeVariantParam(ExperimentSalesPageAbVariantDto variant) {
        String configured = variant.analyticsVariantParam();
        String fallback = "ab=" + Optional.ofNullable(variant.variantKey()).orElse("").toLowerCase();
        String normalized = StringUtils.hasText(configured) ? configured.trim() : fallback;
        return normalized.toLowerCase();
    }

    /** Resolve a variante vencedora somente quando ja existe amostra minima e clique de checkout. */
    private String resolveWinnerVariantKey(ExperimentSalesPageAbTestDto test,
                                           List<ExperimentSalesPageAbVariantResultDto> variants) {
        long totalPageViews = variants.stream().mapToLong(ExperimentSalesPageAbVariantResultDto::pageViews).sum();
        long totalCheckoutClicks = variants.stream().mapToLong(ExperimentSalesPageAbVariantResultDto::checkoutClicks).sum();
        if (totalPageViews < Optional.ofNullable(test.minimumSampleSize()).orElse(0) || totalCheckoutClicks == 0) {
            return null;
        }
        return variants.stream()
                .max(Comparator
                        .comparing(ExperimentSalesPageAbVariantResultDto::checkoutClickRate)
                        .thenComparing(ExperimentSalesPageAbVariantResultDto::pageViews))
                .map(result -> result.variant().variantKey())
                .orElse(null);
    }

    /** Classifica o resultado do teste para a UI sem forcar decisao prematura. */
    private String resolveResultStatus(ExperimentSalesPageAbTestDto test,
                                       List<ExperimentSalesPageAbVariantResultDto> variants,
                                       String winnerVariantKey) {
        long totalPageViews = variants.stream().mapToLong(ExperimentSalesPageAbVariantResultDto::pageViews).sum();
        long totalCheckoutClicks = variants.stream().mapToLong(ExperimentSalesPageAbVariantResultDto::checkoutClicks).sum();
        if (totalPageViews == 0) {
            return "SEM_DADOS";
        }
        if (totalPageViews < Optional.ofNullable(test.minimumSampleSize()).orElse(0)) {
            return "AMOSTRA_INSUFICIENTE";
        }
        if (totalCheckoutClicks == 0) {
            return "SEM_CLIQUE_CHECKOUT";
        }
        return winnerVariantKey == null ? "INCONCLUSIVO" : "VENCEDOR_SUGERIDO";
    }

    /** Gera uma recomendacao comercial objetiva para a tela do experimento. */
    private String buildResultRecommendation(ExperimentSalesPageAbTestDto test,
                                             List<ExperimentSalesPageAbVariantResultDto> variants,
                                             String status,
                                             String winnerVariantKey) {
        long totalPageViews = variants.stream().mapToLong(ExperimentSalesPageAbVariantResultDto::pageViews).sum();
        long minimumSample = Optional.ofNullable(test.minimumSampleSize()).orElse(0);
        return switch (status) {
            case "SEM_DADOS" -> "Ainda nao ha eventos com ab=a ou ab=b suficientes para comparar as variantes.";
            case "AMOSTRA_INSUFICIENTE" -> "Continue o teste ate atingir pelo menos " + minimumSample
                    + " page views rastreados por A/B. Atual: " + totalPageViews + ".";
            case "SEM_CLIQUE_CHECKOUT" -> "Ainda nao houve clique no checkout; nao declare vencedor antes de gerar intencao de compra.";
            case "VENCEDOR_SUGERIDO" -> "Variante " + winnerVariantKey
                    + " lidera em taxa de clique no checkout. Confirme com mais volume e compras antes de escalar forte.";
            default -> "Resultado ainda inconclusivo. Mantenha oferta, preco, publico e criativos equivalentes.";
        };
    }

    /** Calcula taxa decimal com escala estavel para comparacao e exibicao. */
    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), RATE_SCALE, RoundingMode.HALF_UP);
    }

    /** Resolve o marco temporal operacional usado pelos paineis de funil do experimento. */
    private Instant resolveBaseline(Experiment experiment) {
        Instant release = experiment.getFacebookReleaseRequestedAt();
        Instant reset = experiment.getFunnelResetAt();
        if (release == null) {
            return reset;
        }
        if (reset == null) {
            return release;
        }
        return release.isAfter(reset) ? release : reset;
    }

    /** Converte o teste para contrato de API com variantes ordenadas. */
    private ExperimentSalesPageAbTestDto toDto(ExperimentSalesPageAbTest test) {
        List<ExperimentSalesPageAbVariantDto> variants = test.getVariants() == null
                ? List.of()
                : test.getVariants().stream()
                .sorted(Comparator.comparing(ExperimentSalesPageAbVariant::getVariantKey))
                .map(this::toVariantDto)
                .toList();
        return new ExperimentSalesPageAbTestDto(
                test.getId(),
                test.getExperiment() != null ? test.getExperiment().getId() : null,
                test.getName(),
                test.getStatus(),
                test.getHypothesis(),
                test.getPrimaryMetric(),
                test.getSecondaryMetrics(),
                test.getWinnerRule(),
                test.getMinimumRuntimeDays(),
                test.getMinimumSampleSize(),
                test.isMetaSplitTestRecommended(),
                test.getNotes(),
                variants,
                test.getCreatedAt(),
                test.getUpdatedAt());
    }

    /** Converte a variante para contrato de API. */
    private ExperimentSalesPageAbVariantDto toVariantDto(ExperimentSalesPageAbVariant variant) {
        return new ExperimentSalesPageAbVariantDto(
                variant.getId(),
                variant.getVariantKey(),
                variant.getName(),
                variant.getVariantType(),
                variant.getStatus(),
                variant.getTrafficWeight(),
                variant.getSalesPageUrl(),
                variant.getCheckoutUrl(),
                variant.getAdDestinationUrl(),
                buildMetricsSafeUrl(variant),
                variant.getAnalyticsVariantParam(),
                variant.getPublicationAudit() != null ? variant.getPublicationAudit().getId() : null,
                variant.getExperimentVideoAsset() != null ? variant.getExperimentVideoAsset().getId() : null,
                variant.isRequiredCollectorsPresent(),
                variant.getCreatedAt(),
                variant.getUpdatedAt());
    }

    /** Monta URL de revisao interna que o Lead Portal ignora nos coletores de metricas. */
    private String buildMetricsSafeUrl(ExperimentSalesPageAbVariant variant) {
        String rawUrl = StringUtils.hasText(variant.getSalesPageUrl())
                ? variant.getSalesPageUrl()
                : variant.getAdDestinationUrl();
        if (!StringUtils.hasText(rawUrl)) {
            return null;
        }
        try {
            return UriComponentsBuilder.fromUriString(rawUrl.trim())
                    .replaceQueryParam("mh_test", "1")
                    .build(true)
                    .toUriString();
        } catch (IllegalArgumentException ex) {
            String trimmed = rawUrl.trim();
            if (trimmed.contains("mh_test=1")) {
                return trimmed;
            }
            return trimmed + (trimmed.contains("?") ? "&" : "?") + "mh_test=1";
        }
    }

    /** Mapeia a agregacao SQL de analytics da variante para o contrato interno do servico. */
    private static class VariantAnalyticsAggregationRowMapper implements RowMapper<VariantAnalyticsAggregation> {

        /** Converte uma linha agregada em contadores de variante. */
        @Override
        public VariantAnalyticsAggregation mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp lastEventAt = rs.getTimestamp("last_event_at");
            return new VariantAnalyticsAggregation(
                    rs.getLong("page_views"),
                    rs.getLong("sessions"),
                    rs.getLong("average_visible_ms_per_session"),
                    rs.getLong("checkout_clicks"),
                    rs.getLong("purchases"),
                    lastEventAt != null ? lastEventAt.toInstant() : null);
        }
    }

    /** Representa a leitura agregada de analytics para uma variante A/B. */
    private record VariantAnalyticsAggregation(
            long pageViews,
            long sessions,
            long averageVisibleMsPerSession,
            long checkoutClicks,
            long purchases,
            Instant lastEventAt) {

        /** Retorna agregacao vazia para variantes sem eventos rastreados. */
        private static VariantAnalyticsAggregation empty() {
            return new VariantAnalyticsAggregation(0, 0, 0, 0, 0, null);
        }
    }
}
