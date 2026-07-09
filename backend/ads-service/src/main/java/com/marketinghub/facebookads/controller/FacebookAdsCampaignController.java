package com.marketinghub.facebookads.controller;

import com.marketinghub.repository.jpa.experiment.AdSetRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.ads.FacebookAccount;
import com.marketinghub.repository.jpa.ads.FacebookAccountRepository;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeStatus;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.funnel.ExperimentFunnelAutoStopService;
import com.marketinghub.experiment.service.ExperimentCampaignMetricService;
import com.marketinghub.experiment.service.ExperimentService;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbTestDto;
import com.marketinghub.experiment.salespageab.dto.ExperimentSalesPageAbVariantDto;
import com.marketinghub.experiment.salespageab.service.ExperimentSalesPageAbTestService;
import com.marketinghub.journey.model.JourneyStep;
import com.marketinghub.journey.model.JourneyStimulusType;
import com.marketinghub.leadportal.LeadPortalFlow;
import com.marketinghub.leadportal.support.LeadPortalPublicUrlResolver;
import com.marketinghub.leadportal.service.LeadPortalMetricsService;
import com.marketinghub.facebookads.AdCreativeKind;
import com.marketinghub.facebookads.BudgetMode;
import com.marketinghub.facebookads.CampaignStrategy;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.service.CampaignStrategyService;
import com.marketinghub.facebookads.service.recommendation.FacebookCampaignRecommendationDto;
import com.marketinghub.facebookads.service.recommendation.FacebookCampaignRecommendationIngestionRequest;
import com.marketinghub.facebookads.service.recommendation.FacebookCampaignRecommendationService;
import com.marketinghub.facebookads.service.publicationstep.FacebookCampaignPublicationJobStepRequest;
import com.marketinghub.facebookads.service.publicationstep.FacebookCampaignPublicationJobStepService;
import com.marketinghub.facebookads.service.recommendation.FacebookCampaignRecommendationSyncTarget;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.experiment.AdSet;
import com.marketinghub.leadportal.dto.LeadPortalExperimentMetricsDto;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Agrupa endpoints de campanhas Facebook Ads consumidos pela UI e pelo worker.
 */
