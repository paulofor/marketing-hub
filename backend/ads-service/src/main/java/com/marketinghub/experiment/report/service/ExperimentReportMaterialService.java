package com.marketinghub.experiment.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.Creative;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.dto.ExperimentCampaignMetricDto;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.report.dto.ExperimentReportMaterialDto;
import com.marketinghub.repository.jpa.experiment.CreativeVariantRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.experiment.CreativeVariant;
import com.marketinghub.experiment.LandingPage;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.LeadPortalFlowQuestion;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.niche.MarketNiche;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Responsável por consolidar todas as informações necessárias para montar o relatório objetivo de um experimento.
 */
@Service
public class ExperimentReportMaterialService {

    private final ExperimentRepository experimentRepository;
    private final CreativeRepository creativeRepository;
    private final CreativeVariantRepository creativeVariantRepository;
    private final LandingPageRepository landingPageRepository;
    private final LeadPortalFlowRepository leadPortalFlowRepository;
    private final LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;
    private final ExperimentFunnelService experimentFunnelService;
    private final ObjectMapper objectMapper;

    /** Inicializa o serviço com repositórios e consolidadores do relatório do experimento. */
    public ExperimentReportMaterialService(ExperimentRepository experimentRepository,
                                           CreativeRepository creativeRepository,
                                           CreativeVariantRepository creativeVariantRepository,
                                           LandingPageRepository landingPageRepository,
                                           LeadPortalFlowRepository leadPortalFlowRepository,
                                           LeadPortalPublicUrlResolver leadPortalPublicUrlResolver,
                                           ExperimentFunnelService experimentFunnelService,
                                           ObjectMapper objectMapper) {
        this.experimentRepository = experimentRepository;
        this.creativeRepository = creativeRepository;
        this.creativeVariantRepository = creativeVariantRepository;
        this.landingPageRepository = landingPageRepository;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.leadPortalPublicUrlResolver = leadPortalPublicUrlResolver;
        this.experimentFunnelService = experimentFunnelService;
        this.objectMapper = objectMapper;
    }

    /** Consolida o material completo do relatório, incluindo funil e analytics da landing. */
    @Transactional(readOnly = true)
    public ExperimentReportMaterialDto build(Long experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new IllegalArgumentException("Experimento não encontrado: " + experimentId));

        List<Creative> creatives = creativeRepository.findByExperimentId(experimentId);
        List<CreativeVariant> variants = creativeVariantRepository.findByExperimentId(experimentId);
        List<LandingPage> landingPages = landingPageRepository.findByExperimentId(experimentId);
        List<LeadPortalFlow> flows = leadPortalFlowRepository.findAllByExperimentIdOrderByCreatedAtDesc(experimentId);
        List<ExperimentFunnelStageDto> funnelStages = experimentFunnelService.summarize(experimentId);

        ExperimentReportMaterialDto dto = ExperimentReportMaterialDto.builder()
                .experiment(toExperimentSnapshot(experiment))
                .niche(toMarketNicheSnapshot(experiment.getNiche()))
                .hypothesis(toHypothesisSnapshot(experiment.getHypothesisRef()))
                .instantForm(toInstantFormSnapshot(experiment.getFacebookInstantForm()))
                .campaignMetric(toCampaignMetricDto(experiment.getCampaignMetric()))
                .landingAnalytics(experimentFunnelService.summarizeLandingAnalytics(experimentId))
                .build();

