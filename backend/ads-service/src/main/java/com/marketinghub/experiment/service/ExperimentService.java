package com.marketinghub.experiment.service;

import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.sampleemail.SampleEmailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.repository.jpa.ads.FacebookInstantFormRepository;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.repository.jpa.ads.FacebookPageRepository;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.experiment.*;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.funnel.ExperimentFunnelStandbyService;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.gerasalespage.v1.GeraSalesPageAnalyticsContract;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.repository.jpa.experiment.ExperimentPromiseGenerationRequestRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.productai.service.ProductAiExperimentPreparationService;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.imagegeneration.ImageGenerationModel;
import com.marketinghub.imagegeneration.ImageGenerationQuality;
import com.marketinghub.repository.jpa.imagegeneration.ImageGenerationModelRepository;
import com.marketinghub.repository.jpa.imagegeneration.ImageGenerationQualityRepository;
import com.marketinghub.sampleemail.SampleEmail;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Orquestra as regras de negócio dos experimentos.
 */
@Service
public class ExperimentService {
    private final ExperimentRepository repository;
    private final ExperimentPromiseGenerationRequestRepository promiseGenerationRequestRepository;
    private final MarketNicheRepository nicheRepository;
    private final com.marketinghub.repository.jpa.hypothesis.HypothesisRepository hypothesisRepository;
    private final MetricPresetService metricPresetService;
    private final EntityManager entityManager;
    private final JourneyTemplateRepository journeyTemplateRepository;
    private final FacebookPageRepository facebookPageRepository;
    private final InstagramAccountRepository instagramAccountRepository;
    private final FacebookInstantFormRepository facebookInstantFormRepository;
    private final LeadPortalFlowRepository leadPortalFlowRepository;
    private final LeadPortalFlowPublisher leadPortalFlowPublisher;
    private final ImageGenerationModelRepository imageGenerationModelRepository;
    private final ImageGenerationQualityRepository imageGenerationQualityRepository;
    private final com.marketinghub.repository.jpa.sampleemail.SampleEmailRepository sampleEmailRepository;
    private final ExperimentFunnelEventRepository experimentFunnelEventRepository;
    private final ExperimentLandingAnalyticsEventRepository experimentLandingAnalyticsEventRepository;
    private final FacebookAdsCampaignRepository facebookAdsCampaignRepository;
    private final FacebookAdsAdSetRepository facebookAdsAdSetRepository;
    private final FacebookAdsAdRepository facebookAdsAdRepository;
    private final GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository;
    private final GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository;
    private final ObjectMapper objectMapper;
    private final CurrencyConversionService currencyConversionService;
    private final ExperimentAiPromptSchemaUsageService promptSchemaUsageService;
    private final ProductAiExperimentPreparationService productAiExperimentPreparationService;
    private final ExperimentFunnelStandbyService experimentFunnelStandbyService;

    public ExperimentService(ExperimentRepository repository,
                             ExperimentPromiseGenerationRequestRepository promiseGenerationRequestRepository,
                             MarketNicheRepository nicheRepository,
                             com.marketinghub.repository.jpa.hypothesis.HypothesisRepository hypothesisRepository,
                             MetricPresetService metricPresetService,
                             EntityManager entityManager,
                             JourneyTemplateRepository journeyTemplateRepository,
                             FacebookPageRepository facebookPageRepository,
                             InstagramAccountRepository instagramAccountRepository,
                             FacebookInstantFormRepository facebookInstantFormRepository,
                             LeadPortalFlowRepository leadPortalFlowRepository,
                             LeadPortalFlowPublisher leadPortalFlowPublisher,
                             ImageGenerationModelRepository imageGenerationModelRepository,
                             ImageGenerationQualityRepository imageGenerationQualityRepository,
                             com.marketinghub.repository.jpa.sampleemail.SampleEmailRepository sampleEmailRepository,
                             ExperimentFunnelEventRepository experimentFunnelEventRepository,
                             ExperimentLandingAnalyticsEventRepository experimentLandingAnalyticsEventRepository,
                             FacebookAdsCampaignRepository facebookAdsCampaignRepository,
                             FacebookAdsAdSetRepository facebookAdsAdSetRepository,
                             FacebookAdsAdRepository facebookAdsAdRepository,
                             GeraSalesPageStageExecutionRepository geraSalesPageStageExecutionRepository,
                             GeraSalesPagePublicationAuditRepository geraSalesPagePublicationAuditRepository,
                             ObjectMapper objectMapper,
                             CurrencyConversionService currencyConversionService,
                             ExperimentAiPromptSchemaUsageService promptSchemaUsageService,
                             ProductAiExperimentPreparationService productAiExperimentPreparationService,
                             ExperimentFunnelStandbyService experimentFunnelStandbyService) {
        this.repository = repository;
        this.promiseGenerationRequestRepository = promiseGenerationRequestRepository;
        this.nicheRepository = nicheRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.metricPresetService = metricPresetService;
        this.entityManager = entityManager;
        this.journeyTemplateRepository = journeyTemplateRepository;
        this.facebookPageRepository = facebookPageRepository;
        this.instagramAccountRepository = instagramAccountRepository;
        this.facebookInstantFormRepository = facebookInstantFormRepository;
        this.leadPortalFlowRepository = leadPortalFlowRepository;
        this.leadPortalFlowPublisher = leadPortalFlowPublisher;
        this.imageGenerationModelRepository = imageGenerationModelRepository;
        this.imageGenerationQualityRepository = imageGenerationQualityRepository;
        this.sampleEmailRepository = sampleEmailRepository;
        this.experimentFunnelEventRepository = experimentFunnelEventRepository;
        this.experimentLandingAnalyticsEventRepository = experimentLandingAnalyticsEventRepository;
        this.facebookAdsCampaignRepository = facebookAdsCampaignRepository;
        this.facebookAdsAdSetRepository = facebookAdsAdSetRepository;
        this.facebookAdsAdRepository = facebookAdsAdRepository;
        this.geraSalesPageStageExecutionRepository = geraSalesPageStageExecutionRepository;
        this.geraSalesPagePublicationAuditRepository = geraSalesPagePublicationAuditRepository;
        this.objectMapper = objectMapper;
        this.currencyConversionService = currencyConversionService;
        this.promptSchemaUsageService = promptSchemaUsageService;
        this.productAiExperimentPreparationService = productAiExperimentPreparationService;
        this.experimentFunnelStandbyService = experimentFunnelStandbyService;
    }

