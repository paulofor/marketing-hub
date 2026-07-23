package com.marketinghub.experiment.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.experiment.video.dto.CreateExperimentVideoAssetRequest;
import com.marketinghub.experiment.video.dto.ExperimentVideoAssetDto;
import com.marketinghub.experiment.video.dto.RequestPlannedExperimentVideoRenderRequest;
import com.marketinghub.experiment.video.dto.RequestExperimentVeoVideoRequest;
import com.marketinghub.experiment.video.dto.UpdateExperimentVideoAssetRequest;
import com.marketinghub.media.Asset;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.LandingPageRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.salesvideo.LandingVideoSlotRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoJobRepository;
import com.marketinghub.repository.jpa.salesvideo.SalesVideoProfileRepository;
import com.marketinghub.salesvideo.LandingVideoSlot;
import com.marketinghub.salesvideo.SalesVideoJob;
import com.marketinghub.salesvideo.SalesVideoKind;
import com.marketinghub.salesvideo.SalesVideoProfile;
import com.marketinghub.salesvideo.SalesVideoProviderFamily;
import com.marketinghub.salesvideo.SalesVideoStatus;
import com.marketinghub.salesvideo.dto.ApproveSalesVideoScriptRequest;
import com.marketinghub.salesvideo.dto.CreateSalesVideoProfileRequest;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.salesvideo.service.SalesVideoProductionCostCalculator;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gerencia vídeos como ativos comerciais rastreáveis dentro de um experimento.
 */
@Service
public class ExperimentVideoAssetService {
    private static final int LANDING_HERO_LUMA_TARGET_SECONDS = 30;

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private final ExperimentVideoAssetRepository repository;
    private final ExperimentRepository experimentRepository;
    private final SalesVideoProfileRepository profileRepository;
    private final SalesVideoJobRepository jobRepository;
    private final AssetRepository assetRepository;
    private final LandingVideoSlotRepository landingVideoSlotRepository;
    private final ProductRepository productRepository;
    private final LandingPageRepository landingPageRepository;
    private final SalesVideoService salesVideoService;
    private final SalesVideoProductionCostCalculator costCalculator;

