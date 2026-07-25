package com.marketinghub.salesvideo.service;

import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetProviderReviewProjection;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoCommercialPlaybookRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoConversionEventRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoScriptRepository;
import com.marketinghub.salesvideo.*;
import com.marketinghub.salesvideo.dto.*;
import com.marketinghub.salesvideo.exception.VideoModuleErrorCode;
import com.marketinghub.salesvideo.exception.VideoModuleException;
import com.marketinghub.salesvideo.tenant.TenantContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Consolida aprendizados comerciais dos vídeos a partir de eventos, jobs e revisão visual.
 */
@Component
public class SalesVideoCommercialInsightsService {
    private final SalesVideoProfileRepository profileRepository;
    private final SalesVideoJobRepository jobRepository;
    private final SalesVideoScriptRepository scriptRepository;
    private final SalesVideoCommercialPlaybookRepository playbookRepository;
    private final SalesVideoConversionEventRepository conversionEventRepository;
    private final ExperimentVideoAssetRepository experimentVideoAssetRepository;

    public SalesVideoCommercialInsightsService(SalesVideoProfileRepository profileRepository,
                                               SalesVideoJobRepository jobRepository,
                                               SalesVideoScriptRepository scriptRepository,
                                               SalesVideoCommercialPlaybookRepository playbookRepository,
                                               SalesVideoConversionEventRepository conversionEventRepository,
                                               ExperimentVideoAssetRepository experimentVideoAssetRepository) {
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.scriptRepository = scriptRepository;
        this.playbookRepository = playbookRepository;
        this.conversionEventRepository = conversionEventRepository;
        this.experimentVideoAssetRepository = experimentVideoAssetRepository;
    }

    @Transactional
    public SalesVideoCommercialPlaybookDto createPlaybook(Long profileId, CreateSalesVideoCommercialPlaybookRequest request) {
        SalesVideoProfile profile = loadProfile(profileId);
        SalesVideoCommercialPlaybook playbook = SalesVideoCommercialPlaybook.builder()
                .profile(profile)
                .tenantId(profile.getTenantId())
                .nicheKey(request.getNicheKey().trim())
                .variantKey(request.getVariantKey().trim())
                .objectionText(request.getObjectionText().trim())
                .ctaText(request.getCtaText().trim())
                .active(request.getActive() == null || request.getActive())
                .createdBy(TenantContextHolder.resolveUserEmail(request.getCreatedBy()))
                .build();
        return toDto(playbookRepository.save(playbook));
    }