    /**
     * Obtains a managed reference to {@link MarketNiche} without hitting the database.
     * getReference() avoids {@code detached entity passed to persist} by associating
     * the proxy with the current persistence context.
     *
     * @throws EntityNotFoundException if the id does not exist
     */
    private MarketNiche attachNiche(Long nicheId) {
        if (!nicheRepository.existsById(nicheId)) {
            throw new EntityNotFoundException("MarketNiche not found: " + nicheId);
        }
        return entityManager.getReference(MarketNiche.class, nicheId);
    }

    private com.marketinghub.hypothesis.Hypothesis attachHypothesis(java.util.UUID id) {
        if (!hypothesisRepository.existsById(id)) {
            throw new EntityNotFoundException("Hypothesis not found: " + id);
        }
        return entityManager.getReference(com.marketinghub.hypothesis.Hypothesis.class, id);
    }

    private FacebookPage attachFacebookPage(Long facebookPageId) {
        if (facebookPageId == null) {
            return null;
        }
        if (!facebookPageRepository.existsById(facebookPageId)) {
            throw new EntityNotFoundException("FacebookPage not found: " + facebookPageId);
        }
        return entityManager.getReference(FacebookPage.class, facebookPageId);
    }

    private InstagramAccount attachInstagramAccount(Long instagramAccountId) {
        if (instagramAccountId == null) {
            return null;
        }
        if (!instagramAccountRepository.existsById(instagramAccountId)) {
            throw new EntityNotFoundException("InstagramAccount not found: " + instagramAccountId);
        }
        return entityManager.getReference(InstagramAccount.class, instagramAccountId);
    }

    private FacebookInstantForm attachInstantForm(Long instantFormId, java.util.UUID hypothesisId) {
        if (instantFormId == null) {
            return null;
        }
        FacebookInstantForm form = facebookInstantFormRepository.findById(instantFormId)
                .orElseThrow(() -> new EntityNotFoundException("FacebookInstantForm not found: " + instantFormId));
        if (hypothesisId != null && form.getHypothesis() != null && !form.getHypothesis().getId().equals(hypothesisId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "instant form does not belong to hypothesis");
        }
        return entityManager.getReference(FacebookInstantForm.class, instantFormId);
    }