    /** Inicializa o serviço com os repositórios dos vínculos de experimento e vídeo. */
    public ExperimentVideoAssetService(ExperimentVideoAssetRepository repository,
                                       ExperimentRepository experimentRepository,
                                       SalesVideoProfileRepository profileRepository,
                                       SalesVideoJobRepository jobRepository,
                                       AssetRepository assetRepository,
                                       LandingVideoSlotRepository landingVideoSlotRepository,
                                       ProductRepository productRepository,
                                       LandingPageRepository landingPageRepository,
                                       SalesVideoService salesVideoService,
                                       SalesVideoProductionCostCalculator costCalculator) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.assetRepository = assetRepository;
        this.landingVideoSlotRepository = landingVideoSlotRepository;
        this.productRepository = productRepository;
        this.landingPageRepository = landingPageRepository;
        this.salesVideoService = salesVideoService;
        this.costCalculator = costCalculator;
    }

    /** Lista todos os vídeos registrados para um experimento. */
    @Transactional(readOnly = true)
    public List<ExperimentVideoAssetDto> list(Long experimentId) {
        ensureExperiment(experimentId);
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId).stream()
                .map(this::toDto)
                .toList();
    }

    /** Lista todos os vídeos registrados para acompanhamento operacional. */
    @Transactional(readOnly = true)
    public List<ExperimentVideoAssetDto> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    /** Cria um vídeo de experimento com seus vínculos opcionais validados. */
    @Transactional
    public ExperimentVideoAssetDto create(Long experimentId, CreateExperimentVideoAssetRequest request) {
        Experiment experiment = ensureExperiment(experimentId);
        ExperimentVideoAsset videoAsset = ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(request.slot())
                .objective(request.objective())
                .primaryMetric(request.primaryMetric())
                .script(request.script())
                .prompt(request.prompt())
                .provider(request.provider())
                .model(request.model())
                .status(request.status() == null ? ExperimentVideoStatus.PLANNED : request.status())
                .assetUrl(request.assetUrl())
                .thumbnailUrl(request.thumbnailUrl())
                .durationSeconds(request.durationSeconds())
                .aspectRatio(request.aspectRatio())
                .requestJson(request.requestJson())
                .responseJson(request.responseJson())
                .cost(resolveCost(request.cost(), request.provider(), request.model(), request.durationSeconds(), null))
                .reviewStatus(request.reviewStatus() == null ? ExperimentVideoReviewStatus.PENDING : request.reviewStatus())
                .requiredForRelease(request.requiredForRelease())
                .salesVideoProfile(resolveProfile(request.salesVideoProfileId()))
                .salesVideoJob(resolveJob(request.salesVideoJobId()))
                .asset(resolveAsset(request.assetId()))
                .landingVideoSlot(resolveLandingVideoSlot(experimentId, request.landingVideoSlotId()))
                .build();
        return toDto(repository.save(videoAsset));
    }

    /** Orquestra pelo Marketing Hub a criação de vídeo VEO para um experimento. */
    @Transactional
    public ExperimentVideoAssetDto requestVeoRender(Long experimentId, RequestExperimentVeoVideoRequest request) {
        Experiment experiment = ensureExperiment(experimentId);
        Product product = resolveOrCreateProduct(experiment);
        Long landingPageId = resolveLandingPageId(experimentId);

        CreateSalesVideoProfileRequest profileRequest = new CreateSalesVideoProfileRequest();
        profileRequest.setVideoKind(SalesVideoKind.HERO);
        profileRequest.setTitle(request.title().trim());
        profileRequest.setPersonaName(trimToNull(request.personaName()));
        profileRequest.setPersonaStyle(trimToNull(request.personaStyle()));
        profileRequest.setVoiceStyle(trimToNull(request.voiceStyle()));
        profileRequest.setLanguage(Optional.ofNullable(trimToNull(request.language())).orElse("pt-BR"));
        profileRequest.setTargetDurationSeconds(request.targetDurationSeconds());
        profileRequest.setLandingPageId(landingPageId);
        SalesVideoProfileDto profile = salesVideoService.createProfile(product.getId(), profileRequest);

        ApproveSalesVideoScriptRequest scriptRequest = new ApproveSalesVideoScriptRequest();
        scriptRequest.setScriptText(request.scriptText().trim());
        scriptRequest.setHookText(trimToNull(request.hookText()));
        scriptRequest.setCtaText(trimToNull(request.ctaText()));
        scriptRequest.setCaptionText(trimToNull(request.captionText()));
        scriptRequest.setApprovedBy(request.requestedBy().trim());
        salesVideoService.approveScript(profile.getId(), scriptRequest);

        RequestVideoRenderRequest renderRequest = new RequestVideoRenderRequest();
        renderRequest.setRequestedBy(request.requestedBy().trim());
        renderRequest.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
        renderRequest.setProviderName(Optional.ofNullable(trimToNull(request.providerName())).orElse("VEO"));
        renderRequest.setExecutionMode(request.executionMode());
        renderRequest.setMetadataJson(buildVeoRenderMetadata(experimentId, request));
        SalesVideoJobDto job = salesVideoService.requestRender(profile.getId(), renderRequest);
        String providerName = Optional.ofNullable(trimToNull(request.providerName())).orElse("VEO");
        BigDecimal estimatedCost = costCalculator.estimateUsd(
                providerName,
                providerName,
                request.targetDurationSeconds(),
                "720p");

        ExperimentVideoAsset videoAsset = ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(request.slot())
                .objective(request.objective().trim())
                .primaryMetric(request.primaryMetric().trim())
                .script(request.scriptText().trim())
                .prompt(buildVeoPromptSnapshot(request))
                .provider("VEO")
                .model(providerName)
                .status(ExperimentVideoStatus.GENERATING)
                .durationSeconds(request.targetDurationSeconds())
                .cost(estimatedCost)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(request.requiredForRelease())
                .salesVideoProfile(resolveProfile(profile.getId()))
                .salesVideoJob(resolveJob(job.getId()))
                .build();
        return toDto(repository.save(videoAsset));
    }

    /** Cria jobs de render para ativos planejados e preserva o vínculo original do experimento. */
    @Transactional
    public List<ExperimentVideoAssetDto> requestPlannedRenders(
            Long experimentId,
            RequestPlannedExperimentVideoRenderRequest request) {
        Experiment experiment = ensureExperiment(experimentId);
        Product product = resolveOrCreateProduct(experiment);
        Long landingPageId = resolveLandingPageId(experimentId);
        List<ExperimentVideoAsset> plannedAssets = repository.findByExperimentIdOrderByCreatedAtDesc(experimentId)
                .stream()
                .filter(this::requiresRenderJob)
                .toList();
        if (plannedAssets.isEmpty()) {
            return List.of();
        }
        return plannedAssets.stream()
                .map(videoAsset -> requestRenderForPlannedAsset(videoAsset, product, landingPageId, request))
                .map(repository::save)
                .map(this::toDto)
                .toList();
    }

    /** Atualiza um vídeo de experimento sem perder os campos não enviados. */
    @Transactional
    public ExperimentVideoAssetDto update(Long experimentId, Long videoAssetId, UpdateExperimentVideoAssetRequest request) {
        ensureExperiment(experimentId);
        ExperimentVideoAsset videoAsset = repository.findById(videoAssetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "video asset not found"));
        if (videoAsset.getExperiment() == null || !experimentId.equals(videoAsset.getExperiment().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "video asset not found for experiment");
        }
        applyUpdate(videoAsset, request, experimentId);
        return toDto(repository.save(videoAsset));
    }

    /** Verifica se existe vídeo obrigatório ainda sem status READY e revisão APPROVED. */
    @Transactional(readOnly = true)
    public boolean hasRequiredVideoBlockingRelease(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return repository.existsRequiredReleaseBlocker(
                experimentId,
                ExperimentVideoStatus.READY,
                ExperimentVideoReviewStatus.APPROVED);
    }

    /** Verifica se existe vídeo pronto e aprovado para uma pagina/funil que depende de vídeo. */
    @Transactional(readOnly = true)
    public boolean hasReadyApprovedVideo(Long experimentId) {
        if (experimentId == null) {
            return false;
        }
        return repository.existsByExperimentIdAndStatusAndReviewStatus(
                experimentId,
                ExperimentVideoStatus.READY,
                ExperimentVideoReviewStatus.APPROVED);
    }

    /** Aplica os campos opcionais enviados na atualização do ativo de vídeo. */
    private void applyUpdate(ExperimentVideoAsset videoAsset, UpdateExperimentVideoAssetRequest request, Long experimentId) {
        if (request.slot() != null) {
            videoAsset.setSlot(request.slot());
        }
        if (request.objective() != null) {
            videoAsset.setObjective(request.objective());
        }
        if (request.primaryMetric() != null) {
            videoAsset.setPrimaryMetric(request.primaryMetric());
        }
        if (request.script() != null) {
            videoAsset.setScript(request.script());
        }
        if (request.prompt() != null) {
            videoAsset.setPrompt(request.prompt());
        }
        if (request.provider() != null) {
            videoAsset.setProvider(request.provider());
        }
        if (request.model() != null) {
            videoAsset.setModel(request.model());
        }
        if (request.status() != null) {
            videoAsset.setStatus(request.status());
        }
        if (request.assetUrl() != null) {
            videoAsset.setAssetUrl(request.assetUrl());
        }
        if (request.thumbnailUrl() != null) {
            videoAsset.setThumbnailUrl(request.thumbnailUrl());
        }
        if (request.durationSeconds() != null) {
            videoAsset.setDurationSeconds(request.durationSeconds());
        }
        if (request.aspectRatio() != null) {
            videoAsset.setAspectRatio(request.aspectRatio());
        }
        if (request.requestJson() != null) {
            videoAsset.setRequestJson(request.requestJson());
        }
        if (request.responseJson() != null) {
            videoAsset.setResponseJson(request.responseJson());
        }
        if (request.cost() != null) {
            videoAsset.setCost(request.cost());
        } else if (request.durationSeconds() != null || request.provider() != null || request.model() != null) {
            BigDecimal estimatedCost = resolveCost(
                    null,
                    videoAsset.getProvider(),
                    videoAsset.getModel(),
                    videoAsset.getDurationSeconds(),
                    null);
            if (estimatedCost != null) {
                videoAsset.setCost(estimatedCost);
            }
        }
        if (request.reviewStatus() != null) {
            videoAsset.setReviewStatus(request.reviewStatus());
        }
        if (request.requiredForRelease() != null) {
            videoAsset.setRequiredForRelease(request.requiredForRelease());
        }
        if (request.salesVideoProfileId() != null) {
            videoAsset.setSalesVideoProfile(resolveProfile(request.salesVideoProfileId()));
        }
        if (request.salesVideoJobId() != null) {
            videoAsset.setSalesVideoJob(resolveJob(request.salesVideoJobId()));
        }
        if (request.assetId() != null) {
            videoAsset.setAsset(resolveAsset(request.assetId()));
        }
        if (request.landingVideoSlotId() != null) {
            videoAsset.setLandingVideoSlot(resolveLandingVideoSlot(experimentId, request.landingVideoSlotId()));
        }
    }

    /** Busca o experimento alvo ou falha com resposta HTTP clara. */
    private Experiment ensureExperiment(Long experimentId) {
        return experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "experiment not found"));
    }

    /** Reusa o produto operacional do nicho ou cria um mínimo para suportar o vídeo. */
    private Product resolveOrCreateProduct(Experiment experiment) {
        Long nicheId = experiment.getNiche() != null ? experiment.getNiche().getId() : null;
        if (nicheId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "experiment niche not found");
        }
        return productRepository.findFirstByMarketNiche_IdOrderByCreatedAtDesc(nicheId)
                .orElseGet(() -> productRepository.save(Product.builder()
                        .marketNiche(experiment.getNiche())
                        .niche(experiment.getNiche().getName())
                        .avatar(experiment.getName())
                        .explicitPain(experiment.getSinglePain())
                        .promise(resolveProductPromise(experiment))
                        .build()));
    }

    /** Resolve a promessa comercial mínima do produto operacional. */
    private String resolveProductPromise(Experiment experiment) {
        if (StringUtils.hasText(experiment.getFunnelPromise())) {
            return experiment.getFunnelPromise();
        }
        if (StringUtils.hasText(experiment.getHypothesis())) {
            return experiment.getHypothesis();
        }
        return experiment.getName();
    }

    /** Seleciona a landing do experimento quando já existir publicação registrada. */
    private Long resolveLandingPageId(Long experimentId) {
        return landingPageRepository.findByExperimentId(experimentId).stream()
                .findFirst()
                .map(LandingPage::getId)
                .orElse(null);
    }

    /** Solicita render no módulo SalesVideo usando o roteiro e prompt já planejados no ativo. */
    private ExperimentVideoAsset requestRenderForPlannedAsset(ExperimentVideoAsset videoAsset,
                                                              Product product,
                                                              Long landingPageId,
                                                              RequestPlannedExperimentVideoRenderRequest request) {
        String scriptText = trimToNull(videoAsset.getScript());
        if (scriptText == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planned video asset script is required");
        }
        String providerName = Optional.ofNullable(trimToNull(videoAsset.getProvider())).orElse("LUMA_RAY_3_2");
        Integer targetDurationSeconds = resolvePlannedRenderTargetDurationSeconds(videoAsset, providerName);
        Long experimentId = videoAsset.getExperiment() != null ? videoAsset.getExperiment().getId() : null;
        String title = "Vídeo planejado #%d - experimento %d".formatted(videoAsset.getId(), experimentId);

        CreateSalesVideoProfileRequest profileRequest = new CreateSalesVideoProfileRequest();
        profileRequest.setVideoKind(SalesVideoKind.HERO);
        profileRequest.setTitle(title);
        profileRequest.setPersonaName(trimToNull(request.personaName()));
        profileRequest.setPersonaStyle(trimToNull(request.personaStyle()));
        profileRequest.setVoiceStyle(trimToNull(request.voiceStyle()));
        profileRequest.setLanguage(Optional.ofNullable(trimToNull(request.language())).orElse("pt-BR"));
        profileRequest.setTargetDurationSeconds(targetDurationSeconds);
        profileRequest.setLandingPageId(landingPageId);
        SalesVideoProfileDto profile = salesVideoService.createProfile(product.getId(), profileRequest);

        ApproveSalesVideoScriptRequest scriptRequest = new ApproveSalesVideoScriptRequest();
        scriptRequest.setScriptText(scriptText);
        scriptRequest.setApprovedBy(request.requestedBy().trim());
        salesVideoService.approveScript(profile.getId(), scriptRequest);

        RequestVideoRenderRequest renderRequest = new RequestVideoRenderRequest();
        renderRequest.setRequestedBy(request.requestedBy().trim());
        renderRequest.setProviderFamily(SalesVideoProviderFamily.EXTERNAL_VIDEO_MODULE);
        renderRequest.setProviderName(providerName);
        renderRequest.setExecutionMode(request.executionMode());
        renderRequest.setMetadataJson(buildPlannedRenderMetadata(videoAsset, targetDurationSeconds, request));
        SalesVideoJobDto job = salesVideoService.requestRender(profile.getId(), renderRequest);

        videoAsset.setSalesVideoProfile(resolveProfile(profile.getId()));
        videoAsset.setSalesVideoJob(resolveJob(job.getId()));
        videoAsset.setProvider(providerName);
        videoAsset.setModel(Optional.ofNullable(trimToNull(videoAsset.getModel())).orElse(providerName));
        videoAsset.setStatus(ExperimentVideoStatus.GENERATING);
        videoAsset.setDurationSeconds(targetDurationSeconds);
        videoAsset.setReviewStatus(ExperimentVideoReviewStatus.PENDING);
        if (request.requiredForRelease() != null) {
            videoAsset.setRequiredForRelease(request.requiredForRelease());
        }
        BigDecimal estimatedCost = resolveCost(
                videoAsset.getCost(),
                videoAsset.getProvider(),
                videoAsset.getModel(),
                videoAsset.getDurationSeconds(),
                null);
        if (estimatedCost != null) {
            videoAsset.setCost(estimatedCost);
        }
        return videoAsset;
    }

    /** Normaliza a duração comercial do render planejado conforme o papel do vídeo no funil. */
    private Integer resolvePlannedRenderTargetDurationSeconds(ExperimentVideoAsset videoAsset, String providerName) {
        Integer plannedDurationSeconds = videoAsset.getDurationSeconds();
        boolean lumaHero = videoAsset.getSlot() == ExperimentVideoSlot.LANDING_HERO
                && isLumaProvider(providerName);
        if (lumaHero && (plannedDurationSeconds == null || plannedDurationSeconds < 25)) {
            return LANDING_HERO_LUMA_TARGET_SECONDS;
        }
        return plannedDurationSeconds;
    }

    /** Identifica providers Luma para aplicar a estratégia comercial de hero premium. */
    private boolean isLumaProvider(String providerName) {
        String normalizedProviderName = Optional.ofNullable(trimToNull(providerName)).orElse("").toUpperCase();
        return normalizedProviderName.contains("LUMA") || normalizedProviderName.contains("RAY_3_2");
    }

    /** Identifica ativos planejados ou falhados que podem receber novo job sem duplicar o ativo. */
    private boolean requiresRenderJob(ExperimentVideoAsset videoAsset) {
        if (videoAsset.getStatus() == ExperimentVideoStatus.PLANNED && videoAsset.getSalesVideoJob() == null) {
            return true;
        }
        SalesVideoJob currentJob = videoAsset.getSalesVideoJob();
        return currentJob != null
                && currentJob.getStatus() == SalesVideoStatus.VIDEO_FAILED
                && (videoAsset.getStatus() == ExperimentVideoStatus.GENERATING
                        || videoAsset.getStatus() == ExperimentVideoStatus.FAILED);
    }

    /** Monta um resumo auditável do prompt enviado ao fluxo VEO. */
    private String buildVeoPromptSnapshot(RequestExperimentVeoVideoRequest request) {
        return """
                Provider: VEO
                Objetivo: %s
                Métrica primária: %s
                Imagem da personagem OpenAI:
                Modelo: %s
                Job: %s
                Asset: %s
                Referência: %s
                Prompt:
                %s

                Script:
                %s
                """.formatted(
                request.objective().trim(),
                request.primaryMetric().trim(),
                Optional.ofNullable(trimToNull(request.characterImageModel())).orElse("OPENAI_IMAGE"),
                Optional.ofNullable(trimToNull(request.characterImageJobId())).orElse("pendente"),
                request.characterImageAssetId() != null ? request.characterImageAssetId() : "pendente",
                Optional.ofNullable(trimToNull(request.characterImageReferenceUrl())).orElse("pendente"),
                Optional.ofNullable(trimToNull(request.characterImagePrompt())).orElse("nao informado"),
                request.scriptText().trim());
    }

    /** Monta metadados estruturados para o worker de vídeo reaproveitar a personagem gerada por OpenAI. */
    private String buildVeoRenderMetadata(Long experimentId, RequestExperimentVeoVideoRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("artifactType", "experiment.veoRenderRequest.v1");
        metadata.put("experimentId", experimentId);
        metadata.put("videoProvider", Optional.ofNullable(trimToNull(request.providerName())).orElse("VEO"));
        metadata.put("characterImageProvider", "OPENAI");
        metadata.put("characterImageModel", trimToNull(request.characterImageModel()));
        metadata.put("characterImageJobId", trimToNull(request.characterImageJobId()));
        metadata.put("characterImageAssetId", request.characterImageAssetId());
        metadata.put("characterImageReferenceUrl", trimToNull(request.characterImageReferenceUrl()));
        metadata.put("characterImagePrompt", trimToNull(request.characterImagePrompt()));
        metadata.put("scriptText", request.scriptText().trim());
        metadata.put("hookText", trimToNull(request.hookText()));
        metadata.put("ctaText", trimToNull(request.ctaText()));
        metadata.put("captionText", trimToNull(request.captionText()));
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "video metadata serialization failed", ex);
        }
    }

    /** Monta metadados estruturados para renderizar um ativo planejado existente. */
    private String buildPlannedRenderMetadata(ExperimentVideoAsset videoAsset,
                                              Integer targetDurationSeconds,
                                              RequestPlannedExperimentVideoRenderRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("artifactType", "experiment.plannedVideoRenderRequest.v1");
        metadata.put("experimentId", videoAsset.getExperiment() != null ? videoAsset.getExperiment().getId() : null);
        metadata.put("experimentVideoAssetId", videoAsset.getId());
        metadata.put("slot", videoAsset.getSlot());
        metadata.put("provider", trimToNull(videoAsset.getProvider()));
        metadata.put("model", trimToNull(videoAsset.getModel()));
        metadata.put("durationSeconds", targetDurationSeconds);
        metadata.put("commercialStrategy", buildCommercialStrategyMetadata(videoAsset));
        metadata.put("aspectRatio", trimToNull(videoAsset.getAspectRatio()));
        metadata.put("objective", trimToNull(videoAsset.getObjective()));
        metadata.put("primaryMetric", trimToNull(videoAsset.getPrimaryMetric()));
        metadata.put("prompt", trimToNull(videoAsset.getPrompt()));
        metadata.put("scriptText", trimToNull(videoAsset.getScript()));
        metadata.put("requestedBy", request.requestedBy().trim());
        try {
            return OBJECT_MAPPER.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "planned video metadata serialization failed",
                    ex);
        }
    }

    /** Descreve nos metadados como o ativo deve ser usado para maximizar venda no funil. */
    private Map<String, Object> buildCommercialStrategyMetadata(ExperimentVideoAsset videoAsset) {
        Map<String, Object> strategy = new LinkedHashMap<>();
        boolean landingHero = videoAsset.getSlot() == ExperimentVideoSlot.LANDING_HERO;
        boolean luma = isLumaProvider(videoAsset.getProvider());
        if (landingHero && luma) {
            strategy.put("funnelRole", "LANDING_HERO");
            strategy.put("recommendedUse", "hero principal da landing para atravessar dor, mecanismo, desejo e CTA");
            strategy.put("recommendedPrimaryDurationSeconds", LANDING_HERO_LUMA_TARGET_SECONDS);
            strategy.put("recommendedPaidTrafficDerivativeSeconds", List.of(10, 15));
            strategy.put("approvalRule", "aprovar um hero principal e usar os demais como variacoes ou base para cortes curtos");
            strategy.put("salesJourney", "desconhecimento -> relevancia -> mecanismo -> desejo -> compra");
        } else if (videoAsset.getSlot() == ExperimentVideoSlot.AD) {
            strategy.put("funnelRole", "PAID_TRAFFIC_HOOK");
            strategy.put("recommendedUse", "criativo curto para captar atencao e levar para a landing");
            strategy.put("recommendedPrimaryDurationSeconds", 15);
            strategy.put("salesJourney", "desconhecimento -> relevancia");
        } else {
            strategy.put("funnelRole", videoAsset.getSlot() != null ? videoAsset.getSlot().name() : "EXPERIMENT_VIDEO");
            strategy.put("recommendedUse", "apoio de funil conforme contexto do experimento");
            strategy.put("salesJourney", "reduzir incerteza e esforco percebido");
        }
        return strategy;
    }

    /** Resolve o custo em USD informado ou calculado pela tabela oficial do provider. */
    private BigDecimal resolveCost(BigDecimal informedCost,
                                   String provider,
                                   String model,
                                   Integer durationSeconds,
                                   String resolution) {
        if (informedCost != null) {
            return informedCost;
        }
        return costCalculator.estimateUsd(provider, model, durationSeconds, resolution);
    }

    /** Normaliza textos opcionais em branco para nulo. */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Busca o perfil de vídeo quando informado. */
    private SalesVideoProfile resolveProfile(Long profileId) {
        if (profileId == null) {
            return null;
        }
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "sales video profile not found"));
    }

    /** Busca o job de vídeo quando informado. */
    private SalesVideoJob resolveJob(Long jobId) {
        if (jobId == null) {
            return null;
        }
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "sales video job not found"));
    }

    /** Busca o asset final quando informado. */
    private Asset resolveAsset(Long assetId) {
        if (assetId == null) {
            return null;
        }
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "asset not found"));
    }

    /** Busca o slot de landing e impede vínculo com outro experimento. */
    private LandingVideoSlot resolveLandingVideoSlot(Long experimentId, Long slotId) {
        if (slotId == null) {
            return null;
        }
        LandingVideoSlot slot = landingVideoSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "landing video slot not found"));
        LandingPage landingPage = slot.getLandingPage();
        Long slotExperimentId = landingPage != null && landingPage.getExperiment() != null
                ? landingPage.getExperiment().getId()
                : null;
        if (!experimentId.equals(slotExperimentId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "landing video slot belongs to another experiment");
        }
        return slot;
    }

    /** Converte a entidade para o contrato de leitura da API. */
    private ExperimentVideoAssetDto toDto(ExperimentVideoAsset videoAsset) {
        return new ExperimentVideoAssetDto(
                videoAsset.getId(),
                videoAsset.getExperiment() != null ? videoAsset.getExperiment().getId() : null,
                videoAsset.getSlot(),
                videoAsset.getObjective(),
                videoAsset.getPrimaryMetric(),
                videoAsset.getScript(),
                videoAsset.getPrompt(),
                videoAsset.getProvider(),
                videoAsset.getModel(),
                videoAsset.getStatus(),
                videoAsset.getAssetUrl(),
                videoAsset.getThumbnailUrl(),
                videoAsset.getDurationSeconds(),
                videoAsset.getAspectRatio(),
                videoAsset.getRequestJson(),
                videoAsset.getResponseJson(),
                videoAsset.getCost(),
                videoAsset.getReviewStatus(),
                videoAsset.isRequiredForRelease(),
                videoAsset.getSalesVideoProfile() != null ? videoAsset.getSalesVideoProfile().getId() : null,
                videoAsset.getSalesVideoJob() != null ? videoAsset.getSalesVideoJob().getId() : null,
                videoAsset.getAsset() != null ? videoAsset.getAsset().getId() : null,
                videoAsset.getLandingVideoSlot() != null ? videoAsset.getLandingVideoSlot().getId() : null,
                videoAsset.getCreatedAt(),
                videoAsset.getUpdatedAt());
    }
}