        dto.setCreatives(mapCreatives(creatives));
        dto.setCreativeVariants(mapCreativeVariants(variants));
        dto.setLandingPages(mapLandingPages(landingPages));
        dto.setLeadPortalFlows(mapLeadPortalFlows(flows));
        dto.setFunnelStages(funnelStages);
        return dto;
    }

    private ExperimentReportMaterialDto.ExperimentSnapshot toExperimentSnapshot(Experiment experiment) {
        if (experiment == null) {
            return null;
        }
        return ExperimentReportMaterialDto.ExperimentSnapshot.builder()
                .id(experiment.getId())
                .name(experiment.getName())
                .status(experiment.getStatus() != null ? experiment.getStatus().name() : null)
                .platform(experiment.getPlatform() != null ? experiment.getPlatform().name() : null)
                .stage(experiment.getStage() != null ? experiment.getStage().name() : null)
                .captureDestinationType(experiment.getCaptureDestinationType() != null
                        ? experiment.getCaptureDestinationType().name()
                        : null)
                .primaryVariable(experiment.getPrimaryVariable())
                .primaryMetric(experiment.getPrimaryMetric())
                .startDate(experiment.getStartDate())
                .endDate(experiment.getEndDate())
                .dailyBudget(experiment.getDailyBudget())
                .kpiTargetCpl(experiment.getKpiTargetCpl())
                .stopLossCpl(experiment.getStopLossCpl())
                .sampleSize(experiment.getSampleSize())
                .baselineCvr(experiment.getBaselineCvr())
                .targetCvr(experiment.getTargetCvr())
                .mdePercent(experiment.getMdePercent())
                .createdAt(experiment.getCreatedAt())
                .build();
    }

    private ExperimentReportMaterialDto.MarketNicheSnapshot toMarketNicheSnapshot(MarketNiche niche) {
        if (niche == null) {
            return null;
        }
        return ExperimentReportMaterialDto.MarketNicheSnapshot.builder()
                .id(niche.getId())
                .name(niche.getName())
                .description(niche.getDescription())
                .interestList(Optional.ofNullable(niche.getInterestList()).orElse(Collections.emptyList()))
                .roleList(Optional.ofNullable(niche.getRoleList()).orElse(Collections.emptyList()))
                .behaviorList(Optional.ofNullable(niche.getBehaviorList()).orElse(Collections.emptyList()))
                .build();
    }

    private ExperimentReportMaterialDto.HypothesisSnapshot toHypothesisSnapshot(Hypothesis hypothesis) {
        if (hypothesis == null) {
            return null;
        }
        return ExperimentReportMaterialDto.HypothesisSnapshot.builder()
                .id(hypothesis.getId())
                .title(hypothesis.getTitle())
                .promise(hypothesis.getPromise())
                .problem(hypothesis.getProblem())
                .persona(hypothesis.getPersona())
                .mechanism(hypothesis.getMechanism())
                .uniqueMechanism(hypothesis.getUniqueMechanism())
                .entrega(hypothesis.getEntrega())
                .build();
    }

    private ExperimentReportMaterialDto.InstantFormSnapshot toInstantFormSnapshot(com.marketinghub.ads.FacebookInstantForm instantForm) {
        if (instantForm == null) {
            return null;
        }
        return ExperimentReportMaterialDto.InstantFormSnapshot.builder()
                .id(instantForm.getId())
                .name(instantForm.getName())
                .status(instantForm.getStatus())
                .facebookFormId(instantForm.getFormId())
                .shareLink(instantForm.getShareLink())
                .followUpActionUrl(instantForm.getFollowUpActionUrl())
                .privacyPolicyUrl(instantForm.getPrivacyPolicyUrl())
                .approved(instantForm.isApproved())
                .published(instantForm.isPublished())
                .build();
    }

    private List<ExperimentReportMaterialDto.CreativeSnapshot> mapCreatives(List<Creative> creatives) {
        if (creatives == null || creatives.isEmpty()) {
            return Collections.emptyList();
        }
        return creatives.stream()
                .sorted(Comparator.comparing(Creative::getId))
                .map(creative -> ExperimentReportMaterialDto.CreativeSnapshot.builder()
                        .id(creative.getId())
                        .headline(creative.getHeadline())
                        .primaryText(creative.getPrimaryText())
                        .description(creative.getDescription())
                        .cta(creative.getCta())
                        .destinationUrl(creative.getDestinationUrl())
                        .imageUrl(creative.getImageUrl())
                        .videoId(creative.getVideoId())
                        .format(creative.getFormat())
                        .status(creative.getStatus() != null ? creative.getStatus().name() : null)
                        .angles(toNameList(creative.getAngles(), angle -> angle.getName()))
                        .emotionalTriggers(toNameList(creative.getEmotionalTriggers(), trigger -> trigger.getName()))
                        .visualProofs(toNameList(creative.getVisualProofs(), proof -> proof.getName()))
                        .build())
                .toList();
    }

    private <T> List<String> toNameList(java.util.Collection<T> source, java.util.function.Function<T, String> extractor) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        return source.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private List<ExperimentReportMaterialDto.CreativeVariantSnapshot> mapCreativeVariants(List<CreativeVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return Collections.emptyList();
        }
        return variants.stream()
                .sorted(Comparator.comparing(CreativeVariant::getCreatedAt))
                .map(variant -> ExperimentReportMaterialDto.CreativeVariantSnapshot.builder()
                        .id(variant.getId())
                        .type(variant.getType() != null ? variant.getType().name() : null)
                        .assetUrl(variant.getAssetUrl())
                        .titles(parseTextList(variant.getTitles()))
                        .descriptions(parseTextList(variant.getDescriptions()))
                        .createdAt(variant.getCreatedAt())
                        .build())
                .toList();
    }

    private List<ExperimentReportMaterialDto.LandingPageSnapshot> mapLandingPages(List<LandingPage> landingPages) {
        if (landingPages == null || landingPages.isEmpty()) {
            return Collections.emptyList();
        }
        return landingPages.stream()
                .sorted(Comparator.comparing(LandingPage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(page -> ExperimentReportMaterialDto.LandingPageSnapshot.builder()
                        .id(page.getId())
                        .url(page.getUrl())
                        .type(page.getType() != null ? page.getType().name() : null)
                        .status(page.getStatus() != null ? page.getStatus().name() : null)
                        .createdAt(page.getCreatedAt())
                        .build())
                .toList();
    }

    private List<ExperimentReportMaterialDto.LeadPortalFlowSnapshot> mapLeadPortalFlows(List<LeadPortalFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            return Collections.emptyList();
        }
        return flows.stream()
                .map(flow -> ExperimentReportMaterialDto.LeadPortalFlowSnapshot.builder()
                        .id(flow.getId())
                        .name(flow.getName())
                        .slug(flow.getSlug())
                        .description(flow.getDescription())
                        .model(flow.getModel())
                        .approved(flow.isApproved())
                        .publicUrl(leadPortalPublicUrlResolver.resolve(flow))
                        .previewImageUrl(flow.getSimpleFormStyle() != null ? flow.getSimpleFormStyle().getPreviewImageUrl() : null)
                        .createdAt(flow.getCreatedAt())
                        .questions(mapLeadPortalQuestions(flow.getQuestions()))
                        .build())
                .toList();
    }

    private List<ExperimentReportMaterialDto.LeadPortalQuestionSnapshot> mapLeadPortalQuestions(List<LeadPortalFlowQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }
        return questions.stream()
                .sorted(Comparator.comparingInt(LeadPortalFlowQuestion::getPosition))
                .map(question -> ExperimentReportMaterialDto.LeadPortalQuestionSnapshot.builder()
                        .id(question.getId())
                        .title(question.getTitle())
                        .type(question.getType() != null ? question.getType().name() : null)
                        .required(question.isRequired())
                        .options(Optional.ofNullable(question.getOptions()).orElse(Collections.emptyList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Converte as métricas de campanha para o DTO usado pelo material de relatório.
     */
    private ExperimentCampaignMetricDto toCampaignMetricDto(ExperimentCampaignMetric metric) {
        if (metric == null) {
            return null;
        }
        ExperimentCampaignMetricDto dto = new ExperimentCampaignMetricDto();
        dto.setDateStart(metric.getDateStart());
        dto.setDateStop(metric.getDateStop());
        dto.setReach(metric.getReach());
        dto.setImpressions(metric.getImpressions());
        dto.setClicks(metric.getClicks());
        dto.setLeads(metric.getLeads());
        dto.setSpend(metric.getSpend());
        dto.setCpc(metric.getCpc());
        dto.setCpl(metric.getCpl());
        if (metric.getCampaign() != null) {
            dto.setLastSyncedAt(metric.getCampaign().getMetricsLastSyncedAt());
            dto.setLastSyncError(metric.getCampaign().getMetricsLastError());
        }
        return dto;
    }

    private List<String> parseTextList(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                return objectMapper.readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (IOException ignored) {
                // fallback para split simples
            }
        }
        return java.util.Arrays.stream(trimmed.split("\n"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }
}
