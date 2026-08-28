package com.marketinghub.experiment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.ads.FacebookInstantForm;
import com.marketinghub.ads.FacebookPage;
import com.marketinghub.ads.InstagramAccount;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.dto.CreateExperimentRequest;
import com.marketinghub.experiment.dto.ReactivateExperimentRequest;
import com.marketinghub.experiment.dto.UpdateExperimentRequest;
import com.marketinghub.experiment.funnel.ExperimentFunnelStandbyService;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.finance.CurrencyConversionService;
import com.marketinghub.gerasalespage.v1.GeraSalesPageAnalyticsContract;
import com.marketinghub.gerasalespage.v1.GeraSalesPagePublicationAudit;
import com.marketinghub.gerasalespage.v1.GeraSalesPageStageCode;
import com.marketinghub.imagegeneration.ImageGenerationModel;
import com.marketinghub.imagegeneration.ImageGenerationQuality;
import com.marketinghub.journey.model.JourneyTemplate;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.integration.LeadPortalFlowPublisher;
import com.marketinghub.leadportal.integration.LeadPortalPublicationException;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.productai.service.ProductAiExperimentPreparationService;
import com.marketinghub.repository.jpa.ads.FacebookInstantFormRepository;
import com.marketinghub.repository.jpa.ads.FacebookPageRepository;
import com.marketinghub.repository.jpa.ads.InstagramAccountRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentPromiseGenerationRequestRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentStatusChangeRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPagePublicationAuditRepository;
import com.marketinghub.repository.jpa.gerasalespage.v1.GeraSalesPageStageExecutionRepository;
import com.marketinghub.repository.jpa.imagegeneration.ImageGenerationModelRepository;
import com.marketinghub.repository.jpa.imagegeneration.ImageGenerationQualityRepository;
import com.marketinghub.repository.jpa.journey.JourneyTemplateRepository;
import com.marketinghub.repository.jpa.leadportal.LeadPortalFlowRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.sampleemail.SampleEmail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Orquestra as regras de negócio dos experimentos. */
@Service
public class ExperimentService {
  private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);
  private final ExperimentRepository repository;
  private final ExperimentStatusChangeRepository statusChangeRepository;
  private final ExperimentPromiseGenerationRequestRepository promiseGenerationRequestRepository;
  private final MarketNicheRepository nicheRepository;
  private final com.marketinghub.repository.jpa.hypothesis.HypothesisRepository
      hypothesisRepository;
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
  private final com.marketinghub.repository.jpa.sampleemail.SampleEmailRepository
      sampleEmailRepository;
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
  private final PdeProductionSlotRepository pdeProductionSlotRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final CommercialPlanVisualAssetRepository commercialPlanVisualAssetRepository;
  private final ExperimentDirectPdeActivationService directPdeActivationService;
  private static final String VALIDATION_OK = "OK";

  /** Inicializa o serviço com repositórios, integrações e validadores usados pelos experimentos. */
  public ExperimentService(
      ExperimentRepository repository,
      ExperimentStatusChangeRepository statusChangeRepository,
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
      ExperimentFunnelStandbyService experimentFunnelStandbyService,
      PdeProductionSlotRepository pdeProductionSlotRepository,
      CommercialPlanRepository commercialPlanRepository,
      CommercialPlanVisualAssetRepository commercialPlanVisualAssetRepository,
      ExperimentDirectPdeActivationService directPdeActivationService) {
    this.repository = repository;
    this.statusChangeRepository = statusChangeRepository;
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
    this.pdeProductionSlotRepository = pdeProductionSlotRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.commercialPlanVisualAssetRepository = commercialPlanVisualAssetRepository;
    this.directPdeActivationService = directPdeActivationService;
  }

  /**
   * Obtém uma referência gerenciada ao nicho sem consultar o banco imediatamente. getReference()
   * avoids {@code detached entity passed to persist} by associating the proxy with the current
   * persistence context.
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
    FacebookInstantForm form =
        facebookInstantFormRepository
            .findById(instantFormId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("FacebookInstantForm not found: " + instantFormId));
    if (hypothesisId != null
        && form.getHypothesis() != null
        && !form.getHypothesis().getId().equals(hypothesisId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "instant form does not belong to hypothesis");
    }
    return entityManager.getReference(FacebookInstantForm.class, instantFormId);
  }

  private JourneyTemplate attachJourneyTemplate(Long journeyTemplateId) {
    if (journeyTemplateId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "journeyTemplateId required");
    }
    if (!journeyTemplateRepository.existsById(journeyTemplateId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "journeyTemplateId not found: " + journeyTemplateId);
    }
    return entityManager.getReference(JourneyTemplate.class, journeyTemplateId);
  }

  private LeadPortalFlow attachLeadPortalFlow(Long flowId, Long nicheId) {
    if (flowId == null) {
      return null;
    }
    LeadPortalFlow flow =
        leadPortalFlowRepository
            .findById(flowId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "leadPortalFlowId not found: " + flowId));
    if (flow.getMarketNiche() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "leadPortalFlowId is not associated with a market niche");
    }
    if (nicheId != null && !flow.getMarketNiche().getId().equals(nicheId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "leadPortalFlowId must belong to the experiment's niche");
    }
    return entityManager.getReference(LeadPortalFlow.class, flowId);
  }

  /** Vincula somente um modelo visual ativo e homologado a um experimento. */
  private ImageGenerationModel attachImageGenerationModel(Long imageModelId) {
    if (imageModelId == null) {
      return null;
    }
    ImageGenerationModel model =
        imageGenerationModelRepository
            .findById(imageModelId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "imageModelId not found: " + imageModelId));
    if (model.getApiModel() != null
        && model.getApiModel().toLowerCase(java.util.Locale.ROOT).startsWith("gpt-image-1")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Modelos GPT Image 1 foram desativados; selecione gpt-image-2");
    }
    return model;
  }

  private ImageGenerationQuality attachImageGenerationQuality(Long imageModelQualityId) {
    if (imageModelQualityId == null) {
      return null;
    }
    return imageGenerationQualityRepository
        .findById(imageModelQualityId)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "imageModelQualityId not found: " + imageModelQualityId));
  }

  private ImageGenerationSelection resolveImageGenerationSelection(
      Long imageModelId, Long imageModelQualityId) {
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
            HttpStatus.BAD_REQUEST, "imageModelQualityId does not belong to imageModelId");
      }
    } else if (imageModelId != null) {
      model = attachImageGenerationModel(imageModelId);
    }
    if (model == null && quality != null) {
      model = quality.getModel();
    }
    return new ImageGenerationSelection(model, quality);
  }

  private record ImageGenerationSelection(
      ImageGenerationModel model, ImageGenerationQuality quality) {}

  /** Monta o nome automático do experimento com o código da hipótese e numeração sequencial. */
  private String buildAutomaticExperimentName(
      MarketNiche niche, com.marketinghub.hypothesis.Hypothesis hypothesis) {
    long nextNumber = repository.countByHypothesisRef(hypothesis) + 1;
    return "%s-E%03d".formatted(resolveHypothesisCode(niche, hypothesis), nextNumber);
  }

  /** Resolve o código da hipótese para compor o identificador do experimento. */
  private String resolveHypothesisCode(
      MarketNiche niche, com.marketinghub.hypothesis.Hypothesis hypothesis) {
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
    String normalized =
        Normalizer.normalize(niche.getName(), Normalizer.Form.NFD)
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

  /** Localiza o produto e impede mistura entre nichos no contrato do experimento. */
  private com.marketinghub.product.Product resolveExperimentProduct(
      Long productId, MarketNiche niche) {
    if (productId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId required");
    }
    com.marketinghub.product.Product product =
        entityManager.find(com.marketinghub.product.Product.class, productId);
    if (product == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado");
    }
    if (product.getMarketNiche() != null
        && !product.getMarketNiche().getId().equals(niche.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O produto selecionado não pertence ao nicho do experimento");
    }
    return product;
  }

  /** Valida o território no mapa atual e devolve um snapshot imutável para o experimento. */
  private String resolveDesireTerritorySnapshot(
      com.marketinghub.product.Product product, String territoryCode) {
    if (!StringUtils.hasText(territoryCode)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Selecione um território do Mapa de Desejo");
    }
    if (!StringUtils.hasText(product.getDesireAssociationMapJson())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "O produto selecionado não possui Mapa de Desejo");
    }
    try {
      JsonNode territories =
          objectMapper.readTree(product.getDesireAssociationMapJson()).path("territories");
      for (JsonNode territory : territories) {
        if (territoryCode.trim().equals(territory.path("code").asText())) {
          return objectMapper.writeValueAsString(territory);
        }
      }
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao validar Mapa de Desejo na criação do experimento. productId={}, territoryCode={}",
          product.getId(),
          territoryCode,
          ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Mapa de Desejo do produto inválido", ex);
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Território não pertence ao produto selecionado");
  }

  /** Cria e persiste um novo experimento com o contrato comercial inicial. */
  @Transactional
  public Experiment create(Long nicheId, CreateExperimentRequest request) {
    MarketNiche niche = attachNiche(nicheId);
    com.marketinghub.product.Product product =
        resolveExperimentProduct(request.getProductId(), niche);
    String territorySnapshot =
        resolveDesireTerritorySnapshot(product, request.getDesireTerritoryCode());
    if (request.getHypothesisId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hypothesisId required");
    }
    com.marketinghub.hypothesis.Hypothesis hyp = attachHypothesis(request.getHypothesisId());
    if (!hyp.getMarketNiche().getId().equals(nicheId)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "hypothesis and experiment niche mismatch");
    }
    if (hyp.getProduct() == null || !hyp.getProduct().getId().equals(product.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A hipótese selecionada não pertence ao produto do experimento");
    }
    ExperimentType resolvedExperimentType = resolveExperimentType(request.getExperimentType());
    ExperimentPlatform resolvedPlatform = resolveExperimentPlatform(request.getPlatform());
    validateBudgetForPlatform(resolvedPlatform, request.getDailyBudget());
    var resolvedProductAiSubtype =
        resolvedExperimentType == ExperimentType.FAKE_EXPERIMENT
            ? null
            : resolveProductAiSubtype(request.getProductAiSubtype(), hyp);
    validateProductAiPreparation(request.getHypothesisId(), resolvedProductAiSubtype);
    if (request.getStartDate() != null
        && request.getEndDate() != null
        && request.getStartDate().isAfter(request.getEndDate())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
    }
    String automaticName = buildAutomaticExperimentName(niche, hyp);
    if (repository.existsByNicheAndName(niche, automaticName)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "automatic name already exists for niche");
    }
    String normalizedPrimaryVariable =
        normalizeOptionalPrimaryDescriptor(request.getPrimaryVariable());
    String normalizedPrimaryMetric = normalizeOptionalPrimaryDescriptor(request.getPrimaryMetric());
    MetricPreset preset =
        request.getMetricPresetId() == null || request.getMetricPresetId().isBlank()
            ? null
            : metricPresetService.get(request.getMetricPresetId());
    java.math.BigDecimal computedStopLoss =
        (request.getKpiTargetCpl() != null && preset != null)
            ? request.getKpiTargetCpl().multiply(preset.getStopLossFactor())
            : null;
    if (request.getSampleSize() != null && request.getSampleSize() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize must be at least 1");
    }
    if (request.getDailyBudget() != null
        && request.getDailyBudget().compareTo(java.math.BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "dailyBudget must be greater than zero");
    }
    if (request.getBaselineCvr() != null
        && request.getTargetCvr() != null
        && request.getBaselineCvr().compareTo(request.getTargetCvr()) >= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baselineCvr must be < targetCvr");
    }
    if (request.getJourneyTemplateId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "journeyTemplateId required");
    }
    JourneyTemplate journeyTemplate = attachJourneyTemplate(request.getJourneyTemplateId());
    LeadPortalFlow leadPortalFlow =
        attachLeadPortalFlow(request.getLeadPortalFlowId(), niche.getId());
    String followUpActionUrl = normalizeFollowUpActionUrl(request.getFollowUpActionUrl());
    ImageGenerationSelection imageSelection =
        resolveImageGenerationSelection(
            request.getImageModelId(), request.getImageModelQualityId());
    java.math.BigDecimal unitPrice = normalizeUnitPrice(request.getUnitPrice());
    int imagesPerPackage = normalizeImagesPerPackage(request.getImagesPerPackage());
    Integer openImagesPerPackage =
        normalizeOptionalImagesPerPackage(
            request.getOpenImagesPerPackage(), "openImagesPerPackage");
    Integer compressedImagesPerPackage =
        normalizeOptionalImagesPerPackage(
            request.getCompressedImagesPerPackage(), "compressedImagesPerPackage");
    BigDecimal promiseGenerationCost =
        completedPromiseGenerationCostBrl(request.getPromiseGenerationRequestIds());
    BigDecimal initialTotalCost = addNullable(request.getCost(), promiseGenerationCost);
    Experiment exp =
        Experiment.builder()
            .niche(niche)
            .product(product)
            .desireTerritoryCode(request.getDesireTerritoryCode().trim())
            .desireTerritorySnapshotJson(territorySnapshot)
            .name(automaticName)
            .creationSource(
                request.getCreationSource() != null
                    ? request.getCreationSource()
                    : ExperimentCreationSource.SYSTEM_FLOW)
            .hypothesis(request.getHypothesis())
            .singlePain(normalizeExperimentPromiseField(request.getSinglePain()))
            .freeReward(normalizeExperimentPromiseField(request.getFreeReward()))
            .funnelPromise(normalizeExperimentPromiseField(request.getFunnelPromise()))
            .primaryCta(normalizeExperimentPromiseField(request.getPrimaryCta()))
            .experimentType(resolvedExperimentType)
            .productAiSubtype(resolvedProductAiSubtype)
            .campaignObjective(
                resolveCampaignObjective(
                    request.getCampaignObjective(),
                    request.getFreeReward(),
                    resolvedExperimentType))
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
            .platform(resolvedPlatform)
            .stage(request.getStage() != null ? request.getStage() : ExperimentStage.AD)
            .primaryVariable(normalizedPrimaryVariable)
            .primaryMetric(normalizedPrimaryMetric)
            .commercialObjective(normalizeLongText(request.getCommercialObjective()))
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
            .facebookInstantForm(
                attachInstantForm(request.getFacebookInstantFormId(), request.getHypothesisId()))
            .instagramAccount(attachInstagramAccount(request.getInstagramAccountId()))
            .journeyTemplate(journeyTemplate)
            .leadPortalFlowModel(request.getLeadPortalFlowModel())
            .schemaFirstLeadPortalEnabled(
                Boolean.TRUE.equals(request.getSchemaFirstLeadPortalEnabled()))
            .leadPortalFlow(leadPortalFlow)
            .imageGenerationModel(imageSelection.model())
            .imageGenerationQuality(imageSelection.quality())
            .followUpActionUrl(followUpActionUrl)
            .commercialCheckoutUrl(null)
            .creativeTextPrompt(normalizePrompt(request.getCreativeTextPrompt()))
            .creativeImagePrompt(normalizePrompt(request.getCreativeImagePrompt()))
            .build();
    Experiment savedExperiment = repository.save(exp);
    promptSchemaUsageService.linkHypothesisTemplates(savedExperiment.getId());
    dismissPromiseGenerationDrafts(request.getPromiseGenerationRequestIds());
    synchronizeLeadPortalFlow(savedExperiment);
    return savedExperiment;
  }

  /**
   * Descarta rascunhos de promessa usados para criar o experimento e evita retomada de teste já
   * salvo.
   */
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

  /** Busca um experimento e sinaliza explicitamente quando o identificador não existe. */
  public Experiment get(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Experimento não encontrado: " + id));
  }

  public Iterable<Experiment> list() {
    return repository.findAll();
  }

  /** Lista somente a página administrativa solicitada, aplicando filtros no banco. */
  public org.springframework.data.domain.Page<Experiment> listAdministrativePage(
      int page, int size, ExperimentStatus status, Long nicheId, String search) {
    java.util.List<ExperimentStatus> finalStatuses =
        java.util.List.of(
            ExperimentStatus.FINISHED,
            ExperimentStatus.VALIDATED,
            ExperimentStatus.INVALIDATED,
            ExperimentStatus.INCONCLUSIVE,
            ExperimentStatus.FAILED);
    return repository.findAdministrativePage(
        finalStatuses,
        status,
        nicheId,
        search == null ? "" : search.trim(),
        org.springframework.data.domain.PageRequest.of(
            Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
  }

  public Iterable<Experiment> listByNiche(Long nicheId) {
    return repository.findByNicheId(nicheId);
  }

  public java.util.List<Experiment> listByStatusAndPlatform(
      ExperimentStatus status, ExperimentPlatform platform) {
    return repository.findByStatusAndPlatform(status, platform);
  }

  /** Zera os custos manuais e legados do experimento preservando custos auditáveis externos. */
  @Transactional
  public Experiment resetCosts(Long id) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setCost(BigDecimal.ZERO);
    exp.setExpense(BigDecimal.ZERO);
    exp.setTotalCost(BigDecimal.ZERO);
    return exp;
  }

  public java.util.List<Experiment> listReadyForCampaign() {
    return repository
        .findReadyForCampaign(ExperimentStatus.PLANNED, ExperimentPlatform.FACEBOOK)
        .stream()
        .filter(exp -> exp.getFacebookReleaseRequestedAt() != null)
        .filter(exp -> exp.getExperimentType() != ExperimentType.FAKE_EXPERIMENT)
        .toList();
  }

  /** Duplica um experimento preservando seu contrato comercial e configurações operacionais. */
  @Transactional
  public Experiment duplicate(Long id) {
    Experiment original = repository.findById(id).orElseThrow();
    Experiment copy =
        Experiment.builder()
            .niche(original.getNiche())
            .product(original.getProduct())
            .desireTerritoryCode(original.getDesireTerritoryCode())
            .desireTerritorySnapshotJson(original.getDesireTerritorySnapshotJson())
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
   * Deriva de uma homologação fake um experimento comercial de captação, sem reaproveitar métricas,
   * custos, publicações ou identificadores operacionais do teste.
   */
  @Transactional
  public Experiment commercialize(Long id) {
    Experiment original = repository.findById(id).orElseThrow();
    if (original.getExperimentType() != ExperimentType.FAKE_EXPERIMENT) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "only fake experiments can originate a commercial experiment");
    }
    if (original.getHypothesisRef() == null || original.getNiche() == null) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "fake experiment requires niche and hypothesis");
    }
    String automaticName =
        buildAutomaticExperimentName(original.getNiche(), original.getHypothesisRef());
    Experiment commercial =
        Experiment.builder()
            .niche(original.getNiche())
            .name(automaticName)
            .creationSource(ExperimentCreationSource.SYSTEM_FLOW)
            .hypothesis(original.getHypothesis())
            .singlePain(original.getSinglePain())
            .freeReward(original.getFreeReward())
            .funnelPromise(original.getFunnelPromise())
            .primaryCta(original.getPrimaryCta())
            .commercialObjective(original.getCommercialObjective())
            .experimentType(ExperimentType.NICHE_TEST)
            .productAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE)
            .campaignObjective(ExperimentCampaignObjective.LEADS)
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
            .status(ExperimentStatus.PLANNED)
            .platform(original.getPlatform())
            .stage(ExperimentStage.AD)
            .primaryVariable(original.getPrimaryVariable())
            .primaryMetric(original.getPrimaryMetric())
            .imagesPerPackage(original.getImagesPerPackage())
            .openImagesPerPackage(original.getOpenImagesPerPackage())
            .compressedImagesPerPackage(original.getCompressedImagesPerPackage())
            .facebookPage(original.getFacebookPage())
            .instagramAccount(original.getInstagramAccount())
            .journeyTemplate(original.getJourneyTemplate())
            .leadPortalFlowModel(original.getLeadPortalFlowModel())
            .schemaFirstLeadPortalEnabled(original.isSchemaFirstLeadPortalEnabled())
            .imageGenerationModel(original.getImageGenerationModel())
            .imageGenerationQuality(original.getImageGenerationQuality())
            .build();
    return repository.save(commercial);
  }

  /** Atualiza o status do experimento validando pré-condições comerciais da transição. */
  @Transactional
  public Experiment updateStatus(Long id, ExperimentStatus status) {
    Experiment exp = repository.findById(id).orElseThrow();
    ExperimentStatus previousStatus = exp.getStatus();
    if (status == ExperimentStatus.RUNNING) {
      validateRunningStatusTransition(exp);
    }
    if (status == ExperimentStatus.FINISHED) {
      validateFinishedStatusTransition(exp);
    }
    exp.setStatus(status);
    if (status == ExperimentStatus.RUNNING) {
      directPdeActivationService.activate(exp);
      recordDirectPdeActivation(exp, previousStatus);
    }
    if (status == ExperimentStatus.PAUSED) {
      experimentFunnelStandbyService.requestFacebookCampaignStops(
          exp.getId(),
          FacebookCampaignStopReason.ADMIN_EXPERIMENT_PAUSED,
          "pausa administrativa do experimento");
    }
    return exp;
  }

  /** Pausa um experimento por decisao governada do Operador e registra a origem auditavel. */
  @Transactional
  public Experiment pauseByGrowthOperator(Long id, String reason) {
    Experiment exp = repository.findById(id).orElseThrow();
    ExperimentStatus previousStatus = exp.getStatus();
    if (previousStatus != ExperimentStatus.RUNNING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "experiment is not running");
    }
    exp.setStatus(ExperimentStatus.PAUSED);
    exp.setLastStatusChangeAction("GROWTH_OPERATOR_PREVENTIVE_PAUSE");
    exp.setLastStatusChangeReason(reason);
    exp.setLastStatusChangedAt(Instant.now());
    statusChangeRepository.save(
        ExperimentStatusChange.builder()
            .experiment(exp)
            .previousStatus(previousStatus)
            .newStatus(ExperimentStatus.PAUSED)
            .action("GROWTH_OPERATOR_PREVENTIVE_PAUSE")
            .reason(reason)
            .changedBy("GROWTH_OPERATOR")
            .changedAt(exp.getLastStatusChangedAt())
            .build());
    experimentFunnelStandbyService.requestFacebookCampaignStops(
        exp.getId(),
        FacebookCampaignStopReason.ADMIN_EXPERIMENT_PAUSED,
        "pausa preventiva governada pelo Operador de Crescimento");
    return exp;
  }

  /** Registra pedido de retomada do Operador sem alterar o estado do experimento. */
  @Transactional
  public Experiment requestResumeApprovalByGrowthOperator(Long id, String reason) {
    Experiment exp = repository.findById(id).orElseThrow();
    if (exp.getStatus() == ExperimentStatus.RUNNING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "experiment is already running");
    }
    statusChangeRepository.save(
        ExperimentStatusChange.builder()
            .experiment(exp)
            .previousStatus(exp.getStatus())
            .newStatus(exp.getStatus())
            .action("GROWTH_OPERATOR_RESUME_REQUEST")
            .reason(reason)
            .changedBy("GROWTH_OPERATOR")
            .changedAt(Instant.now())
            .build());
    return exp;
  }

  /** Reativa um experimento parado registrando motivo de negócio e histórico auditável. */
  @Transactional
  public Experiment reactivate(Long id, ReactivateExperimentRequest request) {
    String reason = normalizeStatusChangeReason(request != null ? request.reason() : null);
    Experiment exp = repository.findById(id).orElseThrow();
    ExperimentStatus previousStatus = exp.getStatus();
    if (!isReactivationAvailable(exp)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "experiment status does not allow reactivation");
    }
    validateRunningStatusTransition(exp);
    exp.setStatus(ExperimentStatus.RUNNING);
    directPdeActivationService.activate(exp);
    exp.setLastStatusChangeAction("REACTIVATE");
    exp.setLastStatusChangeReason(reason);
    exp.setLastStatusChangedAt(Instant.now());
    statusChangeRepository.save(
        ExperimentStatusChange.builder()
            .experiment(exp)
            .previousStatus(previousStatus)
            .newStatus(ExperimentStatus.RUNNING)
            .action("REACTIVATE")
            .reason(reason)
            .changedBy("ADMIN_UI")
            .changedAt(exp.getLastStatusChangedAt())
            .build());
    return exp;
  }

  /** Verifica se o estado persistido aceita uma nova execução, antes dos bloqueios financeiros. */
  public boolean isReactivationAvailable(Experiment experiment) {
    if (experiment == null) {
      return false;
    }
    ExperimentStatus status = experiment.getStatus();
    return status == ExperimentStatus.PAUSED
        || status == ExperimentStatus.STANDBY
        || status == ExperimentStatus.USER_STOPPED
        || status == ExperimentStatus.INCONCLUSIVE
        || status == ExperimentStatus.FAILED;
  }

  /** Normaliza e valida o motivo informado para mudança auditável de status. */
  private String normalizeStatusChangeReason(String reason) {
    if (!StringUtils.hasText(reason)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reason required");
    }
    String normalized = reason.trim();
    if (normalized.length() < 10) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "reason must have at least 10 characters");
    }
    if (normalized.length() > 1024) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "reason must have at most 1024 characters");
    }
    return normalized;
  }

  /** Valida se a finalização definitiva possui aprendizado comercial registrado. */
  private void validateFinishedStatusTransition(Experiment exp) {
    if (!StringUtils.hasText(exp.getLearnedLessons())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "learnedLessons required before setting experiment FINISHED");
    }
  }

  /** Valida as evidências mínimas antes de aceitar experimento como em execução. */
  private void validateRunningStatusTransition(Experiment exp) {
    if (exp.getExperimentType() == ExperimentType.FAKE_EXPERIMENT) {
      return;
    }
    if (exp.getSampleSize() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize not set");
    }
    if (exp.getPlatform() == ExperimentPlatform.FACEBOOK
        && (exp.getKpiTargetCpl() == null || exp.getStopLossCpl() == null)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "financial fields not set");
    }
    if (exp.getPlatform() == ExperimentPlatform.FACEBOOK
        && (exp.getDailyBudget() == null
            || exp.getDailyBudget().compareTo(java.math.BigDecimal.ZERO) <= 0)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dailyBudget not set");
    }
    if (exp.getPlatform() == ExperimentPlatform.FACEBOOK
        && !facebookAdsCampaignRepository.existsByExperimentId(exp.getId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Facebook campaign must be registered before setting experiment RUNNING");
    }
    validateActivePdeDestination(exp);
    directPdeActivationService.validateReadyForActivation(exp);
  }

  /** Registra a origem e o motivo da ativação comercial direta quando o estado realmente muda. */
  private void recordDirectPdeActivation(Experiment experiment, ExperimentStatus previousStatus) {
    if (!directPdeActivationService.appliesTo(experiment)
        || previousStatus == ExperimentStatus.RUNNING) {
      return;
    }
    Instant changedAt = Instant.now();
    String reason =
        "Preflight produtivo aprovado; janela comercial direta liberada pelo painel administrativo";
    experiment.setLastStatusChangeAction("START");
    experiment.setLastStatusChangeReason(reason);
    experiment.setLastStatusChangedAt(changedAt);
    statusChangeRepository.save(
        ExperimentStatusChange.builder()
            .experiment(experiment)
            .previousStatus(previousStatus)
            .newStatus(ExperimentStatus.RUNNING)
            .action("START")
            .reason(reason)
            .changedBy("ADMIN_UI")
            .changedAt(changedAt)
            .build());
  }

  /** Bloqueia tráfego para versão PDE planejada, pausada ou encerrada. */
  private void validateActivePdeDestination(Experiment exp) {
    if (exp == null
        || exp.getExperimentType() != ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL) {
      return;
    }
    PdeProductionSlot slot = approvedPdeDestinationSlot(exp);
    if (slot.getStatus() == PdeProductionSlotStatus.ACTIVE
        && VALIDATION_OK.equals(slot.getValidationStatus())) {
      return;
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Versão PDE %s está %s e validação %s; ative e aprove a versão antes de colocar o experimento em RUNNING"
            .formatted(slot.getSlotCode(), slot.getStatus(), slot.getValidationStatus()));
  }

  /** Extrai o domínio de uma URL pública para comparar com o cadastro operacional de PDE. */
  private Optional<String> normalizeDomainFromUrl(String url) {
    if (!StringUtils.hasText(url)) {
      return Optional.empty();
    }
    String normalized = url.trim().replaceFirst("^https?://", "").replaceFirst("^//", "");
    int pathStart = normalized.indexOf('/');
    if (pathStart >= 0) {
      normalized = normalized.substring(0, pathStart);
    }
    int queryStart = normalized.indexOf('?');
    if (queryStart >= 0) {
      normalized = normalized.substring(0, queryStart);
    }
    int hashStart = normalized.indexOf('#');
    if (hashStart >= 0) {
      normalized = normalized.substring(0, hashStart);
    }
    int portStart = normalized.indexOf(':');
    if (portStart >= 0) {
      normalized = normalized.substring(0, portStart);
    }
    if (!StringUtils.hasText(normalized) || !normalized.contains(".")) {
      return Optional.empty();
    }
    return Optional.of(normalized.toLowerCase(Locale.ROOT));
  }

  /** Busca o slot PDE do destino e exige que ele exista no cadastro operacional. */
  private PdeProductionSlot approvedPdeDestinationSlot(Experiment experiment) {
    Optional<PdeProductionSlot> slotByDestination =
        normalizeDomainFromUrl(experiment.getFollowUpActionUrl())
            .flatMap(pdeProductionSlotRepository::findFirstByDomain);
    Optional<PdeProductionSlot> slotByExperiment =
        pdeProductionSlotRepository.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(
            experiment.getId());
    return slotByDestination
        .or(() -> slotByExperiment)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Experimento PDE MUSA exige uma versão PDE cadastrada, validada e aprovada no Marketing Hub antes da campanha."));
  }

  /**
   * Atualiza os campos mutáveis e preserva KPI e preset opcionais em experimentos ainda planejados.
   */
  @Transactional
  public Experiment update(Long id, UpdateExperimentRequest request) {
    Experiment exp = repository.findById(id).orElseThrow();

    if (request.getName() == null || request.getHypothesis() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "required fields missing");
    }

    if (!exp.getName().equals(request.getName())
        && repository.existsByNicheAndName(exp.getNiche(), request.getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name already exists for niche");
    }

    if (request.getStartDate() != null
        && request.getEndDate() != null
        && request.getStartDate().isAfter(request.getEndDate())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before endDate");
    }

    if (request.getSampleSize() != null && request.getSampleSize() < 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sampleSize must be at least 1");
    }
    BigDecimal resolvedBaselineCvr =
        request.getBaselineCvr() != null ? request.getBaselineCvr() : exp.getBaselineCvr();
    BigDecimal resolvedTargetCvr =
        request.getTargetCvr() != null ? request.getTargetCvr() : exp.getTargetCvr();
    if (resolvedBaselineCvr != null
        && resolvedTargetCvr != null
        && resolvedBaselineCvr.compareTo(resolvedTargetCvr) >= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "baselineCvr must be < targetCvr");
    }

    MetricPreset preset =
        StringUtils.hasText(request.getMetricPresetId())
            ? metricPresetService.get(request.getMetricPresetId())
            : exp.getMetricPreset();
    ExperimentStage resolvedStage =
        request.getStage() != null ? request.getStage() : exp.getStage();
    if (resolvedStage == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stage required");
    }
    String normalizedPrimaryVariable =
        resolvePrimaryDescriptor(
            request.getPrimaryVariable(), exp.getPrimaryVariable(), "primaryVariable");
    String normalizedPrimaryMetric =
        resolvePrimaryDescriptor(
            request.getPrimaryMetric(), exp.getPrimaryMetric(), "primaryMetric");

    exp.setName(request.getName());
    exp.setHypothesis(request.getHypothesis());
    if (request.isPlatformPresent()) {
      ExperimentPlatform resolvedPlatform = resolveExperimentPlatform(request.getPlatform());
      if (resolvedPlatform != exp.getPlatform() && exp.getStatus() != ExperimentStatus.PLANNED) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "O canal de aquisição só pode ser alterado enquanto o experimento estiver PLANNED");
      }
      exp.setPlatform(resolvedPlatform);
      if (resolvedPlatform == ExperimentPlatform.DIRECT_ONE_TO_ONE) {
        exp.setDailyBudget(null);
        exp.setFacebookPage(null);
        exp.setFacebookInstantForm(null);
        exp.setInstagramAccount(null);
        exp.setFacebookReleaseRequestedAt(null);
      }
    }
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
      if (exp.getExperimentType() == ExperimentType.FAKE_EXPERIMENT) {
        exp.setProductAiSubtype(null);
      }
    }
    if (request.isProductAiSubtypePresent()) {
      exp.setProductAiSubtype(
          exp.getExperimentType() == ExperimentType.FAKE_EXPERIMENT
              ? null
              : resolveProductAiSubtype(request.getProductAiSubtype(), exp.getHypothesisRef()));
    }
    if (request.isCampaignObjectivePresent()) {
      exp.setCampaignObjective(
          resolveCampaignObjective(
              request.getCampaignObjective(),
              request.isFreeRewardPresent() ? request.getFreeReward() : exp.getFreeReward(),
              exp.getExperimentType()));
    } else if (request.isFreeRewardPresent()) {
      exp.setCampaignObjective(
          resolveCampaignObjective(
              exp.getCampaignObjective(), request.getFreeReward(), exp.getExperimentType()));
    }
    if (request.getKpiTargetCpl() != null) {
      exp.setKpiTargetCpl(request.getKpiTargetCpl());
    }
    exp.setMetricPreset(preset);
    exp.setStage(resolvedStage);
    exp.setPrimaryVariable(normalizedPrimaryVariable);
    exp.setPrimaryMetric(normalizedPrimaryMetric);
    if (request.isDailyBudgetPresent()) {
      if (request.getDailyBudget() != null
          && request.getDailyBudget().compareTo(java.math.BigDecimal.ZERO) <= 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "dailyBudget must be greater than zero");
      }
      exp.setDailyBudget(request.getDailyBudget());
    }
    validateBudgetForPlatform(exp.getPlatform(), exp.getDailyBudget());
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
    } else if (preset != null && exp.getSampleSize() == null) {
      exp.setSampleSize(preset.getSampleSize());
    }
    if (request.getBaselineCvr() != null) {
      exp.setBaselineCvr(request.getBaselineCvr());
    }
    if (request.getTargetCvr() != null) {
      exp.setTargetCvr(request.getTargetCvr());
    }
    if (request.getMdePercent() != null) {
      exp.setMdePercent(request.getMdePercent());
    } else if (preset != null && exp.getMdePercent() == null) {
      exp.setMdePercent(preset.getDefaultMdePp());
    }
    if (request.getKpiTargetCpl() != null && preset != null && preset.getStopLossFactor() != null) {
      exp.setStopLossCpl(request.getKpiTargetCpl().multiply(preset.getStopLossFactor()));
    }
    if (exp.getPlatform() == ExperimentPlatform.DIRECT_ONE_TO_ONE) {
      exp.setKpiTargetCpl(null);
      exp.setMetricPreset(null);
      exp.setStopLossCpl(null);
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
      exp.setSchemaFirstLeadPortalEnabled(
          Boolean.TRUE.equals(request.getSchemaFirstLeadPortalEnabled()));
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
      exp.setFacebookInstantForm(
          attachInstantForm(request.getFacebookInstantFormId(), exp.getHypothesisRef().getId()));
    }
    if (request.isInstagramAccountIdPresent()) {
      exp.setInstagramAccount(attachInstagramAccount(request.getInstagramAccountId()));
    }
    if (request.isLeadPortalFlowIdPresent()) {
      if (request.getLeadPortalFlowId() == null) {
        exp.setLeadPortalFlow(null);
      } else {
        exp.setLeadPortalFlow(
            attachLeadPortalFlow(request.getLeadPortalFlowId(), exp.getNiche().getId()));
      }
    }
    if (request.isImageModelIdPresent() || request.isImageModelQualityIdPresent()) {
      Long currentModelId =
          request.isImageModelIdPresent()
              ? request.getImageModelId()
              : (exp.getImageGenerationModel() != null
                  ? exp.getImageGenerationModel().getId()
                  : null);
      Long currentQualityId =
          request.isImageModelQualityIdPresent()
              ? request.getImageModelQualityId()
              : (exp.getImageGenerationQuality() != null
                  ? exp.getImageGenerationQuality().getId()
                  : null);
      ImageGenerationSelection selection =
          resolveImageGenerationSelection(currentModelId, currentQualityId);
      exp.setImageGenerationModel(selection.model());
      exp.setImageGenerationQuality(selection.quality());
    }
    synchronizeLeadPortalFlow(exp);
    return exp;
  }

  /** Atualiza apenas a síntese de lições aprendidas do experimento. */
  @Transactional
  public Experiment updateLearnedLessons(Long id, String learnedLessons) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setLearnedLessons(normalizeLongText(learnedLessons));
    return exp;
  }

  /** Atualiza o objetivo comercial e a função operacional atual do experimento. */
  @Transactional
  public Experiment updateStrategicPositioning(
      Long id, String commercialObjective, String currentOperationalFunction) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setCommercialObjective(normalizeLongText(commercialObjective));
    exp.setCurrentOperationalFunction(normalizeLongText(currentOperationalFunction));
    return exp;
  }

  /** Normaliza texto longo editável preservando quebras de linha relevantes. */
  private String normalizeLongText(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  /** Solicita geração de novos criativos registrando estado operacional da fila. */
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
   * Enfileira a geração de criativos usando os textos e briefings já aprovados no pipeline do
   * experimento.
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

  /** Verifica se o pipeline possui texto e briefing de imagem antes de enfileirar anúncios. */
  private void ensurePipelinePrerequisites(Experiment exp) {
    if (!StringUtils.hasText(exp.getAdCopy()) || !StringUtils.hasText(exp.getAdImageBriefing())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Conclua as etapas de Texto do Anúncio e Prompt da Imagem antes de gerar anúncios do pipeline.");
    }
    if (!hasCompletedLandingReference(exp.getLandingPageImageAssets())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Dédalo precisa concluir ao menos um exemplo visual real da landing antes de materializar o criativo.");
    }
  }

  /** Confirma que o manifesto possui ao menos um arquivo concluído e acessível para referência. */
  private boolean hasCompletedLandingReference(String manifest) {
    if (!StringUtils.hasText(manifest)) {
      return false;
    }
    try {
      var images = objectMapper.readTree(manifest).path("images");
      if (!images.isArray()) {
        return false;
      }
      for (var image : images) {
        String status = image.path("status").asText("");
        boolean completed =
            "COMPLETED".equalsIgnoreCase(status)
                || "CONCLUIDO".equalsIgnoreCase(status)
                || "READY".equalsIgnoreCase(status);
        boolean hasUrl =
            StringUtils.hasText(image.path("resolvedUrl").asText(null))
                || StringUtils.hasText(image.path("webUrl").asText(null))
                || StringUtils.hasText(image.path("sourceUrl").asText(null));
        if (completed && hasUrl) {
          return true;
        }
      }
      return false;
    } catch (Exception ex) {
      log.warn("Manifesto de imagens da landing inválido durante preparação de criativo", ex);
      return false;
    }
  }

  @Transactional
  public Experiment requestInstantForms(Long id, int quantity) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setInstantFormsToGenerate(quantity);
    return exp;
  }

  /** Solicita geração de novos e-mails definindo a quantidade pendente. */
  @Transactional
  public Experiment requestEmails(Long id, int quantity) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setEmailsToGenerate(quantity);
    return exp;
  }

  /** Solicita geração de novos e-mails de amostra definindo a quantidade pendente. */
  @Transactional
  public Experiment requestSampleEmails(Long id, int quantity) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setSampleEmailsToGenerate(quantity);
    return exp;
  }

  /** Define qual e-mail de amostra gerado deve ser vinculado ao experimento. */
  @Transactional
  public Experiment updateSelectedSampleEmail(Long id, Long sampleEmailId) {
    Experiment exp = repository.findById(id).orElseThrow();
    if (sampleEmailId == null) {
      exp.setSelectedSampleEmail(null);
      return exp;
    }

    SampleEmail email =
        sampleEmailRepository
            .findById(sampleEmailId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sample email not found"));

    if (email.getExperiment() == null || !email.getExperiment().getId().equals(id)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Sample email does not belong to experiment");
    }

    exp.setSelectedSampleEmail(email);
    return exp;
  }

  /** Solicita geração de novas definições de entregáveis definindo a quantidade pendente. */
  @Transactional
  public Experiment requestDeliverables(Long id, int quantity) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setDeliverablesToGenerate(quantity);
    return exp;
  }

  /** Solicita geração de novos fluxos do portal do lead definindo a quantidade pendente. */
  @Transactional
  public Experiment requestLeadPortalFlows(Long id, int quantity) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setLeadPortalFlowsToGenerate(quantity);
    return exp;
  }

  /**
   * Libera o experimento para o Facebook Ads Worker e remove eventos dependentes antes de zerar o
   * funil.
   */
  @Transactional
  public Experiment releaseForFacebook(Long id) {
    Experiment experiment = repository.findById(id).orElseThrow();
    if (experiment.getExperimentType() == ExperimentType.FAKE_EXPERIMENT) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento fake é somente simulação para testar telas, PDE, vídeo e métricas; não pode ser liberado para campanha real.");
    }
    if (experiment.getPlatform() != ExperimentPlatform.FACEBOOK) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Experiment platform must be Facebook");
    }
    ensureLowTicketSalesPageWasBuiltByPipeline(experiment);
    ensurePdeMembershipDestinationIsCanonical(experiment);
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
    boolean pipelineCompleted =
        geraSalesPageStageExecutionRepository
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
    GeraSalesPagePublicationAudit publication =
        geraSalesPagePublicationAuditRepository
            .findTopByExperimentIdOrderByPublishedAtDesc(experiment.getId())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
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

  /** Bloqueia funil PDE MUSA quando o destino não aponta para slot produtivo versionado. */
  private void ensurePdeMembershipDestinationIsCanonical(Experiment experiment) {
    if (experiment == null
        || experiment.getExperimentType() != ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL) {
      return;
    }
    String destinationUrl = normalizeUrl(experiment.getFollowUpActionUrl());
    boolean canonicalDestination =
        StringUtils.hasText(destinationUrl)
            && destinationUrl.matches("^https://v[0-9]+\\.clubemusa\\.com\\.br($|/.*)");
    if (!canonicalDestination) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento PDE MUSA exige que o link do anúncio aponte para um slot produtivo versionado aprovado, como https://v5.clubemusa.com.br, com login gratuito e paywall interno.");
    }
    PdeProductionSlot slot = approvedPdeDestinationSlot(experiment);
    if (slot.getStatus() != PdeProductionSlotStatus.ACTIVE
        || !VALIDATION_OK.equals(slot.getValidationStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Experimento PDE MUSA exige versão PDE aprovada manualmente no Marketing Hub antes de liberar campanha; slot %s está %s e validação %s."
              .formatted(slot.getSlotCode(), slot.getStatus(), slot.getValidationStatus()));
    }
  }

  /** Informa se o experimento precisa de página de venda antes do checkout. */
  private boolean requiresSalesPageBeforePurchase(Experiment experiment) {
    return experiment != null
        && experiment.getExperimentType() != ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL
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
      Experiment experiment, GeraSalesPagePublicationAudit publication) {
    String destinationUrl = normalizeUrl(experiment.getFollowUpActionUrl());
    String salesPageUrl = normalizeUrl(publication.getSalesPageUrl());
    String checkoutUrl = normalizeUrl(publication.getCheckoutUrl());
    return StringUtils.hasText(destinationUrl)
        && StringUtils.hasText(salesPageUrl)
        && destinationUrl.equals(salesPageUrl)
        && (!StringUtils.hasText(checkoutUrl) || !destinationUrl.equals(checkoutUrl));
  }

  /** Confirma que o HTML publicado possui os coletores mínimos para ler o funil de venda. */
  private boolean hasRequiredSalesPageAnalyticsCollectors(
      GeraSalesPagePublicationAudit publication) {
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

  /** Remove a publicação anterior do Facebook e seus dados dependentes para permitir novo ciclo. */
  private void removePreviousFacebookPublication(Long experimentId) {
    List<String> campaignIds =
        facebookAdsCampaignRepository.findByExperimentId(experimentId).stream()
            .map(campaign -> campaign.getId())
            .toList();
    if (campaignIds.isEmpty()) {
      return;
    }
    entityManager
        .createNativeQuery(
            "DELETE FROM campaign_strategy_evaluation WHERE campaign_id IN (:campaignIds)")
        .setParameter("campaignIds", campaignIds)
        .executeUpdate();
    entityManager
        .createNativeQuery("DELETE FROM campaign_strategy WHERE campaign_id IN (:campaignIds)")
        .setParameter("campaignIds", campaignIds)
        .executeUpdate();
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
    if (quality != null
        && quality.getModel() != null
        && StringUtils.hasText(quality.getModel().getApiModel())) {
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
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "failed to synchronise lead portal flow", ex);
    }
  }

  private java.math.BigDecimal normalizeUnitPrice(java.math.BigDecimal unitPrice) {
    if (unitPrice == null) {
      return null;
    }
    if (unitPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "unitPrice must be greater than zero");
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

  private String resolvePrimaryDescriptor(
      String requestedValue, String currentValue, String fieldName) {
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

  /** Normaliza campos textuais da promessa única do experimento. */
  private String normalizeExperimentPromiseField(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  /** Resolve o tipo comercial do experimento mantendo compatibilidade com testes antigos. */
  private ExperimentType resolveExperimentType(ExperimentType requestedType) {
    return requestedType != null ? requestedType : ExperimentType.NICHE_TEST;
  }

  /** Resolve o canal mantendo Facebook como padrão para contratos legados sem o campo. */
  private ExperimentPlatform resolveExperimentPlatform(ExperimentPlatform requestedPlatform) {
    return requestedPlatform != null ? requestedPlatform : ExperimentPlatform.FACEBOOK;
  }

  /** Impede que verba de mídia seja registrada em uma validação individual sem campanha. */
  private void validateBudgetForPlatform(ExperimentPlatform platform, BigDecimal dailyBudget) {
    if (platform == ExperimentPlatform.DIRECT_ONE_TO_ONE && dailyBudget != null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "dailyBudget não se aplica ao canal de abordagem individual consentida");
    }
  }

  /**
   * Resolve o subtipo de Produto IA priorizando o experimento e herdando da hipótese quando
   * ausente.
   */
  private com.marketinghub.productai.ProductAiSubtype resolveProductAiSubtype(
      com.marketinghub.productai.ProductAiSubtype requestedSubtype,
      com.marketinghub.hypothesis.Hypothesis hypothesis) {
    if (requestedSubtype != null) {
      return requestedSubtype;
    }
    return hypothesis != null ? hypothesis.getProductAiSubtype() : null;
  }

  /** Impede que Produto IA avance para experimento sem preparo sistêmico da hipótese. */
  private void validateProductAiPreparation(
      java.util.UUID hypothesisId, com.marketinghub.productai.ProductAiSubtype productAiSubtype) {
    if (productAiSubtype == null) {
      return;
    }
    productAiExperimentPreparationService.assertReadyForExperiment(hypothesisId, productAiSubtype);
  }

  /** Resolve o objetivo de campanha conforme o tipo comercial do experimento. */
  private ExperimentCampaignObjective resolveCampaignObjective(
      ExperimentCampaignObjective requestedObjective,
      String freeReward,
      ExperimentType experimentType) {
    ExperimentType resolvedType = resolveExperimentType(experimentType);
    ExperimentCampaignObjective objective =
        requestedObjective != null
            ? requestedObjective
            : resolvedType == ExperimentType.FAKE_EXPERIMENT
                ? ExperimentCampaignObjective.TRAFFIC
                : resolvedType == ExperimentType.LOW_TICKET_PRODUCT
                        || resolvedType == ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL
                    ? ExperimentCampaignObjective.SALES
                    : ExperimentCampaignObjective.LEADS;
    if (resolvedType == ExperimentType.NICHE_TEST
        && StringUtils.hasText(freeReward)
        && objective != ExperimentCampaignObjective.LEADS) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "campaignObjective must be LEADS when freeReward is present");
    }
    return objective;
  }

  /** Lista experimentos com geração de criativos pendente para consumo operacional do AI Worker. */
  @Transactional(readOnly = true)
  public List<Experiment> listPendingCreativeGeneration(int limit) {
    int effectiveLimit = Math.max(1, limit);
    return repository.findAllToGenerateCreatives().stream()
        .filter(
            experiment ->
                experiment.getCreativeGenerationStatus() == CreativeGenerationStatus.REQUESTED
                    || experiment.getCreativeGenerationStatus()
                        == CreativeGenerationStatus.PROCESSING)
        .peek(this::attachApprovedCommercialVisualAssets)
        .limit(effectiveLimit)
        .toList();
  }

  /** Anexa somente referências aprovadas do plano ao contrato operacional do AI Worker. */
  private void attachApprovedCommercialVisualAssets(Experiment experiment) {
    commercialPlanRepository.findByExperimentReference(experiment.getId()).stream()
        .findFirst()
        .ifPresent(
            plan -> {
              var assets =
                  commercialPlanVisualAssetRepository
                      .findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
                          plan.getId(), CommercialPlanVisualAssetStatus.APPROVED)
                      .stream()
                      .map(
                          asset ->
                              java.util.Map.of(
                                  "url", asset.getAssetUrl(),
                                  "label", asset.getLabel(),
                                  "purpose", asset.getPurpose(),
                                  "purposesJson", Objects.toString(asset.getPurposesJson(), "[]"),
                                  "origin", asset.getOrigin(),
                                  "version", asset.getVersionNumber()))
                      .toList();
              try {
                experiment.setCommercialPlanVisualAssets(
                    objectMapper.writeValueAsString(java.util.Map.of("assets", assets)));
              } catch (JsonProcessingException ex) {
                log.error(
                    "Falha ao montar Biblioteca Audiovisual aprovada. experimentId={} planId={}",
                    experiment.getId(),
                    plan.getId(),
                    ex);
                throw new IllegalStateException(
                    "Falha ao montar kit visual aprovado do experimento " + experiment.getId(), ex);
              }
            });
  }

  /** Marca a solicitação de criativos como em processamento pelo worker. */
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

  /** Marca a solicitação de criativos como concluída e limpa a pendência da tela. */
  @Transactional
  public Experiment markCreativeGenerationCompleted(Long id) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setCreativesToGenerate(0);
    exp.setCreativeGenerationStatus(CreativeGenerationStatus.COMPLETED);
    exp.setCreativeGenerationFinishedAt(Instant.now());
    exp.setCreativeGenerationError(null);
    return exp;
  }

  /** Marca a solicitação de criativos como falha para destravar nova tentativa consciente. */
  @Transactional
  public Experiment markCreativeGenerationFailed(Long id, String error) {
    Experiment exp = repository.findById(id).orElseThrow();
    exp.setCreativesToGenerate(0);
    exp.setCreativeGenerationStatus(CreativeGenerationStatus.FAILED);
    exp.setCreativeGenerationFinishedAt(Instant.now());
    exp.setCreativeGenerationError(
        StringUtils.hasText(error) ? error.trim() : "Falha ao gerar criativos");
    return exp;
  }

  private int normalizeImagesPerPackage(Integer imagesPerPackage) {
    if (imagesPerPackage == null) {
      return 20;
    }
    if (imagesPerPackage <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "imagesPerPackage must be greater than zero");
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
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "followUpActionUrl must start with http:// or https://");
    }
    if (trimmed.length() > 512) {
      return trimmed.substring(0, 512);
    }
    return trimmed;
  }
}