    private JourneyTemplate attachJourneyTemplate(Long journeyTemplateId) {
        if (journeyTemplateId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "journeyTemplateId required");
        }
        if (!journeyTemplateRepository.existsById(journeyTemplateId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "journeyTemplateId not found: " + journeyTemplateId);
        }
        return entityManager.getReference(JourneyTemplate.class, journeyTemplateId);
    }

    private LeadPortalFlow attachLeadPortalFlow(Long flowId, Long nicheId) {
        if (flowId == null) {
            return null;
        }
        LeadPortalFlow flow = leadPortalFlowRepository.findById(flowId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "leadPortalFlowId not found: " + flowId));
        if (flow.getMarketNiche() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "leadPortalFlowId is not associated with a market niche");
        }
        if (nicheId != null && !flow.getMarketNiche().getId().equals(nicheId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "leadPortalFlowId must belong to the experiment's niche");
        }
        return entityManager.getReference(LeadPortalFlow.class, flowId);
    }

    private ImageGenerationModel attachImageGenerationModel(Long imageModelId) {
        if (imageModelId == null) {
            return null;
        }
        return imageGenerationModelRepository.findById(imageModelId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "imageModelId not found: " + imageModelId));
    }

    private ImageGenerationQuality attachImageGenerationQuality(Long imageModelQualityId) {
        if (imageModelQualityId == null) {
            return null;
        }
        return imageGenerationQualityRepository.findById(imageModelQualityId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "imageModelQualityId not found: " + imageModelQualityId));
    }

    private ImageGenerationSelection resolveImageGenerationSelection(Long imageModelId, Long imageModelQualityId) {
        if (imageModelId == null && imageModelQualityId == null) {
            return new ImageGenerationSelection(null, null);
        }
        ImageGenerationQuality quality = null;
        ImageGenerationModel model = null;
        if (imageModelQualityId != null) {
            quality = attachImageGenerationQuality(imageModelQualityId);
            model = quality.getModel();
            if (imageModelId != null && !model.getId().equals(imageModelId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "imageModelQualityId does not belong to imageModelId");
            }
        } else if (imageModelId != null) {
            model = attachImageGenerationModel(imageModelId);
        }
        if (model == null && quality != null) {
            model = quality.getModel();
        }
        return new ImageGenerationSelection(model, quality);
    }

    private record ImageGenerationSelection(ImageGenerationModel model, ImageGenerationQuality quality) { }

    /** Monta o nome automático do experimento com o código da hipótese e numeração sequencial. */
    private String buildAutomaticExperimentName(MarketNiche niche, com.marketinghub.hypothesis.Hypothesis hypothesis) {
        long nextNumber = repository.countByHypothesisRef(hypothesis) + 1;
        return "%s-E%03d".formatted(resolveHypothesisCode(niche, hypothesis), nextNumber);
    }

    /** Resolve o código da hipótese para compor o identificador do experimento. */
    private String resolveHypothesisCode(MarketNiche niche, com.marketinghub.hypothesis.Hypothesis hypothesis) {
        if (hypothesis != null && StringUtils.hasText(hypothesis.getTitle())) {
            return hypothesis.getTitle().trim();
        }
        return "%s-H000".formatted(nicheAcronym(niche));
    }

    /** Gera uma sigla estável a partir do nome do nicho para identificar experimentos. */
    private String nicheAcronym(MarketNiche niche) {
        if (niche == null || niche.getName() == null || niche.getName().isBlank()) {
            return "GER";
        }
        String normalized = Normalizer.normalize(niche.getName(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        StringBuilder acronym = new StringBuilder();
        for (String word : normalized.split("[^A-Z0-9]+")) {
            if (!word.isBlank()) {
                acronym.append(word.charAt(0));
            }
            if (acronym.length() == 4) {
                break;
            }
        }
        if (acronym.isEmpty()) {
            return "GER";
        }
        while (acronym.length() < 3) {
            acronym.append('X');
        }
        return acronym.toString();
    }

    /**
     * Cria e persiste um novo experimento com o contrato comercial inicial.
     */
    @Transactional
    public Experiment create(Long nicheId, CreateExperimentRequest request) {
        MarketNiche niche = attachNiche(nicheId);
        if (request.getHypothesisId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hypothesisId required");
        }
        com.marketinghub.hypothesis.Hypothesis hyp = attachHypothesis(request.getHypothesisId());
        if (!hyp.getMarketNiche().getId().equals(nicheId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hypothesis and experiment niche mismatch");
        }
        var resolvedProductAiSubtype = resolveProductAiSubtype(request.getProductAiSubtype(), hyp);
        validateProductAiPreparation(request.getHypothesisId(), resolvedProductAiSubtype);
        if (request.getStartDate() != null && request.getEndDate() != null &&
                request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }
        String automaticName = buildAutomaticExperimentName(niche, hyp);
        if (repository.existsByNicheAndName(niche, automaticName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "automatic name already exists for niche");
        }
        String normalizedPrimaryVariable = normalizeOptionalPrimaryDescriptor(request.getPrimaryVariable());
        String normalizedPrimaryMetric = normalizeOptionalPrimaryDescriptor(request.getPrimaryMetric());
        MetricPreset preset = request.getMetricPresetId() == null || request.getMetricPresetId().isBlank()
                ? null
                : metricPresetService.get(request.getMetricPresetId());
        java.math.BigDecimal computedStopLoss =
                (request.getKpiTargetCpl() != null && preset != null)
                        ? request.getKpiTargetCpl().multiply(preset.getStopLossFactor())
                        : null;
        if (request.getSampleSize() != null && request.getSampleSize() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize must be at least 1");
        }
        if (request.getDailyBudget() != null && request.getDailyBudget().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyBudget must be greater than zero");
        }
        if (request.getBaselineCvr() != null && request.getTargetCvr() != null &&
                request.getBaselineCvr().compareTo(request.getTargetCvr()) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baselineCvr must be < targetCvr");
        }
        if (request.getJourneyTemplateId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "journeyTemplateId required");
        }
        JourneyTemplate journeyTemplate = attachJourneyTemplate(request.getJourneyTemplateId());
        LeadPortalFlow leadPortalFlow = attachLeadPortalFlow(request.getLeadPortalFlowId(), niche.getId());
        String followUpActionUrl = normalizeFollowUpActionUrl(request.getFollowUpActionUrl());
        ImageGenerationSelection imageSelection =
                resolveImageGenerationSelection(request.getImageModelId(), request.getImageModelQualityId());
        java.math.BigDecimal unitPrice = normalizeUnitPrice(request.getUnitPrice());
        int imagesPerPackage = normalizeImagesPerPackage(request.getImagesPerPackage());
        Integer openImagesPerPackage =
                normalizeOptionalImagesPerPackage(request.getOpenImagesPerPackage(), "openImagesPerPackage");
        Integer compressedImagesPerPackage =
                normalizeOptionalImagesPerPackage(request.getCompressedImagesPerPackage(), "compressedImagesPerPackage");
        BigDecimal promiseGenerationCost = completedPromiseGenerationCostBrl(request.getPromiseGenerationRequestIds());
        BigDecimal initialTotalCost = addNullable(request.getCost(), promiseGenerationCost);
        Experiment exp = Experiment.builder()
                .niche(niche)
                .name(automaticName)
                .creationSource(request.getCreationSource() != null
                        ? request.getCreationSource()
                        : ExperimentCreationSource.SYSTEM_FLOW)
                .hypothesis(request.getHypothesis())
                .singlePain(normalizeExperimentPromiseField(request.getSinglePain()))
                .freeReward(normalizeExperimentPromiseField(request.getFreeReward()))
                .funnelPromise(normalizeExperimentPromiseField(request.getFunnelPromise()))
                .primaryCta(normalizeExperimentPromiseField(request.getPrimaryCta()))
                .experimentType(resolveExperimentType(request.getExperimentType()))
                .productAiSubtype(resolvedProductAiSubtype)
                .campaignObjective(resolveCampaignObjective(
                        request.getCampaignObjective(), request.getFreeReward(), request.getExperimentType()))
                .hypothesisRef(hyp)
                .kpiTargetCpl(request.getKpiTargetCpl())
                .metricPreset(preset)
                .stopLossCpl(computedStopLoss)
                .sampleSize(request.getSampleSize())
                .baselineCvr(request.getBaselineCvr())
                .targetCvr(request.getTargetCvr())
                .mdePercent(request.getMdePercent())
                .dailyBudget(request.getDailyBudget())
                .unitPrice(unitPrice)
                .cost(request.getCost())
                .totalCost(initialTotalCost)
                .expense(request.getExpense())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .stage(request.getStage() != null ? request.getStage() : ExperimentStage.AD)
                .primaryVariable(normalizedPrimaryVariable)
                .primaryMetric(normalizedPrimaryMetric)
                .creativesToGenerate(request.getCreativesToGenerate())
                .instantFormsToGenerate(request.getInstantFormsToGenerate())
                .emailsToGenerate(request.getEmailsToGenerate())
                .sampleEmailsToGenerate(request.getSampleEmailsToGenerate())
                .deliverablesToGenerate(request.getDeliverablesToGenerate())
                .leadPortalFlowsToGenerate(request.getLeadPortalFlowsToGenerate())
                .imagesPerPackage(imagesPerPackage)
                .openImagesPerPackage(openImagesPerPackage)
                .compressedImagesPerPackage(compressedImagesPerPackage)
                .facebookPage(attachFacebookPage(request.getFacebookPageId()))
                .facebookInstantForm(attachInstantForm(request.getFacebookInstantFormId(), request.getHypothesisId()))
                .instagramAccount(attachInstagramAccount(request.getInstagramAccountId()))
                .journeyTemplate(journeyTemplate)
                .leadPortalFlowModel(request.getLeadPortalFlowModel())
                .schemaFirstLeadPortalEnabled(Boolean.TRUE.equals(request.getSchemaFirstLeadPortalEnabled()))
                .leadPortalFlow(leadPortalFlow)
                .imageGenerationModel(imageSelection.model())
                .imageGenerationQuality(imageSelection.quality())
                .followUpActionUrl(followUpActionUrl)
                .creativeTextPrompt(normalizePrompt(request.getCreativeTextPrompt()))
                .creativeImagePrompt(normalizePrompt(request.getCreativeImagePrompt()))
                .build();
        Experiment savedExperiment = repository.save(exp);
        promptSchemaUsageService.linkHypothesisTemplates(savedExperiment.getId());
        dismissPromiseGenerationDrafts(request.getPromiseGenerationRequestIds());
        synchronizeLeadPortalFlow(savedExperiment);
        return savedExperiment;
    }

    /** Descarta rascunhos de promessa usados para criar o experimento e evita retomada de teste já salvo. */
    private void dismissPromiseGenerationDrafts(List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return;
        }
        promiseGenerationRequestRepository.dismissByIdIn(requestIds, Instant.now());
    }

    /** Soma em reais o custo das solicitações de promessa usadas para criar o experimento. */
    private BigDecimal completedPromiseGenerationCostBrl(List<Long> requestIds) {
        if (requestIds == null || requestIds.isEmpty()) {
            return null;
        }
        BigDecimal costUsd = promiseGenerationRequestRepository.sumCompletedCostUsdByIdIn(requestIds);
        return currencyConversionService.usdToBrl(costUsd);
    }

    /** Soma dois valores opcionais de custo preservando nulo quando não há custo. */
    private BigDecimal addNullable(BigDecimal first, BigDecimal second) {
        BigDecimal total = BigDecimal.ZERO;
        boolean hasValue = false;
        if (first != null) {
            total = total.add(first);
            hasValue = true;
        }
        if (second != null) {
            total = total.add(second);
            hasValue = true;
        }
        return hasValue ? total : null;
    }

    @Transactional
    public Experiment create(CreateExperimentRequest request) {
        if (request.getMarketNicheId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "marketNicheId required");
        }
        return create(request.getMarketNicheId(), request);
    }

    public Experiment get(Long id) {
        return repository.findById(id).orElseThrow();
    }

    public Iterable<Experiment> list() {
        return repository.findAll();
    }

    public Iterable<Experiment> listByNiche(Long nicheId) {
        return repository.findByNicheId(nicheId);
    }

    public java.util.List<Experiment> listByStatusAndPlatform(
            ExperimentStatus status, ExperimentPlatform platform) {
        return repository.findByStatusAndPlatform(status, platform);
    }

    public java.util.List<Experiment> listReadyForCampaign() {
        return repository.findReadyForCampaign(ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK)
                .stream()
                .filter(exp -> exp.getFacebookReleaseRequestedAt() != null)
                .toList();
    }

    /**
     * Duplica um experimento preservando seu contrato comercial e configurações operacionais.
     */
    @Transactional
    public Experiment duplicate(Long id) {
        Experiment original = repository.findById(id).orElseThrow();
        Experiment copy = Experiment.builder()
                .niche(original.getNiche())
                .name(original.getName() + " copy")
                .creationSource(original.getCreationSource())
                .hypothesis(original.getHypothesis())
                .singlePain(original.getSinglePain())
                .freeReward(original.getFreeReward())
                .funnelPromise(original.getFunnelPromise())
                .primaryCta(original.getPrimaryCta())
                .experimentType(original.getExperimentType())
                .productAiSubtype(original.getProductAiSubtype())
                .campaignObjective(original.getCampaignObjective())
                .hypothesisRef(original.getHypothesisRef())
                .kpiTargetCpl(original.getKpiTargetCpl())
                .metricPreset(original.getMetricPreset())
                .stopLossCpl(original.getStopLossCpl())
                .sampleSize(original.getSampleSize())
                .baselineCvr(original.getBaselineCvr())
                .targetCvr(original.getTargetCvr())
                .mdePercent(original.getMdePercent())
                .dailyBudget(original.getDailyBudget())
                .unitPrice(original.getUnitPrice())
                .cost(original.getCost())
                .totalCost(original.getTotalCost())
                .expense(original.getExpense())
                .startDate(original.getStartDate())
                .endDate(original.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .platform(original.getPlatform())
                .stage(original.getStage())
                .primaryVariable(original.getPrimaryVariable())
                .primaryMetric(original.getPrimaryMetric())
                .creativesToGenerate(original.getCreativesToGenerate())
                .instantFormsToGenerate(original.getInstantFormsToGenerate())
                .emailsToGenerate(original.getEmailsToGenerate())
                .sampleEmailsToGenerate(original.getSampleEmailsToGenerate())
                .deliverablesToGenerate(original.getDeliverablesToGenerate())
                .leadPortalFlowsToGenerate(original.getLeadPortalFlowsToGenerate())
                .imagesPerPackage(original.getImagesPerPackage())
                .openImagesPerPackage(original.getOpenImagesPerPackage())
                .compressedImagesPerPackage(original.getCompressedImagesPerPackage())
                .creativeTextPrompt(original.getCreativeTextPrompt())
                .creativeImagePrompt(original.getCreativeImagePrompt())
                .facebookPage(original.getFacebookPage())
                .instagramAccount(original.getInstagramAccount())
                .facebookInstantForm(original.getFacebookInstantForm())
                .journeyTemplate(original.getJourneyTemplate())
                .leadPortalFlowModel(original.getLeadPortalFlowModel())
                .schemaFirstLeadPortalEnabled(original.isSchemaFirstLeadPortalEnabled())
                .leadPortalFlow(original.getLeadPortalFlow())
                .imageGenerationModel(original.getImageGenerationModel())
                .imageGenerationQuality(original.getImageGenerationQuality())
                .followUpActionUrl(original.getFollowUpActionUrl())
                .build();
        return repository.save(copy);
    }

    /**
     * Atualiza o status do experimento e solicita pausa operacional das campanhas Meta quando aplicável.
     */
    @Transactional
    public Experiment updateStatus(Long id, ExperimentStatus status) {
        Experiment exp = repository.findById(id).orElseThrow();
        if (status == ExperimentStatus.RUNNING) {
            if (exp.getKpiTargetCpl() == null || exp.getStopLossCpl() == null || exp.getSampleSize() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "financial fields not set");
            }
            if (exp.getDailyBudget() == null || exp.getDailyBudget().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyBudget not set");
            }
        }
        exp.setStatus(status);
        if (status == ExperimentStatus.PAUSED) {
            experimentFunnelStandbyService.requestFacebookCampaignStops(
                    exp.getId(),
                    FacebookCampaignStopReason.ADMIN_EXPERIMENT_PAUSED,
                    "pausa administrativa do experimento"
            );
        }
        return exp;
    }

    /**
     * Atualiza os campos mutáveis do experimento preservando o contrato comercial quando ausente no payload.
     */
    @Transactional
    public Experiment update(Long id, UpdateExperimentRequest request) {
        Experiment exp = repository.findById(id).orElseThrow();

        if (request.getName() == null || request.getKpiTargetCpl() == null
                || request.getHypothesis() == null || request.getMetricPresetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "required fields missing");
        }

        if (!exp.getName().equals(request.getName()) &&
                repository.existsByNicheAndName(exp.getNiche(), request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name already exists for niche");
        }

        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }

        if (request.getSampleSize() != null && request.getSampleSize() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize must be at least 1");
        }

        MetricPreset preset = metricPresetService.get(request.getMetricPresetId());
        ExperimentStage resolvedStage = request.getStage() != null ? request.getStage() : exp.getStage();
        if (resolvedStage == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stage required");
        }
        String normalizedPrimaryVariable = resolvePrimaryDescriptor(
                request.getPrimaryVariable(), exp.getPrimaryVariable(), "primaryVariable");
        String normalizedPrimaryMetric = resolvePrimaryDescriptor(
                request.getPrimaryMetric(), exp.getPrimaryMetric(), "primaryMetric");

        exp.setName(request.getName());
        exp.setHypothesis(request.getHypothesis());
        if (request.isSinglePainPresent()) {
            exp.setSinglePain(normalizeExperimentPromiseField(request.getSinglePain()));
        }
        if (request.isFreeRewardPresent()) {
            exp.setFreeReward(normalizeExperimentPromiseField(request.getFreeReward()));
        }
        if (request.isFunnelPromisePresent()) {
            exp.setFunnelPromise(normalizeExperimentPromiseField(request.getFunnelPromise()));
        }
        if (request.isPrimaryCtaPresent()) {
            exp.setPrimaryCta(normalizeExperimentPromiseField(request.getPrimaryCta()));
        }
        if (request.isExperimentTypePresent()) {
            exp.setExperimentType(resolveExperimentType(request.getExperimentType()));
        }
        if (request.isProductAiSubtypePresent()) {
            exp.setProductAiSubtype(resolveProductAiSubtype(request.getProductAiSubtype(), exp.getHypothesisRef()));
        }
        if (request.isCampaignObjectivePresent()) {
            exp.setCampaignObjective(resolveCampaignObjective(
                    request.getCampaignObjective(),
                    request.isFreeRewardPresent() ? request.getFreeReward() : exp.getFreeReward(),
                    exp.getExperimentType()));
        } else if (request.isFreeRewardPresent()) {
            exp.setCampaignObjective(resolveCampaignObjective(
                    exp.getCampaignObjective(), request.getFreeReward(), exp.getExperimentType()));
        }
        exp.setKpiTargetCpl(request.getKpiTargetCpl());
        exp.setMetricPreset(preset);
        exp.setStage(resolvedStage);
        exp.setPrimaryVariable(normalizedPrimaryVariable);
        exp.setPrimaryMetric(normalizedPrimaryMetric);
        if (request.isDailyBudgetPresent()) {
            if (request.getDailyBudget() != null && request.getDailyBudget().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyBudget must be greater than zero");
            }
            exp.setDailyBudget(request.getDailyBudget());
        }
        if (request.isUnitPricePresent()) {
            exp.setUnitPrice(normalizeUnitPrice(request.getUnitPrice()));
        }
        if (request.isCostPresent()) {
            exp.setCost(request.getCost());
        }
        if (request.isExpensePresent()) {
            exp.setExpense(request.getExpense());
        }
        if (request.getSampleSize() != null) {
            exp.setSampleSize(request.getSampleSize());
        } else {
            exp.setSampleSize(preset.getSampleSize());
        }
        if (request.getMdePercent() != null) {
            exp.setMdePercent(request.getMdePercent());
        } else {
            exp.setMdePercent(preset.getDefaultMdePp());
        }
        if (request.getKpiTargetCpl() != null && preset.getStopLossFactor() != null) {
            exp.setStopLossCpl(request.getKpiTargetCpl().multiply(preset.getStopLossFactor()));
        }
        exp.setStartDate(request.getStartDate());
        exp.setEndDate(request.getEndDate());
        if (request.getCreativesToGenerate() != null) {
            exp.setCreativesToGenerate(request.getCreativesToGenerate());
        }
        if (request.getInstantFormsToGenerate() != null) {
            exp.setInstantFormsToGenerate(request.getInstantFormsToGenerate());
        }
        if (request.getEmailsToGenerate() != null) {
            exp.setEmailsToGenerate(request.getEmailsToGenerate());
        }
        if (request.getSampleEmailsToGenerate() != null) {
            exp.setSampleEmailsToGenerate(request.getSampleEmailsToGenerate());
        }
        if (request.getDeliverablesToGenerate() != null) {
            exp.setDeliverablesToGenerate(request.getDeliverablesToGenerate());
        }
        if (request.getLeadPortalFlowsToGenerate() != null) {
            exp.setLeadPortalFlowsToGenerate(request.getLeadPortalFlowsToGenerate());
        }
        if (request.getImagesPerPackage() != null) {
            exp.setImagesPerPackage(normalizeImagesPerPackage(request.getImagesPerPackage()));
        }
        if (request.isOpenImagesPerPackagePresent()) {
            exp.setOpenImagesPerPackage(
                    normalizeOptionalImagesPerPackage(
                            request.getOpenImagesPerPackage(), "openImagesPerPackage"));
        }
        if (request.isCompressedImagesPerPackagePresent()) {
            exp.setCompressedImagesPerPackage(
                    normalizeOptionalImagesPerPackage(
                            request.getCompressedImagesPerPackage(), "compressedImagesPerPackage"));
        }
        if (request.getCreativeApproved() != null) {
            exp.setCreativeApproved(request.getCreativeApproved());
        }
        if (request.isFollowUpActionUrlPresent()) {
            String followUpActionUrl = normalizeFollowUpActionUrl(request.getFollowUpActionUrl());
            exp.setFollowUpActionUrl(followUpActionUrl);
        }
        if (request.isLeadPortalFlowModelPresent()) {
            exp.setLeadPortalFlowModel(request.getLeadPortalFlowModel());
        }
        if (request.isSchemaFirstLeadPortalEnabledPresent()) {
            exp.setSchemaFirstLeadPortalEnabled(Boolean.TRUE.equals(request.getSchemaFirstLeadPortalEnabled()));
        }
        if (request.isCreativeTextPromptPresent()) {
            exp.setCreativeTextPrompt(normalizePrompt(request.getCreativeTextPrompt()));
        }
        if (request.isCreativeImagePromptPresent()) {
            exp.setCreativeImagePrompt(normalizePrompt(request.getCreativeImagePrompt()));
        }
        if (request.isJourneyTemplateIdPresent()) {
            if (request.getJourneyTemplateId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "journeyTemplateId required");
            }
            exp.setJourneyTemplate(attachJourneyTemplate(request.getJourneyTemplateId()));
        }
        if (request.isFacebookPageIdPresent()) {
            exp.setFacebookPage(attachFacebookPage(request.getFacebookPageId()));
        }
        if (request.isFacebookInstantFormIdPresent()) {
            exp.setFacebookInstantForm(attachInstantForm(request.getFacebookInstantFormId(), exp.getHypothesisRef().getId()));
        }
        if (request.isInstagramAccountIdPresent()) {
            exp.setInstagramAccount(attachInstagramAccount(request.getInstagramAccountId()));
        }
        if (request.isLeadPortalFlowIdPresent()) {
            if (request.getLeadPortalFlowId() == null) {
                exp.setLeadPortalFlow(null);
            } else {
                exp.setLeadPortalFlow(attachLeadPortalFlow(request.getLeadPortalFlowId(), exp.getNiche().getId()));
            }
        }
        if (request.isImageModelIdPresent() || request.isImageModelQualityIdPresent()) {
            Long currentModelId = request.isImageModelIdPresent()
                    ? request.getImageModelId()
                    : (exp.getImageGenerationModel() != null ? exp.getImageGenerationModel().getId() : null);
            Long currentQualityId = request.isImageModelQualityIdPresent()
                    ? request.getImageModelQualityId()
                    : (exp.getImageGenerationQuality() != null ? exp.getImageGenerationQuality().getId() : null);
            ImageGenerationSelection selection = resolveImageGenerationSelection(currentModelId, currentQualityId);
            exp.setImageGenerationModel(selection.model());
            exp.setImageGenerationQuality(selection.quality());
        }
        synchronizeLeadPortalFlow(exp);
        return exp;
    }

    /**
     * Solicita geração de novos criativos registrando estado operacional da fila.
     */
    @Transactional
    public Experiment requestCreatives(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setCreativesToGenerate(quantity);
        exp.setCreativeGenerationMode(CreativeGenerationMode.DEFAULT);
        exp.setCreativeGenerationStatus(CreativeGenerationStatus.REQUESTED);
        exp.setCreativeGenerationRequestedAt(java.time.Instant.now());
        exp.setCreativeGenerationStartedAt(null);
        exp.setCreativeGenerationFinishedAt(null);
        exp.setCreativeGenerationError(null);
        return exp;
    }

    /**
     * Enfileira a geração de criativos usando os textos e briefings já aprovados no pipeline do experimento.
     */
    @Transactional
    public Experiment requestPipelineCreatives(Long id) {
        Experiment exp = repository.findById(id).orElseThrow();
        ensurePipelinePrerequisites(exp);
        exp.setCreativesToGenerate(3);
        exp.setCreativeGenerationMode(CreativeGenerationMode.PIPELINE_ADS);
        exp.setCreativeGenerationStatus(CreativeGenerationStatus.REQUESTED);
        exp.setCreativeGenerationRequestedAt(Instant.now());
        exp.setCreativeGenerationStartedAt(null);
        exp.setCreativeGenerationFinishedAt(null);
        exp.setCreativeGenerationError(null);
        return exp;
    }

    /**
     * Verifica se o pipeline possui texto e briefing de imagem antes de enfileirar anúncios.
     */
    private void ensurePipelinePrerequisites(Experiment exp) {
        if (!StringUtils.hasText(exp.getAdCopy()) || !StringUtils.hasText(exp.getAdImageBriefing())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Conclua as etapas de Texto do Anúncio e Prompt da Imagem antes de gerar anúncios do pipeline.");
        }
    }

    @Transactional
    public Experiment requestInstantForms(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setInstantFormsToGenerate(quantity);
        return exp;
    }

    /**
     * Requests generation of new emails by setting the pending quantity.
     */
    @Transactional
    public Experiment requestEmails(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setEmailsToGenerate(quantity);
        return exp;
    }

    /**
     * Requests generation of new sample emails by setting the pending quantity.
     */
    @Transactional
    public Experiment requestSampleEmails(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setSampleEmailsToGenerate(quantity);
        return exp;
    }
    /**
     * Defines which generated sample email should be linked to the experiment.
     */
    @Transactional
    public Experiment updateSelectedSampleEmail(Long id, Long sampleEmailId) {
        Experiment exp = repository.findById(id).orElseThrow();
        if (sampleEmailId == null) {
            exp.setSelectedSampleEmail(null);
            return exp;
        }

        SampleEmail email = sampleEmailRepository
                .findById(sampleEmailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sample email not found"));

        if (email.getExperiment() == null || !email.getExperiment().getId().equals(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Sample email does not belong to experiment");
        }

        exp.setSelectedSampleEmail(email);
        return exp;
    }

    /**
     * Requests generation of new deliverable definitions by setting the pending quantity.
     */
    @Transactional
    public Experiment requestDeliverables(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setDeliverablesToGenerate(quantity);
        return exp;
    }

    /**
     * Requests generation of new lead portal flows by setting the pending quantity.
     */
    @Transactional
    public Experiment requestLeadPortalFlows(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setLeadPortalFlowsToGenerate(quantity);
        return exp;
    }

    /**
     * Libera o experimento para o Facebook Ads Worker e remove eventos dependentes antes de zerar o funil.
     */
    @Transactional
    public Experiment releaseForFacebook(Long id) {
        Experiment experiment = repository.findById(id).orElseThrow();
        if (experiment.getPlatform() != ExperimentPlatform.FACEBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Experiment platform must be Facebook");
        }
        ensureLowTicketSalesPageWasBuiltByPipeline(experiment);
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setFacebookReleaseRequestedAt(Instant.now());
        experiment.setFunnelResetAt(experiment.getFacebookReleaseRequestedAt());
        removePreviousFacebookPublication(id);
        experimentLandingAnalyticsEventRepository.deleteByExperimentId(id);
        experimentFunnelEventRepository.deleteByExperimentId(id);
        return experiment;
    }

    /** Bloqueia venda quando contrato, página, destino ou métricas não estão prontos para tráfego. */
    private void ensureLowTicketSalesPageWasBuiltByPipeline(Experiment experiment) {
        if (!requiresSalesPageBeforePurchase(experiment)) {
            return;
        }
        if (!hasCompleteCommercialContract(experiment)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Experimento com intenção de compra exige contrato comercial completo da etapa Oferta antes da campanha.");
        }
        boolean pipelineCompleted = geraSalesPageStageExecutionRepository
                .findTopByExperimentIdAndStageCodeOrderByExecutionRequestedAtDesc(
                        experiment.getId(), GeraSalesPageStageCode.PUBLICATION_PACKAGE.code())
                .map(com.marketinghub.gerasalespage.v1.GeraSalesPageStageExecution::getStatus)
                .map("CONCLUIDO"::equalsIgnoreCase)
                .orElse(false);
        if (!pipelineCompleted) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Experimento com intenção de compra exige página de venda criada pelo GeraSalesPage v1 antes da campanha.");
        }
        GeraSalesPagePublicationAudit publication = geraSalesPagePublicationAuditRepository
                .findTopByExperimentIdOrderByPublishedAtDesc(experiment.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Experimento com intenção de compra exige página de venda publicada e auditada pelo GeraSalesPage v1 antes da campanha."));
        if (!hasAdDestinationPointingToSalesPage(experiment, publication)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Experimento com intenção de compra exige que o link do anúncio aponte para a página de venda publicada, não para o checkout direto.");
        }
        if (!hasRequiredSalesPageAnalyticsCollectors(publication)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Experimento com intenção de compra exige página de venda com coletores page_view, page_load_metric, section_view_time e checkout_click antes da campanha.");
        }
    }

    /** Informa se o experimento precisa de página de venda antes do checkout. */
    private boolean requiresSalesPageBeforePurchase(Experiment experiment) {
        return experiment != null
                && (experiment.getExperimentType() == ExperimentType.LOW_TICKET_PRODUCT
                || experiment.getCampaignObjective() == ExperimentCampaignObjective.SALES);
    }

    /** Confirma se a etapa Oferta preencheu o contrato comercial mínimo de venda. */
    private boolean hasCompleteCommercialContract(Experiment experiment) {
        return experiment != null
                && StringUtils.hasText(experiment.getSinglePain())
                && StringUtils.hasText(experiment.getFreeReward())
                && StringUtils.hasText(experiment.getFunnelPromise())
                && StringUtils.hasText(experiment.getPrimaryCta())
                && experiment.getUnitPrice() != null
                && experiment.getUnitPrice().signum() > 0;
    }

    /** Confirma que o destino do anúncio é a página auditada e não o checkout. */
    private boolean hasAdDestinationPointingToSalesPage(
            Experiment experiment,
            GeraSalesPagePublicationAudit publication) {
        String destinationUrl = normalizeUrl(experiment.getFollowUpActionUrl());
        String salesPageUrl = normalizeUrl(publication.getSalesPageUrl());
        String checkoutUrl = normalizeUrl(publication.getCheckoutUrl());
        return StringUtils.hasText(destinationUrl)
                && StringUtils.hasText(salesPageUrl)
                && destinationUrl.equals(salesPageUrl)
                && (!StringUtils.hasText(checkoutUrl) || !destinationUrl.equals(checkoutUrl));
    }

    /** Confirma que o HTML publicado possui os coletores mínimos para ler o funil de venda. */
    private boolean hasRequiredSalesPageAnalyticsCollectors(GeraSalesPagePublicationAudit publication) {
        if (publication == null || !StringUtils.hasText(publication.getHtml())) {
            return false;
        }
        return GeraSalesPageAnalyticsContract.hasRequiredCollectors(publication.getHtml());
    }

    /** Normaliza URL para comparação de destino sem depender de barra final. */
    private String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Remove a publicação anterior do Facebook para permitir que o worker publique um novo ciclo.
     */
    private void removePreviousFacebookPublication(Long experimentId) {
        List<String> campaignIds = facebookAdsCampaignRepository.findByExperimentId(experimentId)
                .stream()
                .map(campaign -> campaign.getId())
                .toList();
        if (campaignIds.isEmpty()) {
            return;
        }
        facebookAdsAdRepository.deleteByAdSetCampaignIdIn(campaignIds);
        facebookAdsAdSetRepository.deleteByCampaignIdIn(campaignIds);
        facebookAdsCampaignRepository.deleteByExperimentId(experimentId);
    }

    private void synchronizeLeadPortalFlow(Experiment experiment) {
        LeadPortalFlow flowRef = experiment.getLeadPortalFlow();
        if (flowRef == null || flowRef.getId() == null) {
            return;
        }
        LeadPortalFlow flow = leadPortalFlowRepository.findById(flowRef.getId()).orElse(null);
        if (flow == null) {
            return;
        }
        String desiredModel = resolveImagePromptModel(experiment);
        Integer desiredBatchSize = experiment.getImagesPerPackage();
        boolean changed = false;
        if (!Objects.equals(flow.getImagePromptModel(), desiredModel)) {
            flow.setImagePromptModel(desiredModel);
            changed = true;
        }
        if (!Objects.equals(flow.getImagePromptBatchSize(), desiredBatchSize)) {
            flow.setImagePromptBatchSize(desiredBatchSize);
            changed = true;
        }
        if (changed) {
            LeadPortalFlow savedFlow = leadPortalFlowRepository.save(flow);
            publishFlowIfNeeded(savedFlow);
        }
    }

    private String resolveImagePromptModel(Experiment experiment) {
        ImageGenerationModel model = experiment.getImageGenerationModel();
        if (model != null && StringUtils.hasText(model.getApiModel())) {
            return model.getApiModel();
        }
        ImageGenerationQuality quality = experiment.getImageGenerationQuality();
        if (quality != null && quality.getModel() != null && StringUtils.hasText(quality.getModel().getApiModel())) {
            return quality.getModel().getApiModel();
        }
        return null;
    }

    private void publishFlowIfNeeded(LeadPortalFlow flow) {
        if (!flow.isApproved()) {
            return;
        }
        try {
            leadPortalFlowPublisher.publish(flow);
        } catch (LeadPortalPublicationException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "failed to synchronise lead portal flow", ex);
        }
    }

    private java.math.BigDecimal normalizeUnitPrice(java.math.BigDecimal unitPrice) {
        if (unitPrice == null) {
            return null;
        }
        if (unitPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unitPrice must be greater than zero");
        }
        return unitPrice.setScale(2, java.math.RoundingMode.HALF_UP);
    }


    private String normalizeOptionalPrimaryDescriptor(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizePrimaryDescriptor(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " required");
        }
        return value.trim();
    }

    private String resolvePrimaryDescriptor(String requestedValue, String currentValue, String fieldName) {
        if (StringUtils.hasText(requestedValue)) {
            return normalizePrimaryDescriptor(requestedValue, fieldName);
        }
        if (StringUtils.hasText(currentValue)) {
            return normalizePrimaryDescriptor(currentValue, fieldName);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " required");
    }

    private String normalizePrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return null;
        }
        return prompt.trim();
    }

    /**
     * Normaliza campos textuais da promessa única do experimento.
     */
    private String normalizeExperimentPromiseField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * Resolve o tipo comercial do experimento mantendo compatibilidade com testes antigos.
     */
    private ExperimentType resolveExperimentType(ExperimentType requestedType) {
        return requestedType != null ? requestedType : ExperimentType.NICHE_TEST;
    }

    /**
     * Resolve o subtipo de Produto IA priorizando o experimento e herdando da hipótese quando ausente.
     */
    private com.marketinghub.productai.ProductAiSubtype resolveProductAiSubtype(
            com.marketinghub.productai.ProductAiSubtype requestedSubtype,
            com.marketinghub.hypothesis.Hypothesis hypothesis) {
        if (requestedSubtype != null) {
            return requestedSubtype;
        }
        return hypothesis != null ? hypothesis.getProductAiSubtype() : null;
    }

    /**
     * Impede que Produto IA avance para experimento sem preparo sistêmico da hipótese.
     */
    private void validateProductAiPreparation(
            java.util.UUID hypothesisId,
            com.marketinghub.productai.ProductAiSubtype productAiSubtype) {
        if (productAiSubtype == null) {
            return;
        }
        productAiExperimentPreparationService.assertReadyForExperiment(hypothesisId, productAiSubtype);
    }

    /**
     * Resolve o objetivo de campanha conforme o tipo comercial do experimento.
     */
    private ExperimentCampaignObjective resolveCampaignObjective(
            ExperimentCampaignObjective requestedObjective, String freeReward, ExperimentType experimentType) {
        ExperimentType resolvedType = resolveExperimentType(experimentType);
        ExperimentCampaignObjective objective = requestedObjective != null
                ? requestedObjective
                : resolvedType == ExperimentType.LOW_TICKET_PRODUCT
                        ? ExperimentCampaignObjective.SALES
                        : ExperimentCampaignObjective.LEADS;
        if (resolvedType == ExperimentType.NICHE_TEST
                && StringUtils.hasText(freeReward)
                && objective != ExperimentCampaignObjective.LEADS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "campaignObjective must be LEADS when freeReward is present");
        }
        return objective;
    }

    /**
     * Lista experimentos com geração de criativos pendente para consumo operacional do AI Worker.
     */
    @Transactional(readOnly = true)
    public List<Experiment> listPendingCreativeGeneration(int limit) {
        int effectiveLimit = Math.max(1, limit);
        return repository.findAllToGenerateCreatives().stream()
                .filter(experiment -> experiment.getCreativeGenerationStatus() == CreativeGenerationStatus.REQUESTED
                        || experiment.getCreativeGenerationStatus() == CreativeGenerationStatus.PROCESSING)
                .limit(effectiveLimit)
                .toList();
    }

    /**
     * Marca a solicitação de criativos como em processamento pelo worker.
     */
    @Transactional
    public Experiment markCreativeGenerationStarted(Long id) {
        Experiment exp = repository.findById(id).orElseThrow();
        Integer pending = exp.getCreativesToGenerate();
        if (pending == null || pending <= 0) {
            return exp;
        }
        exp.setCreativeGenerationStatus(CreativeGenerationStatus.PROCESSING);
        if (exp.getCreativeGenerationStartedAt() == null) {
            exp.setCreativeGenerationStartedAt(Instant.now());
        }
        exp.setCreativeGenerationError(null);
        return exp;
    }

    /**
     * Marca a solicitação de criativos como concluída e limpa a pendência da tela.
     */
    @Transactional
    public Experiment markCreativeGenerationCompleted(Long id) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setCreativesToGenerate(0);
        exp.setCreativeGenerationStatus(CreativeGenerationStatus.COMPLETED);
        exp.setCreativeGenerationFinishedAt(Instant.now());
        exp.setCreativeGenerationError(null);
        return exp;
    }

    /**
     * Marca a solicitação de criativos como falha para destravar nova tentativa consciente.
     */
    @Transactional
    public Experiment markCreativeGenerationFailed(Long id, String error) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setCreativesToGenerate(0);
        exp.setCreativeGenerationStatus(CreativeGenerationStatus.FAILED);
        exp.setCreativeGenerationFinishedAt(Instant.now());
        exp.setCreativeGenerationError(StringUtils.hasText(error) ? error.trim() : "Falha ao gerar criativos");
        return exp;
    }

    private int normalizeImagesPerPackage(Integer imagesPerPackage) {
        if (imagesPerPackage == null) {
            return 20;
        }
        if (imagesPerPackage <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imagesPerPackage must be greater than zero");
        }
        return imagesPerPackage;
    }

    private Integer normalizeOptionalImagesPerPackage(Integer imagesPerPackage, String fieldName) {
        if (imagesPerPackage == null) {
            return null;
        }
        if (imagesPerPackage <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, fieldName + " must be greater than zero");
        }
        return imagesPerPackage;
    }

    private String normalizeFollowUpActionUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "followUpActionUrl must start with http:// or https://");
        }
        if (trimmed.length() > 512) {
            return trimmed.substring(0, 512);
        }
        return trimmed;
    }

}