    @Transactional(readOnly = true)
    public List<SalesVideoCommercialPlaybookDto> listPlaybooks(Long profileId) {
        SalesVideoProfile profile = loadProfile(profileId);
        return playbookRepository.findByProfileIdAndTenantIdOrderByCreatedAtDesc(profileId, profile.getTenantId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public SalesVideoConversionEventDto createConversionEvent(Long profileId, CreateSalesVideoConversionEventRequest request) {
        SalesVideoProfile profile = loadProfile(profileId);
        SalesVideoJob job = null;
        if (request.getJobId() != null) {
            job = jobRepository.findById(request.getJobId())
                    .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.JOB_NOT_FOUND,
                            "Job de vídeo não encontrado: " + request.getJobId()));
            TenantContextHolder.assertTenant(job.getTenantId());
            if (!Objects.equals(job.getProfile().getId(), profileId)) {
                throw VideoModuleException.badRequest(VideoModuleErrorCode.BAD_REQUEST,
                        "O job informado não pertence ao perfil de vídeo.");
            }
        }
        SalesVideoScript script = null;
        if (request.getScriptId() != null) {
            script = scriptRepository.findById(request.getScriptId())
                    .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.SCRIPT_NOT_FOUND,
                            "Script não encontrado: " + request.getScriptId()));
            if (!Objects.equals(script.getProfile().getId(), profileId)) {
                throw VideoModuleException.badRequest(VideoModuleErrorCode.BAD_REQUEST,
                        "O script informado não pertence ao perfil de vídeo.");
            }
        } else if (job != null) {
            script = job.getScript();
        }
        SalesVideoConversionEvent event = SalesVideoConversionEvent.builder()
                .profile(profile)
                .job(job)
                .script(script)
                .tenantId(profile.getTenantId())
                .eventType(request.getEventType())
                .eventValue(request.getEventValue())
                .currencyCode(normalizeBlank(request.getCurrencyCode()))
                .source(normalizeBlank(request.getSource()))
                .occurredAt(Optional.ofNullable(request.getOccurredAt()).orElse(Instant.now()))
                .metadataJson(normalizeBlank(request.getMetadataJson()))
                .build();
        return toDto(conversionEventRepository.save(event));
    }

    /** Resume a performance comercial e a reputação dos providers do perfil. */
    @Transactional(readOnly = true)
    public SalesVideoPerformanceSummaryDto summarizePerformance(Long profileId, Instant from, Instant to) {
        SalesVideoProfile profile = loadProfile(profileId);
        List<SalesVideoConversionEvent> events;
        if (from != null && to != null) {
            events = conversionEventRepository.findByProfileIdAndTenantIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                    profileId, profile.getTenantId(), from, to);
        } else {
            events = conversionEventRepository.findByProfileIdAndTenantIdOrderByOccurredAtDesc(profileId, profile.getTenantId());
        }

        long totalLeads = events.stream()
                .filter(event -> event.getEventType() == SalesVideoConversionEventType.LEAD
                        || event.getEventType() == SalesVideoConversionEventType.QUALIFIED_LEAD)
                .count();
        long totalPurchases = events.stream()
                .filter(event -> event.getEventType() == SalesVideoConversionEventType.PURCHASE)
                .count();
        BigDecimal totalRevenue = events.stream()
                .map(SalesVideoConversionEvent::getEventValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, VariantAccumulator> matrix = new LinkedHashMap<>();
        List<SalesVideoCommercialPlaybook> playbooks = playbookRepository
                .findByProfileIdAndTenantIdOrderByCreatedAtDesc(profileId, profile.getTenantId());
        String defaultVariant = playbooks.stream()
                .filter(SalesVideoCommercialPlaybook::isActive)
                .map(SalesVideoCommercialPlaybook::getVariantKey)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("default");
        for (SalesVideoConversionEvent event : events) {
            Long scriptId = event.getScript() != null ? event.getScript().getId() : null;
            String providerName = event.getJob() != null ? event.getJob().getProviderName() : null;
            String variantKey = scriptId != null ? "script-" + scriptId : defaultVariant;
            String key = (scriptId == null ? "null" : scriptId) + "|" + (providerName == null ? "unknown" : providerName);
            VariantAccumulator accumulator = matrix.computeIfAbsent(key,
                    ignored -> new VariantAccumulator(scriptId, providerName, variantKey));
            accumulator.events++;
            if (event.getEventType() == SalesVideoConversionEventType.LEAD
                    || event.getEventType() == SalesVideoConversionEventType.QUALIFIED_LEAD) {
                accumulator.leads++;
            }
            if (event.getEventType() == SalesVideoConversionEventType.PURCHASE) {
                accumulator.purchases++;
            }
            if (event.getEventValue() != null) {
                accumulator.revenue = accumulator.revenue.add(event.getEventValue());
            }
        }

        List<SalesVideoVariantPerformanceDto> variants = matrix.values()
                .stream()
                .map(VariantAccumulator::toDto)
                .toList();
        List<SalesVideoJob> jobs = jobRepository.findByProfileIdOrderByRequestedAtDesc(profileId)
                .stream()
                .filter(job -> Objects.equals(job.getTenantId(), profile.getTenantId()))
                .toList();
        List<ExperimentVideoAssetProviderReviewProjection> providerReviews =
                experimentVideoAssetRepository.findProviderReviewsBySalesVideoProfileId(profileId);
        List<SalesVideoProviderScoreDto> providerScores = summarizeProviderScores(jobs, providerReviews, events);

        return SalesVideoPerformanceSummaryDto.builder()
                .profileId(profileId)
                .tenantId(profile.getTenantId())
                .totalEvents(events.size())
                .totalLeads(totalLeads)
                .totalPurchases(totalPurchases)
                .totalRevenue(totalRevenue)
                .variants(variants)
                .providerScores(providerScores)
                .build();
    }

    /** Resume a reputação global dos providers de vídeo no tenant atual. */
    @Transactional(readOnly = true)
    public List<SalesVideoProviderScoreDto> summarizeProviderScores() {
        String tenantId = TenantContextHolder.requireTenant();
        List<SalesVideoJob> jobs = jobRepository.findByTenantIdOrderByRequestedAtDesc(tenantId);
        List<ExperimentVideoAssetProviderReviewProjection> providerReviews =
                experimentVideoAssetRepository.findProviderReviewsByTenantId(tenantId);
        List<SalesVideoConversionEvent> events = conversionEventRepository.findByTenantIdOrderByOccurredAtDesc(tenantId);
        return summarizeProviderScores(jobs, providerReviews, events);
    }

    /** Calcula a reputação dos providers com base em qualidade, falha técnica e conversão. */
    private List<SalesVideoProviderScoreDto> summarizeProviderScores(List<SalesVideoJob> jobs,
                                                                     List<ExperimentVideoAssetProviderReviewProjection> providerReviews,
                                                                     List<SalesVideoConversionEvent> events) {
        Map<String, ProviderScoreAccumulator> providers = new LinkedHashMap<>();
        for (SalesVideoJob job : jobs) {
            ProviderScoreAccumulator accumulator = providers.computeIfAbsent(normalizeProvider(job.getProviderName()),
                    ProviderScoreAccumulator::new);
            if (job.getStatus() == SalesVideoStatus.VIDEO_READY || job.getStatus() == SalesVideoStatus.PUBLISHED) {
                accumulator.readyJobs++;
            }
            if (job.getStatus() == SalesVideoStatus.VIDEO_FAILED) {
                accumulator.failedJobs++;
            }
        }
        for (ExperimentVideoAssetProviderReviewProjection asset : providerReviews) {
            ProviderScoreAccumulator accumulator = providers.computeIfAbsent(normalizeProvider(asset.getProvider()),
                    ProviderScoreAccumulator::new);
            if ("READY".equals(asset.getStatus()) && "APPROVED".equals(asset.getReviewStatus())) {
                accumulator.approvedAssets++;
            }
            if ("REJECTED".equals(asset.getReviewStatus())) {
                accumulator.rejectedAssets++;
            }
        }
        for (SalesVideoConversionEvent event : events) {
            String providerName = event.getJob() != null ? event.getJob().getProviderName() : null;
            ProviderScoreAccumulator accumulator = providers.computeIfAbsent(normalizeProvider(providerName),
                    ProviderScoreAccumulator::new);
            if (event.getEventType() == SalesVideoConversionEventType.LEAD) {
                accumulator.leads++;
            }
            if (event.getEventType() == SalesVideoConversionEventType.QUALIFIED_LEAD) {
                accumulator.qualifiedLeads++;
            }
            if (event.getEventType() == SalesVideoConversionEventType.CHECKOUT_STARTED) {
                accumulator.checkoutStarts++;
            }
            if (event.getEventType() == SalesVideoConversionEventType.PURCHASE) {
                accumulator.purchases++;
            }
            if (event.getEventValue() != null) {
                accumulator.revenue = accumulator.revenue.add(event.getEventValue());
            }
        }
        return providers.values()
                .stream()
                .map(ProviderScoreAccumulator::toDto)
                .sorted(Comparator.comparing(SalesVideoProviderScoreDto::getScore).reversed()
                        .thenComparing(SalesVideoProviderScoreDto::getProviderName))
                .toList();
    }

    /** Normaliza providers ausentes para não perder fatos comerciais no resumo. */
    private static String normalizeProvider(String providerName) {
        return StringUtils.hasText(providerName) ? providerName.trim() : "unknown";
    }

    private SalesVideoProfile loadProfile(Long profileId) {
        SalesVideoProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> VideoModuleException.notFound(VideoModuleErrorCode.PROFILE_NOT_FOUND,
                        "Perfil de vídeo não encontrado: " + profileId));
        TenantContextHolder.assertTenant(profile.getTenantId());
        return profile;
    }

    private static String normalizeBlank(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SalesVideoCommercialPlaybookDto toDto(SalesVideoCommercialPlaybook playbook) {
        return SalesVideoCommercialPlaybookDto.builder()
                .id(playbook.getId())
                .profileId(playbook.getProfile().getId())
                .tenantId(playbook.getTenantId())
                .nicheKey(playbook.getNicheKey())
                .variantKey(playbook.getVariantKey())
                .objectionText(playbook.getObjectionText())
                .ctaText(playbook.getCtaText())
                .active(playbook.isActive())
                .createdBy(playbook.getCreatedBy())
                .createdAt(playbook.getCreatedAt())
                .updatedAt(playbook.getUpdatedAt())
                .build();
    }

    private SalesVideoConversionEventDto toDto(SalesVideoConversionEvent event) {
        return SalesVideoConversionEventDto.builder()
                .id(event.getId())
                .profileId(event.getProfile().getId())
                .jobId(event.getJob() != null ? event.getJob().getId() : null)
                .scriptId(event.getScript() != null ? event.getScript().getId() : null)
                .tenantId(event.getTenantId())
                .eventType(event.getEventType())
                .eventValue(event.getEventValue())
                .currencyCode(event.getCurrencyCode())
                .source(event.getSource())
                .occurredAt(event.getOccurredAt())
                .metadataJson(event.getMetadataJson())
                .createdAt(event.getCreatedAt())
                .build();
    }

    private static final class VariantAccumulator {
        private final Long scriptId;
        private final String providerName;
        private final String variantKey;
        private long events;
        private long leads;
        private long purchases;
        private BigDecimal revenue = BigDecimal.ZERO;

        private VariantAccumulator(Long scriptId, String providerName, String variantKey) {
            this.scriptId = scriptId;
            this.providerName = providerName;
            this.variantKey = variantKey;
        }

        private SalesVideoVariantPerformanceDto toDto() {
            return SalesVideoVariantPerformanceDto.builder()
                    .scriptId(scriptId)
                    .providerName(providerName)
                    .variantKey(variantKey)
                    .events(events)
                    .leads(leads)
                    .purchases(purchases)
                    .revenue(revenue)
                    .build();
        }
    }

    /**
     * Acumula sinais comerciais e técnicos para calcular a reputação de um provider.
     */
    private static final class ProviderScoreAccumulator {
        private final String providerName;
        private long readyJobs;
        private long failedJobs;
        private long approvedAssets;
        private long rejectedAssets;
        private long leads;
        private long qualifiedLeads;
        private long checkoutStarts;
        private long purchases;
        private BigDecimal revenue = BigDecimal.ZERO;

        private ProviderScoreAccumulator(String providerName) {
            this.providerName = providerName;
        }

        /** Converte os sinais acumulados em DTO de pontuação comercial. */
        private SalesVideoProviderScoreDto toDto() {
            int score = Math.max(0, Math.min(100, 50
                    + (int) readyJobs * 8
                    + (int) approvedAssets * 10
                    + (int) leads * 4
                    + (int) qualifiedLeads * 8
                    + (int) checkoutStarts * 18
                    + (int) purchases * 30
                    - (int) failedJobs * 12
                    - (int) rejectedAssets * 25));
            return SalesVideoProviderScoreDto.builder()
                    .providerName(providerName)
                    .score(score)
                    .readyJobs(readyJobs)
                    .failedJobs(failedJobs)
                    .approvedAssets(approvedAssets)
                    .rejectedAssets(rejectedAssets)
                    .leads(leads)
                    .qualifiedLeads(qualifiedLeads)
                    .checkoutStarts(checkoutStarts)
                    .purchases(purchases)
                    .revenue(revenue)
                    .recommendation(recommendation(score, rejectedAssets, failedJobs, purchases))
                    .build();
        }

        /** Define recomendação operacional para o provider conforme score e falhas críticas. */
        private String recommendation(int score, long rejectedAssets, long failedJobs, long purchases) {
            if (rejectedAssets > 0 || score < 40) {
                return "bloquear_ou_regenerar";
            }
            if (purchases > 0 || score >= 75) {
                return "priorizar";
            }
            if (failedJobs > 0 || score < 60) {
                return "usar_com_cautela";
            }
            return "testar_controlado";
        }
    }
}
