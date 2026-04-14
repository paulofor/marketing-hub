package com.marketinghub.experiment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookInstantFormRepository;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.FacebookPageRepository;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.ads.InstagramAccountRepository;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.pipeline.ads.ExperimentPipelineAdExtractor;
import com.marketinghub.experiment.pipeline.ads.PipelineAdCreativePlan;
import com.marketinghub.experiment.repository.ExperimentRepository;
import com.marketinghub.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.journey.repository.JourneyTemplateRepository;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.leadportal.repository.LeadPortalFlowRepository;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.niche.repository.MarketNicheRepository;
import com.marketinghub.imagegeneration.ImageGenerationModel;
import com.marketinghub.imagegeneration.ImageGenerationQuality;
import com.marketinghub.imagegeneration.repository.ImageGenerationModelRepository;
import com.marketinghub.imagegeneration.repository.ImageGenerationQualityRepository;
import com.marketinghub.sampleemail.SampleEmail;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Objects;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for experiments.
 */
@Service
public class ExperimentService {
    private final ExperimentRepository repository;
    private final MarketNicheRepository nicheRepository;
    private final com.marketinghub.hypothesis.repository.HypothesisRepository hypothesisRepository;
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
    private final com.marketinghub.sampleemail.repository.SampleEmailRepository sampleEmailRepository;
    private final ExperimentFunnelEventRepository experimentFunnelEventRepository;
    private final ObjectMapper objectMapper;

    public ExperimentService(ExperimentRepository repository, MarketNicheRepository nicheRepository,
                             com.marketinghub.hypothesis.repository.HypothesisRepository hypothesisRepository,
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
                             com.marketinghub.sampleemail.repository.SampleEmailRepository sampleEmailRepository,
                             ExperimentFunnelEventRepository experimentFunnelEventRepository, ObjectMapper objectMapper) {
        this.repository = repository;
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
        this.objectMapper = objectMapper;
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

    /**
     * Creates and stores a new experiment.
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
        if (request.getStartDate() != null && request.getEndDate() != null &&
                request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
        }
        if (repository.existsByNicheAndName(niche, request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name already exists for niche");
        }
        if (request.getKpiTargetCpl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kpiTargetCpl required");
        }
        if (request.getMetricPresetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "metricPresetId required");
        }
        if (request.getStage() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stage required");
        }
        String normalizedPrimaryVariable = normalizePrimaryDescriptor(request.getPrimaryVariable(), "primaryVariable");
        String normalizedPrimaryMetric = normalizePrimaryDescriptor(request.getPrimaryMetric(), "primaryMetric");
        if (request.getJourneyTemplateId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "journeyTemplateId required");
        }
        MetricPreset preset = metricPresetService.get(request.getMetricPresetId());
        java.math.BigDecimal computedStopLoss = request.getKpiTargetCpl().multiply(preset.getStopLossFactor());
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
        Experiment exp = Experiment.builder()
                .niche(niche)
                .name(request.getName())
                .hypothesis(request.getHypothesis())
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
                .totalCost(request.getCost())
                .expense(request.getExpense())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(ExperimentStatus.PLANNED)
                .platform(ExperimentPlatform.FACEBOOK)
                .stage(request.getStage())
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
        synchronizeLeadPortalFlow(savedExperiment);
        return savedExperiment;
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

    public java.util.List<Experiment> listReadyForPixel() {
        return repository.findReadyForPixel(ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK);
    }

    @Transactional
    public Experiment duplicate(Long id) {
        Experiment original = repository.findById(id).orElseThrow();
        Experiment copy = Experiment.builder()
                .niche(original.getNiche())
                .name(original.getName() + " copy")
                .hypothesis(original.getHypothesis())
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

    @Transactional
    public Experiment attachFacebookPixel(Long id, String pixelId, String pixelCode, java.time.Instant createdAt) {
        if (pixelId == null || pixelId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pixelId is required");
        }
        Experiment exp = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found: " + id));
        exp.setFacebookPixelId(pixelId.trim());
        exp.setFacebookPixelCode(pixelCode);
        exp.setFacebookPixelCreatedAt(createdAt != null ? createdAt : java.time.Instant.now());
        return exp;
    }


    /**
     * Updates the status of an experiment.
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
        return exp;
    }

    /**
     * Updates mutable fields of an experiment.
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "leadPortalFlowId required");
            }
            exp.setLeadPortalFlow(attachLeadPortalFlow(request.getLeadPortalFlowId(), exp.getNiche().getId()));
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
     * Requests generation of new creatives by setting the pending quantity.
     */
    @Transactional
    public Experiment requestCreatives(Long id, int quantity) {
        Experiment exp = repository.findById(id).orElseThrow();
        exp.setCreativesToGenerate(quantity);
        return exp;
    }

    @Transactional
    public Experiment requestPipelineCreatives(Long id) {
        Experiment exp = repository.findById(id).orElseThrow();
        Integer pending = exp.getCreativesToGenerate();
        if (pending != null && pending > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma solicitação de criativos pendente");
        }
        ensurePipelinePrerequisites(exp);
        ExperimentPipelineAdExtractor extractor = new ExperimentPipelineAdExtractor(objectMapper);
        java.util.List<PipelineAdCreativePlan> plans = extractor.extract(exp);
        if (plans.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nenhuma variação válida foi encontrada no pipeline de anúncio.");
        }
        int quantity = Math.min(3, plans.size());
        exp.setCreativesToGenerate(quantity);
        exp.setCreativeGenerationMode(CreativeGenerationMode.PIPELINE_ADS);
        return exp;
    }

    /**
     * Requests generation of new instant forms by setting the pending quantity.
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
     * Marks the experiment as released to the Facebook Ads Worker and resets the funnel.
     */
    @Transactional
    public Experiment releaseForFacebook(Long id) {
        Experiment experiment = repository.findById(id).orElseThrow();
        if (experiment.getPlatform() != ExperimentPlatform.FACEBOOK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Experiment platform must be Facebook");
        }
        experiment.setStatus(ExperimentStatus.PLANNED);
        experiment.setFacebookReleaseRequestedAt(Instant.now());
        experimentFunnelEventRepository.deleteByExperimentId(id);
        return experiment;
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