@RestController
@RequestMapping("/api/facebook-campaigns")
public class FacebookAdsCampaignController {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookAdsCampaignController.class);
    private static final Duration METRICS_SETTLEMENT_WINDOW = Duration.ofHours(72);
    private static final Set<ExperimentStatus> METRICS_SETTLEMENT_STATUSES = EnumSet.of(
            ExperimentStatus.PAUSED,
            ExperimentStatus.STANDBY,
            ExperimentStatus.USER_STOPPED,
            ExperimentStatus.VALIDATED,
            ExperimentStatus.INVALIDATED,
            ExperimentStatus.INCONCLUSIVE,
            ExperimentStatus.FINISHED,
            ExperimentStatus.FAILED);

    private final ExperimentService experimentService;
    private final FacebookAdsCampaignRepository campaignRepository;
    private final FacebookAccountRepository accountRepository;
    private final FacebookAdsAdSetRepository adSetRepository;
    private final FacebookAdsAdCreativeRepository adCreativeRepository;
    private final FacebookAdsAdRepository adRepository;
    private final CreativeRepository creativeRepository;
    private final com.marketinghub.repository.jpa.experiment.AdSetRepository experimentAdSetRepository;
    private final ObjectMapper objectMapper;
    private final ExperimentCampaignMetricService campaignMetricService;
    private final ExperimentFunnelAutoStopService funnelAutoStopService;
    private final com.marketinghub.experiment.service.ExperimentReadinessService experimentReadinessService;
    private final LeadPortalPublicUrlResolver leadPortalPublicUrlResolver;

    private final LeadPortalMetricsService leadPortalMetricsService;
    private final FacebookCampaignRecommendationService recommendationService;
    private final FacebookCampaignPublicationJobStepService publicationJobStepService;
    private final CampaignStrategyService campaignStrategyService;
    private final ExperimentSalesPageAbTestService salesPageAbTestService;

    /**
     * Cria o controller com os repositórios e serviços usados pelos contratos de campanhas Facebook.
     */
    public FacebookAdsCampaignController(ExperimentService experimentService,
                                         FacebookAdsCampaignRepository campaignRepository,
                                         FacebookAccountRepository accountRepository,
                                         FacebookAdsAdSetRepository adSetRepository,
                                         FacebookAdsAdCreativeRepository adCreativeRepository,
                                         FacebookAdsAdRepository adRepository,
                                         CreativeRepository creativeRepository,
                                         com.marketinghub.repository.jpa.experiment.AdSetRepository experimentAdSetRepository,
                                         ObjectMapper objectMapper,
                                         ExperimentCampaignMetricService campaignMetricService,
                                         ExperimentFunnelAutoStopService funnelAutoStopService,
                                         com.marketinghub.experiment.service.ExperimentReadinessService experimentReadinessService,
                                         LeadPortalPublicUrlResolver leadPortalPublicUrlResolver,
                                         LeadPortalMetricsService leadPortalMetricsService,
                                         FacebookCampaignRecommendationService recommendationService,
                                         FacebookCampaignPublicationJobStepService publicationJobStepService,
                                         CampaignStrategyService campaignStrategyService,
                                         ExperimentSalesPageAbTestService salesPageAbTestService) {
        this.experimentService = experimentService;
        this.campaignRepository = campaignRepository;
        this.accountRepository = accountRepository;
        this.adSetRepository = adSetRepository;
        this.adCreativeRepository = adCreativeRepository;
        this.adRepository = adRepository;
        this.creativeRepository = creativeRepository;
        this.experimentAdSetRepository = experimentAdSetRepository;
        this.objectMapper = objectMapper;
        this.campaignMetricService = campaignMetricService;
        this.funnelAutoStopService = funnelAutoStopService;
        this.experimentReadinessService = experimentReadinessService;
        this.leadPortalPublicUrlResolver = leadPortalPublicUrlResolver;
        this.leadPortalMetricsService = leadPortalMetricsService;
        this.recommendationService = recommendationService;
        this.publicationJobStepService = publicationJobStepService;
        this.campaignStrategyService = campaignStrategyService;
        this.salesPageAbTestService = salesPageAbTestService;
    }

    @GetMapping("/experiments-ready")
    // Executa a operação experimentsReady da integração Facebook Ads.
    public List<ExperimentSummary> experimentsReady() {
        Map<Long, LeadPortalExperimentMetricsDto> leadPortalMetrics = indexLeadPortalMetrics();
        return experimentService
                .listByStatusAndPlatform(com.marketinghub.experiment.ExperimentStatus.PLANNED,
                        com.marketinghub.experiment.ExperimentPlatform.FACEBOOK)
                .stream()
                .filter(experiment -> experiment.getFacebookReleaseRequestedAt() != null)
                .filter(experiment -> !campaignRepository.existsByExperimentId(experiment.getId()))
                .filter(experimentReadinessService::isReadyForCampaign)
                .peek(this::registerBackendDispatchStep)
                .map(experiment -> toSummary(experiment, leadPortalMetrics.get(experiment.getId())))
                .toList();
    }

    @PostMapping("/publication-job-steps")
    @ResponseStatus(HttpStatus.ACCEPTED)
    // Registra um passo executado pelo Facebook Ads Worker na publicação de campanha.
    public void registerPublicationJobStep(@RequestBody FacebookCampaignPublicationJobStepRequest request) {
        publicationJobStepService.register(request);
    }

    @GetMapping("/experiments")
    public List<ExperimentSummary> experiments(@RequestParam("status")
            com.marketinghub.experiment.ExperimentStatus status) {
        Map<Long, LeadPortalExperimentMetricsDto> leadPortalMetrics = indexLeadPortalMetrics();
        return experimentService
                .listByStatusAndPlatform(status, com.marketinghub.experiment.ExperimentPlatform.FACEBOOK)
                .stream()
                .map(experiment -> toSummary(experiment, leadPortalMetrics.get(experiment.getId())))
                .toList();
    }

    // Executa a operação indexLeadPortalMetrics da integração Facebook Ads.
    private Map<Long, LeadPortalExperimentMetricsDto> indexLeadPortalMetrics() {
        List<LeadPortalExperimentMetricsDto> metrics = leadPortalMetricsService.listExperimentMetrics();
        if (metrics == null || metrics.isEmpty()) {
            return Map.of();
        }
        return metrics.stream()
                .collect(Collectors.toMap(
                        LeadPortalExperimentMetricsDto::experimentId,
                        dto -> dto,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }


    @GetMapping("/recommendations/sync-targets")
    // Lista campanhas ativas que precisam de coleta de sugestões oficiais da Meta.
    public List<FacebookCampaignRecommendationSyncTarget> recommendationSyncTargets() {
        return recommendationService.listSyncTargets();
    }

    @PostMapping("/{campaignId}/recommendations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Recebe do worker o retrato mais recente de sugestões oficiais da Meta para a campanha.
    public void ingestRecommendations(@PathVariable String campaignId,
                                      @RequestBody FacebookCampaignRecommendationIngestionRequest request) {
        recommendationService.ingest(campaignId, request);
    }

    @PostMapping("/{campaignId}/recommendations-error")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    // Registra falha de coleta de sugestões da Meta sem apagar o último retrato válido.
    public void recommendationSyncError(@PathVariable String campaignId,
                                        @RequestBody CampaignMetricsErrorRequest request) {
        recommendationService.registerError(campaignId, request != null ? request.message() : null);
    }

    @GetMapping("/{campaignId}/recommendations")
    // Lista as sugestões oficiais da Meta salvas para uma campanha.
    public List<FacebookCampaignRecommendationDto> listRecommendations(@PathVariable String campaignId) {
        return recommendationService.listByCampaign(campaignId);
    }

    @GetMapping("/metrics/sync-targets")
    // Lista campanhas em execução, campanhas em liquidação e campanhas encerradas sem fechamento final.
    public List<CampaignMetricsSyncTarget> metricsSyncTargets() {
        Instant settlementCutoff = Instant.now().minus(METRICS_SETTLEMENT_WINDOW);
        return campaignRepository.findMetricsSyncTargets(
                        ExperimentStatus.RUNNING,
                        METRICS_SETTLEMENT_STATUSES,
                        settlementCutoff)
                .stream()
                .map(c -> new CampaignMetricsSyncTarget(c.getId(), c.getExperiment().getId(), c.getMetricsLastSyncedAt()))
                .toList();
    }


    @GetMapping("/experiments/{experimentId}/creatives-ready")
    // Lista os criativos aprovados para consumo exclusivo do Facebook Ads Worker.
    public List<FacebookCreativeConsumptionResponse> listReadyCreativesForFacebook(@PathVariable Long experimentId) {
        return creativeRepository.findByExperimentId(experimentId).stream()
                .filter(creative -> creative.getStatus() == CreativeStatus.READY)
                .sorted(Comparator.comparing(Creative::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toFacebookCreativeConsumptionResponse)
                .toList();
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    // Executa a operação create da integração Facebook Ads.
    public void create(@RequestBody CreateCampaignRequest req) {
        Experiment experiment;
        try {
            experiment = experimentService.get(req.experimentId());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Experiment not found: " + req.experimentId(), ex);
        }
        var existingCampaign = campaignRepository.findById(req.id());
        if (existingCampaign.isPresent()) {
            if (!Objects.equals(existingCampaign.get().getExperiment().getId(), req.experimentId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Facebook campaign already belongs to another experiment: " + req.id());
            }
            reconcileExistingCampaignPublication(existingCampaign.get(), req);
            campaignStrategyService.ensureDefaultStrategy(existingCampaign.get());
            experiment.setStatus(ExperimentStatus.RUNNING);
            return;
        }
        if (campaignRepository.existsByExperimentId(req.experimentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Experiment already has a Facebook campaign: " + req.experimentId());
        }
        FacebookAccount account = accountRepository.findById(req.facebookAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Facebook account not found: " + req.facebookAccountId()));
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId(req.id());
        campaign.setExternalId(resolveMetaId(req.externalId(), req.id()));
        campaign.setAdAccountId(req.adAccountId());
        campaign.setName(req.name());
        campaign.setObjective(req.objective());
        campaign.setStatus(resolveCampaignPublicationStatus(req.status()));
        campaign.setBudgetMode(req.budgetMode());
        campaign.setExperiment(experiment);
        campaign.setFacebookAccount(account);
        FacebookAdsCampaign savedCampaign = campaignRepository.save(campaign);
        campaignStrategyService.ensureDefaultStrategy(savedCampaign);

        FacebookAdsAdSet savedAdSet = null;
        if (req.adSet() != null) {
            AdSet experimentAdSet = resolveExperimentAdSet(req.adSet(), experiment);
            savedAdSet = adSetRepository.save(mapAdSet(req.adSet(), savedCampaign, experimentAdSet));
        }

        List<CreateCampaignRequest.AdCreative> creativeRequests = resolveAdCreativeRequests(req);
        Map<String, FacebookAdsAdCreative> savedCreatives = new LinkedHashMap<>();
        for (CreateCampaignRequest.AdCreative creativeRequest : creativeRequests) {
            FacebookAdsAdCreative savedCreative = adCreativeRepository.save(mapAdCreative(creativeRequest));
            savedCreatives.put(savedCreative.getId(), savedCreative);
        }

        List<CreateCampaignRequest.Ad> adRequests = resolveAdRequests(req);
        if (!adRequests.isEmpty()) {
            if (savedAdSet == null || savedCreatives.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ad set and ad creatives must be provided when reporting ads"
                );
            }
            for (CreateCampaignRequest.Ad adRequest : adRequests) {
                FacebookAdsAdCreative creative = savedCreatives.get(adRequest.creativeId());
                if (creative == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Ad references unknown creative: " + adRequest.creativeId());
                }
                adRepository.save(mapAd(adRequest, savedAdSet, creative));
            }
        }
        experiment.setStatus(ExperimentStatus.RUNNING);
    }


    // Resolve o status canônico da campanha após confirmação de publicação completa pelo worker.
    private FacebookAdStatus resolveCampaignPublicationStatus(FacebookAdStatus status) {
        return status != null ? status : FacebookAdStatus.ACTIVE;
    }

    // Reconcilia status de publicação quando o worker reporta novamente uma campanha já persistida.
    private void reconcileExistingCampaignPublication(FacebookAdsCampaign campaign, CreateCampaignRequest req) {
        campaign.setStatus(resolveCampaignPublicationStatus(req.status()));
        if (req.adSet() != null && StringUtils.hasText(req.adSet().id())) {
            adSetRepository.findById(req.adSet().id())
                    .ifPresent(adSet -> adSet.setStatus(resolveCampaignPublicationStatus(req.adSet().status())));
        }
        for (CreateCampaignRequest.Ad adRequest : resolveAdRequests(req)) {
            if (adRequest == null || !StringUtils.hasText(adRequest.id())) {
                continue;
            }
            adRepository.findById(adRequest.id())
                    .ifPresent(ad -> ad.setStatus(resolveCampaignPublicationStatus(adRequest.status())));
        }
    }

    /**
     * Atualiza o status de campanha, ad sets e anúncios com o retrato efetivo recebido da Meta.
     */
    @PostMapping("/{campaignId}/status-sync")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public void syncStatus(
            @PathVariable String campaignId,
            @RequestBody CampaignStatusSyncRequest request) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Facebook campaign not found: " + campaignId));
        campaign.setStatus(resolveMetaStatus(request.status(), request.effectiveStatus(), campaign.getStatus()));
        reconcilePausedCampaignWithExperiment(campaign);
        if (request.adSets() != null) {
            for (CampaignStatusSyncRequest.AdSetStatus adSetStatus : request.adSets()) {
                if (adSetStatus == null || !StringUtils.hasText(adSetStatus.id())) {
                    continue;
                }
                adSetRepository.findById(adSetStatus.id())
                        .ifPresent(adSet -> adSet.setStatus(resolveMetaStatus(
                                adSetStatus.status(),
                                adSetStatus.effectiveStatus(),
                                adSet.getStatus())));
            }
        }
        if (request.ads() != null) {
            for (CampaignStatusSyncRequest.AdStatus adStatus : request.ads()) {
                if (adStatus == null || !StringUtils.hasText(adStatus.id())) {
                    continue;
                }
                adRepository.findById(adStatus.id())
                        .ifPresent(ad -> ad.setStatus(resolveMetaStatus(
                                adStatus.status(),
                                adStatus.effectiveStatus(),
                                ad.getStatus())));
            }
        }
    }

    // Resolve o status vindo da Meta preferindo o status efetivo quando ele estiver disponível.
    private FacebookAdStatus resolveMetaStatus(String status, String effectiveStatus, FacebookAdStatus fallback) {
        FacebookAdStatus effective = parseFacebookStatus(effectiveStatus);
        if (effective != null) {
            return effective;
        }
        FacebookAdStatus configured = parseFacebookStatus(status);
        if (configured != null) {
            return configured;
        }
        return fallback != null ? fallback : FacebookAdStatus.ACTIVE;
    }

    // Converte texto de status da Meta para o enum interno quando existir correspondência.
    private FacebookAdStatus parseFacebookStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return FacebookAdStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Atualiza métricas da campanha e avalia paradas automáticas dependentes do gasto sincronizado.
     */
    @PostMapping("/{campaignId}/metrics")
    @Transactional
    public CampaignMetricSummary updateMetrics(
            @PathVariable String campaignId,
            @RequestBody CampaignMetricsUpdateRequest request) {
        ExperimentCampaignMetric metric = campaignMetricService.upsert(
                campaignId,
                request.dateStart(),
                request.dateStop(),
                request.reach(),
                request.impressions(),
                request.clicks(),
                request.leads(),
                request.spend());
        FacebookAdsCampaign campaign = metric.getCampaign();
        campaign.setMetricsLastSyncedAt(Instant.now());
        campaign.setMetricsLastError(null);
        markFinalMetricsSyncWhenExperimentIsSettled(campaign);
        evaluateMetricsSideEffectsSafely(campaign, metric);
        return toMetricSummary(metric);
    }

    // Marca que uma campanha encerrada já teve reconciliação final de métricas contra a Meta.
    private void markFinalMetricsSyncWhenExperimentIsSettled(FacebookAdsCampaign campaign) {
        if (campaign == null || campaign.getExperiment() == null) {
            return;
        }
        if (METRICS_SETTLEMENT_STATUSES.contains(campaign.getExperiment().getStatus())) {
            campaign.setMetricsFinalSyncedAt(campaign.getMetricsLastSyncedAt());
        }
    }

    // Fecha o experimento quando a Meta informa que a campanha foi pausada fora do Hub.
    private void reconcilePausedCampaignWithExperiment(FacebookAdsCampaign campaign) {
        if (campaign.getStatus() != FacebookAdStatus.PAUSED || campaign.getExperiment() == null) {
            return;
        }
        Experiment experiment = campaign.getExperiment();
        if (experiment.getStatus() == ExperimentStatus.RUNNING) {
            experiment.setStatus(ExperimentStatus.USER_STOPPED);
        }
    }

    // Executa regras derivadas da métrica sem impedir a persistência do gasto oficial da Meta.
    private void evaluateMetricsSideEffectsSafely(FacebookAdsCampaign campaign, ExperimentCampaignMetric metric) {
        try {
            funnelAutoStopService.stopIfNoPrimaryResultAfterMinimumSpend(campaign.getExperiment());
            funnelAutoStopService.stopIfAnyPrioritizedStageStatisticallyFailed(campaign.getExperiment());
            funnelAutoStopService.stopIfLowImpressionsAfterRunningTime(
                    campaign.getExperiment(),
                    metric.getImpressions(),
                    campaign.getCreatedAt());
            campaignStrategyService.evaluateAfterMetrics(metric);
        } catch (Exception ex) {
            Long experimentId = campaign.getExperiment() != null ? campaign.getExperiment().getId() : null;
            campaign.setMetricsLastError("Metric side-effect evaluation failed: " + ex.getMessage());
            LOGGER.warn(
                    "Falha ao avaliar regras derivadas de metricas Facebook Ads. modulo=facebookads operacao=updateMetrics campaignId={} experimentId={}",
                    campaign.getId(),
                    experimentId,
                    ex);
        }
    }

    @PostMapping("/{campaignId}/metrics-error")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public void registerMetricsError(
            @PathVariable String campaignId,
            @RequestBody CampaignMetricsErrorRequest request) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Facebook campaign not found: " + campaignId));
        campaign.setMetricsLastError(request.message());
        campaign.setMetricsLastSyncedAt(Instant.now());
    }

    // Executa a operação resolveExperimentAdSet da integração Facebook Ads.
    private AdSet resolveExperimentAdSet(CreateCampaignRequest.AdSet adSetReq, Experiment experiment) {
        if (adSetReq.experimentAdSetId() == null) {
            return null;
        }
        AdSet experimentAdSet = experimentAdSetRepository.findById(adSetReq.experimentAdSetId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Experiment ad set not found: " + adSetReq.experimentAdSetId()));
        if (!experimentAdSet.getExperiment().getId().equals(experiment.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Experiment ad set does not belong to experiment " + experiment.getId());
        }
        return experimentAdSet;
    }


    // Converte o criativo do domínio Experimento no contrato de leitura do módulo Facebook.
    private FacebookCreativeConsumptionResponse toFacebookCreativeConsumptionResponse(Creative creative) {
        Long experimentId = creative.getExperiment() != null ? creative.getExperiment().getId() : null;
        return new FacebookCreativeConsumptionResponse(
                creative.getId(),
                experimentId,
                creative.getFormat(),
                creative.getHeadline(),
                creative.getPrimaryText(),
                creative.getImageUrl(),
                creative.getDescription(),
                creative.getCta(),
                creative.getDestinationUrl(),
                creative.getLeadGenFormId(),
                creative.getInstagramUserId(),
                creative.getStatus() != null ? creative.getStatus().name() : null
        );
    }

    // Converte o ad set reportado pelo worker e preserva o status publicado na Meta.
    private FacebookAdsAdSet mapAdSet(CreateCampaignRequest.AdSet adSetReq,
                                      FacebookAdsCampaign campaign,
                                      AdSet experimentAdSet) {
        FacebookAdsAdSet adSet = new FacebookAdsAdSet();
        adSet.setId(adSetReq.id());
        adSet.setExternalId(resolveMetaId(adSetReq.externalId(), adSetReq.id()));
        adSet.setCampaign(campaign);
        adSet.setName(adSetReq.name());
        adSet.setExperimentAdSet(experimentAdSet);
        adSet.setBillingEvent(adSetReq.billingEvent());
        adSet.setOptimizationGoal(adSetReq.optimizationGoal());
        adSet.setBidStrategy(resolveBidStrategy(adSetReq.bidStrategy()));
        adSet.setDailyBudgetMinor(parseLong(adSetReq.dailyBudget()));
        adSet.setLifetimeBudgetMinor(parseLong(adSetReq.lifetimeBudget()));
        adSet.setBidAmountMinor(parseLong(adSetReq.bidAmount()));
        adSet.setStatus(resolveCampaignPublicationStatus(adSetReq.status()));
        adSet.setPromotedObjectJson(buildPromotedObjectJson(
                adSetReq.pageId(),
                adSetReq.pixelId(),
                adSetReq.customEventType()));
        adSet.setTargetingJson(buildTargetingJson(adSetReq.targetCountry(), adSetReq.targetingJson(), adSetReq.savedAudienceId()));
        return adSet;
    }

    // Executa a operação mapAdCreative da integração Facebook Ads.
    private FacebookAdsAdCreative mapAdCreative(CreateCampaignRequest.AdCreative creativeReq) {
        FacebookAdsAdCreative creative = new FacebookAdsAdCreative();
        creative.setId(creativeReq.id());
        creative.setPageId(creativeReq.pageId());
        creative.setInstagramUserId(creativeReq.instagramActorId());
        creative.setKind(AdCreativeKind.LINK);
        creative.setLinkDataJson(buildLinkDataJson(creativeReq));
        return creative;
    }

    // Converte o anúncio reportado pelo worker e preserva o status publicado na Meta.
    private FacebookAdsAd mapAd(CreateCampaignRequest.Ad adReq,
                                 FacebookAdsAdSet adSet,
                                 FacebookAdsAdCreative creative) {
        FacebookAdsAd ad = new FacebookAdsAd();
        ad.setId(adReq.id());
        ad.setExternalId(resolveMetaId(adReq.externalId(), adReq.id()));
        ad.setName(adReq.name());
        ad.setAdSet(adSet);
        ad.setCreative(creative);
        ad.setStatus(resolveCampaignPublicationStatus(adReq.status()));
        return ad;
    }

    // Executa a operação resolveAdCreativeRequests da integração Facebook Ads.
    private List<CreateCampaignRequest.AdCreative> resolveAdCreativeRequests(CreateCampaignRequest req) {
        List<CreateCampaignRequest.AdCreative> creatives = new ArrayList<>();
        if (req.adCreative() != null) {
            creatives.add(req.adCreative());
        }
        if (req.adCreatives() != null) {
            req.adCreatives().stream()
                    .filter(Objects::nonNull)
                    .forEach(creatives::add);
        }
        return creatives;
    }

    // Executa a operação resolveAdRequests da integração Facebook Ads.
    private List<CreateCampaignRequest.Ad> resolveAdRequests(CreateCampaignRequest req) {
        List<CreateCampaignRequest.Ad> ads = new ArrayList<>();
        if (req.ad() != null) {
            ads.add(req.ad());
        }
        if (req.ads() != null) {
            req.ads().stream()
                    .filter(Objects::nonNull)
                    .forEach(ads::add);
        }
        return ads;
    }

    // Executa a operação resolveBidStrategy da integração Facebook Ads.
    private String resolveBidStrategy(String bidStrategy) {
        if (StringUtils.hasText(bidStrategy)) {
            return bidStrategy.trim();
        }
        return "LOWEST_COST_WITHOUT_CAP";
    }

    // Executa a operação parseLong da integração Facebook Ads.
    private Long parseLong(String numeric) {
        if (!StringUtils.hasText(numeric)) {
            return null;
        }
        try {
            return Long.parseLong(numeric.trim());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid numeric value: " + numeric, ex);
        }
    }

    // Executa a operação resolveMetaId da integração Facebook Ads.
    private String resolveMetaId(String externalId, String fallbackId) {
        if (StringUtils.hasText(externalId)) {
            return externalId.trim();
        }
        if (StringUtils.hasText(fallbackId) && !isUuid(fallbackId)) {
            return fallbackId.trim();
        }
        return null;
    }

    // Executa a operação isUuid da integração Facebook Ads.
    private boolean isUuid(String value) {
        try {
            UUID.fromString(value.trim());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    // Executa a operação buildPromotedObjectJson da integração Facebook Ads.
    private String buildPromotedObjectJson(String pageId, String pixelId, String customEventType) {
        if (!StringUtils.hasText(pageId) && !StringUtils.hasText(pixelId) && !StringUtils.hasText(customEventType)) {
            return null;
        }
        ObjectNode node = objectMapper.createObjectNode();
        if (StringUtils.hasText(pageId)) {
            node.put("page_id", pageId);
        }
        if (StringUtils.hasText(pixelId)) {
            node.put("pixel_id", pixelId);
        }
        if (StringUtils.hasText(customEventType)) {
            node.put("custom_event_type", customEventType);
        }
        return node.toString();
    }

    // Executa a operação buildTargetingJson da integração Facebook Ads.
    private String buildTargetingJson(String targetCountry, String targetingJson, String savedAudienceId) {
        ObjectNode node;
        if (StringUtils.hasText(targetingJson)) {
            try {
                JsonNode parsed = objectMapper.readTree(targetingJson);
                if (parsed != null && parsed.isObject()) {
                    node = ((ObjectNode) parsed).deepCopy();
                } else {
                    node = objectMapper.createObjectNode();
                }
            } catch (JsonProcessingException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid targeting JSON", ex);
            }
        } else {
            node = objectMapper.createObjectNode();
        }

        if (StringUtils.hasText(savedAudienceId)) {
            node.put("saved_audience_id", savedAudienceId);
        }

        if (!node.has("geo_locations")) {
            ObjectNode geoLocations = node.putObject("geo_locations");
            ArrayNode countries = geoLocations.putArray("countries");
            if (StringUtils.hasText(targetCountry)) {
                countries.add(targetCountry);
            }
        }

        return node.toString();
    }

    // Executa a operação buildLinkDataJson da integração Facebook Ads.
    private String buildLinkDataJson(CreateCampaignRequest.AdCreative creativeReq) {
        ObjectNode linkData = objectMapper.createObjectNode();
        if (StringUtils.hasText(creativeReq.websiteUrl())) {
            linkData.put("link", creativeReq.websiteUrl());
        }
        linkData.put("message", creativeReq.message());
        if (StringUtils.hasText(creativeReq.headline())) {
            linkData.put("name", creativeReq.headline());
        }
        if (StringUtils.hasText(creativeReq.description())) {
            linkData.put("description", creativeReq.description());
        }
        if (StringUtils.hasText(creativeReq.callToActionType())) {
            ObjectNode callToAction = linkData.putObject("call_to_action");
            callToAction.put("type", creativeReq.callToActionType());
            ObjectNode value = callToAction.putObject("value");
            if (StringUtils.hasText(creativeReq.websiteUrl())) {
                value.put("link", creativeReq.websiteUrl());
            }
            if (StringUtils.hasText(creativeReq.leadGenFormId())) {
                value.put("lead_gen_form_id", creativeReq.leadGenFormId());
            }
            if (value.isEmpty()) {
                callToAction.remove("value");
            }
        }
        return linkData.toString();
    }

    // Executa a operação toSummary da integração Facebook Ads.
    private ExperimentSummary toSummary(Experiment experiment, LeadPortalExperimentMetricsDto leadPortalMetrics) {
        String pageId = resolveExperimentPageId(experiment);
        return new ExperimentSummary(
                experiment.getId(),
                experiment.getName(),
                experiment.getHypothesis(),
                experiment.getSinglePain(),
                experiment.getFreeReward(),
                experiment.getFunnelPromise(),
                experiment.getPrimaryCta(),
                experiment.getExperimentType() != null
                        ? experiment.getExperimentType().name()
                        : null,
                experiment.getCampaignObjective() != null
                        ? experiment.getCampaignObjective().name()
                        : null,
                experiment.getFollowUpActionUrl(),
                pageId,
                experiment.getNiche() != null ? experiment.getNiche().getFacebookPixelId() : null,
                experiment.getKpiTargetCpl(),
                experiment.getDailyBudget(),
                experiment.getStartDate(),
                experiment.getEndDate(),
                experiment.getNiche() != null ? experiment.getNiche().getName() : null,
                experiment.getHypothesisRef() != null ? experiment.getHypothesisRef().getTitle() : null,
                experimentReadinessService.computeMissingConfiguration(experiment),
                toLeadPortalFlowSummary(experiment),
                toFacebookPageSummary(experiment),
                toInstagramAccountSummary(experiment),
                toInstantFormSummary(experiment),
                isNextStepInstantForm(experiment),
                buildPublicationJobId(experiment),
                toLeadPortalFunnelSummary(leadPortalMetrics),
                toMetricSummary(experiment.getCampaignMetric()),
                toCampaignStrategySummary(campaignStrategyService.findLatestByExperimentId(experiment.getId())),
                toSalesPageAbTestSummary(salesPageAbTestService.findActiveForCampaign(experiment.getId()).orElse(null)));
    }

    // Converte o teste A/B de pagina de venda para resumo consumido pelo worker Meta.
    private SalesPageAbTestSummary toSalesPageAbTestSummary(ExperimentSalesPageAbTestDto test) {
        if (test == null) {
            return null;
        }
        List<SalesPageAbVariantSummary> variants = test.variants() == null
                ? List.of()
                : test.variants().stream()
                .map(this::toSalesPageAbVariantSummary)
                .toList();
        return new SalesPageAbTestSummary(
                test.id(),
                test.name(),
                test.status() != null ? test.status().name() : null,
                test.primaryMetric(),
                test.winnerRule(),
                test.minimumRuntimeDays(),
                test.minimumSampleSize(),
                test.metaSplitTestRecommended(),
                variants);
    }

    // Converte uma variante A/B para resumo consumido pelo worker Meta.
    private SalesPageAbVariantSummary toSalesPageAbVariantSummary(ExperimentSalesPageAbVariantDto variant) {
        return new SalesPageAbVariantSummary(
                variant.id(),
                variant.variantKey(),
                variant.name(),
                variant.variantType() != null ? variant.variantType().name() : null,
                variant.status() != null ? variant.status().name() : null,
                variant.trafficWeight(),
                variant.salesPageUrl(),
                variant.checkoutUrl(),
                variant.adDestinationUrl(),
                variant.analyticsVariantParam(),
                variant.experimentVideoAssetId(),
                variant.requiredCollectorsPresent());
    }

    // Converte a estrategia de campanha para resumo consumido pela tela.
    private CampaignStrategySummary toCampaignStrategySummary(CampaignStrategy strategy) {
        if (strategy == null) {
            return null;
        }
        return new CampaignStrategySummary(
                strategy.getObjective() != null ? strategy.getObjective().name() : null,
                strategy.getPreset(),
                strategy.getMaxSpendWithoutPurchase(),
                strategy.getMinimumCheckoutRate(),
                strategy.getMinimumLinkClicks(),
                strategy.getMinimumImpressions(),
                strategy.isEnabled());
    }

    // Executa a operação resolveExperimentPageId da integração Facebook Ads.
    private String resolveExperimentPageId(Experiment experiment) {
        if (experiment.getFacebookPage() == null) {
            return null;
        }
        return experiment.getFacebookPage().getPageId();
    }

    // Executa a operação toLeadPortalFunnelSummary da integração Facebook Ads.
    private LeadPortalFunnelSummary toLeadPortalFunnelSummary(LeadPortalExperimentMetricsDto metrics) {
        if (metrics == null) {
            return null;
        }
        long submissions = metrics.uniqueLeads() != null ? metrics.uniqueLeads().size() : 0L;
        return new LeadPortalFunnelSummary(metrics.leadsAccessed(), submissions);
    }

    // Executa a operação toLeadPortalFlowSummary da integração Facebook Ads.
    private LeadPortalFlowSummary toLeadPortalFlowSummary(Experiment experiment) {
        LeadPortalFlow flow = experiment.getLeadPortalFlow();
        if (flow == null) {
            return null;
        }
        return new LeadPortalFlowSummary(
                flow.getId(),
                flow.getName(),
                flow.getSlug(),
                leadPortalPublicUrlResolver.resolve(flow)
        );
    }

    // Executa a operação toFacebookPageSummary da integração Facebook Ads.
    private FacebookPageSummary toFacebookPageSummary(Experiment experiment) {
        if (experiment.getFacebookPage() == null) {
            return null;
        }
        var page = experiment.getFacebookPage();
        Long accountId = page.getAccount() != null ? page.getAccount().getId() : null;
        return new FacebookPageSummary(page.getId(), accountId, page.getPageId(), page.getName());
    }

    // Executa a operação toInstagramAccountSummary da integração Facebook Ads.
    private InstagramAccountSummary toInstagramAccountSummary(Experiment experiment) {
        if (experiment.getInstagramAccount() == null) {
            return null;
        }
        var account = experiment.getInstagramAccount();
        return new InstagramAccountSummary(account.getId(), account.getHandle(), account.getCode(), account.getName());
    }

    // Executa a operação toInstantFormSummary da integração Facebook Ads.
    private InstantFormSummary toInstantFormSummary(Experiment experiment) {
        if (experiment.getFacebookInstantForm() == null) {
            return null;
        }
        var form = experiment.getFacebookInstantForm();
        return new InstantFormSummary(
                form.getId(),
                form.getFormId(),
                form.getName(),
                form.getStatus(),
                form.isApproved(),
                form.isPublished(),
                form.getShareLink()
        );
    }

    // Executa a operação toMetricSummary da integração Facebook Ads.
    private CampaignMetricSummary toMetricSummary(ExperimentCampaignMetric metric) {
        if (metric == null) {
            return null;
        }
        FacebookAdsCampaign campaign = metric.getCampaign();
        Instant lastSyncedAt = campaign != null ? campaign.getMetricsLastSyncedAt() : null;
        String lastSyncError = campaign != null ? campaign.getMetricsLastError() : null;
        return new CampaignMetricSummary(
                metric.getDateStart(),
                metric.getDateStop(),
                metric.getReach(),
                metric.getImpressions(),
                metric.getClicks(),
                metric.getLeads(),
                metric.getSpend(),
                metric.getCpc(),
                metric.getCpl(),
                lastSyncedAt,
                lastSyncError);
    }

    // Executa a operação isNextStepInstantForm da integração Facebook Ads.
    private boolean isNextStepInstantForm(Experiment experiment) {
        if (experiment.getJourneyTemplate() == null) {
            return false;
        }
        List<JourneyStep> steps = experiment.getJourneyTemplate().getSteps();
        if (steps == null || steps.isEmpty()) {
            return false;
        }
        List<JourneyStep> ordered = steps.stream()
                .sorted(Comparator.comparingInt(step -> step.getPosition() != null ? step.getPosition() : Integer.MAX_VALUE))
                .toList();
        JourneyStep previous = null;
        for (JourneyStep step : ordered) {
            if (previous != null && previous.getStimulusType() == JourneyStimulusType.AD) {
                return step.getStimulusType() == JourneyStimulusType.INSTANT_FORM;
            }
            previous = step;
        }
        return false;
    }

    // Registra o primeiro passo do job quando o backend disponibiliza o experimento ao worker.
    private void registerBackendDispatchStep(Experiment experiment) {
        publicationJobStepService.register(new FacebookCampaignPublicationJobStepRequest(
                buildPublicationJobId(experiment),
                experiment.getId(),
                "BACKEND_DISPATCH_EXPERIMENT_READY",
                "BACKEND",
                "/api/facebook-campaigns/experiments-ready",
                "GET",
                200,
                null,
                objectMapper.valueToTree(Map.of("experimentId", experiment.getId())),
                null,
                Instant.now()));
    }

    // Cria um hash estável para rastrear a publicação liberada ao worker como job.
    private String buildPublicationJobId(Experiment experiment) {
        String source = "%d:%s:%s".formatted(
                experiment.getId(),
                experiment.getFacebookReleaseRequestedAt(),
                experiment.getUpdatedAt());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível para criar jobId de publicação", ex);
        }
    }

    public record ExperimentSummary(
            Long id,
            String name,
            String hypothesis,
            String singlePain,
            String freeReward,
            String funnelPromise,
            String primaryCta,
            String experimentType,
            String campaignObjective,
            String followUpActionUrl,
            String pageId,
            String facebookPixelId,
            BigDecimal kpiTargetCpl,
            BigDecimal dailyBudget,
            LocalDate startDate,
            LocalDate endDate,
            String nicheName,
            String hypothesisTitle,
            List<String> missingConfiguration,
            LeadPortalFlowSummary leadPortalFlow,
            FacebookPageSummary facebookPage,
            InstagramAccountSummary instagramAccount,
            InstantFormSummary facebookInstantForm,
            boolean nextStepInstantForm,
            String publicationJobId,
            LeadPortalFunnelSummary leadPortalFunnel,
            CampaignMetricSummary metrics,
            CampaignStrategySummary campaignStrategy,
            SalesPageAbTestSummary salesPageAbTest) {}

    public record LeadPortalFlowSummary(Long id, String name, String slug, String publicUrl) {}

    public record FacebookPageSummary(Long id, Long accountId, String pageId, String name) {}

    public record InstagramAccountSummary(Long id, String handle, String code, String name) {}

    public record LeadPortalFunnelSummary(Long formAccesses, Long formSubmissions) {}

    public record InstantFormSummary(
            Long id,
            String facebookFormId,
            String name,
            String status,
            boolean approved,
            boolean published,
            String shareLink) {}

    public record CampaignMetricSummary(
            LocalDate dateStart,
            LocalDate dateStop,
            Long reach,
            Long impressions,
            Long clicks,
            Long leads,
            BigDecimal spend,
            BigDecimal cpc,
            BigDecimal cpl,
            Instant lastSyncedAt,
            String lastSyncError) {}

    public record CampaignStrategySummary(
            String objective,
            String preset,
            BigDecimal maxSpendWithoutPurchase,
            BigDecimal minimumCheckoutRate,
            Long minimumLinkClicks,
            Long minimumImpressions,
            boolean enabled) {}

    public record SalesPageAbTestSummary(
            Long id,
            String name,
            String status,
            String primaryMetric,
            String winnerRule,
            Integer minimumRuntimeDays,
            Integer minimumSampleSize,
            boolean metaSplitTestRecommended,
            List<SalesPageAbVariantSummary> variants) {}

    public record SalesPageAbVariantSummary(
            Long id,
            String variantKey,
            String name,
            String variantType,
            String status,
            BigDecimal trafficWeight,
            String salesPageUrl,
            String checkoutUrl,
            String adDestinationUrl,
            String analyticsVariantParam,
            Long experimentVideoAssetId,
            boolean requiredCollectorsPresent) {}

    public record CampaignMetricsSyncTarget(String campaignId, Long experimentId, Instant lastSyncedAt) {}

    public record CampaignMetricsUpdateRequest(
            LocalDate dateStart,
            LocalDate dateStop,
            Long reach,
            Long impressions,
            Long clicks,
            Long leads,
            BigDecimal spend) {}

    public record CampaignMetricsErrorRequest(String message) {}

    public record CampaignStatusSyncRequest(
            String status,
            String effectiveStatus,
            List<AdSetStatus> adSets,
            List<AdStatus> ads) {

        public record AdSetStatus(String id, String status, String effectiveStatus) {}

        public record AdStatus(String id, String status, String effectiveStatus) {}
    }


    /**
     * Contrato de leitura dos criativos aprovados para publicação pelo módulo Facebook.
     */
    public record FacebookCreativeConsumptionResponse(
            Long id,
            Long experimentId,
            String format,
            String headline,
            String primaryText,
            String imageUrl,
            String description,
            String cta,
            String destinationUrl,
            String leadGenFormId,
            String instagramUserId,
            String status) {}

    public record CreateCampaignRequest(
            String id,
            String externalId,
            String adAccountId,
            String name,
            String objective,
            FacebookAdStatus status,
            BudgetMode budgetMode,
            Long experimentId,
            Long facebookAccountId,
            AdSet adSet,
            AdCreative adCreative,
            Ad ad,
            List<AdCreative> adCreatives,
            List<Ad> ads) {

        public record AdSet(
                String id,
                String externalId,
                String name,
                String billingEvent,
                String optimizationGoal,
                String bidStrategy,
                String bidAmount,
                String dailyBudget,
                String lifetimeBudget,
                String targetCountry,
                String destinationType,
                String pageId,
                String pixelId,
                String customEventType,
                String targetingJson,
                String savedAudienceId,
                String savedAudienceName,
                Long experimentAdSetId,
                FacebookAdStatus status) {}

        public record AdCreative(
                String id,
                String pageId,
                String instagramActorId,
                String websiteUrl,
                String leadGenFormId,
                String message,
                String callToActionType,
                String headline,
                String description) {}

        public record Ad(
                String id,
                String externalId,
                String name,
                String adSetId,
                String creativeId,
                FacebookAdStatus status) {}
    }
}
