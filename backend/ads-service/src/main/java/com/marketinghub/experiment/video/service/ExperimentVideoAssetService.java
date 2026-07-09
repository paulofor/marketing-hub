package com.marketinghub.experiment.video.service;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.LandingPage;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.experiment.video.dto.CreateExperimentVideoAssetRequest;
import com.marketinghub.experiment.video.dto.ExperimentVideoAssetDto;
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
import com.marketinghub.salesvideo.dto.ApproveSalesVideoScriptRequest;
import com.marketinghub.salesvideo.dto.CreateSalesVideoProfileRequest;
import com.marketinghub.salesvideo.dto.RequestVideoRenderRequest;
import com.marketinghub.salesvideo.dto.SalesVideoJobDto;
import com.marketinghub.salesvideo.dto.SalesVideoProfileDto;
import com.marketinghub.salesvideo.service.SalesVideoService;
import java.util.List;
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
    private final ExperimentVideoAssetRepository repository;
    private final ExperimentRepository experimentRepository;
    private final SalesVideoProfileRepository profileRepository;
    private final SalesVideoJobRepository jobRepository;
    private final AssetRepository assetRepository;
    private final LandingVideoSlotRepository landingVideoSlotRepository;
    private final ProductRepository productRepository;
    private final LandingPageRepository landingPageRepository;
    private final SalesVideoService salesVideoService;

    /** Inicializa o serviço com os repositórios dos vínculos de experimento e vídeo. */
    public ExperimentVideoAssetService(ExperimentVideoAssetRepository repository,
                                       ExperimentRepository experimentRepository,
                                       SalesVideoProfileRepository profileRepository,
                                       SalesVideoJobRepository jobRepository,
                                       AssetRepository assetRepository,
                                       LandingVideoSlotRepository landingVideoSlotRepository,
                                       ProductRepository productRepository,
                                       LandingPageRepository landingPageRepository,
                                       SalesVideoService salesVideoService) {
        this.repository = repository;
        this.experimentRepository = experimentRepository;
        this.profileRepository = profileRepository;
        this.jobRepository = jobRepository;
        this.assetRepository = assetRepository;
        this.landingVideoSlotRepository = landingVideoSlotRepository;
        this.productRepository = productRepository;
        this.landingPageRepository = landingPageRepository;
        this.salesVideoService = salesVideoService;
    }

    /** Lista todos os vídeos registrados para um experimento. */
    @Transactional(readOnly = true)
    public List<ExperimentVideoAssetDto> list(Long experimentId) {
        ensureExperiment(experimentId);
        return repository.findByExperimentIdOrderByCreatedAtDesc(experimentId).stream()
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
                .cost(request.cost())
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
        SalesVideoJobDto job = salesVideoService.requestRender(profile.getId(), renderRequest);

        ExperimentVideoAsset videoAsset = ExperimentVideoAsset.builder()
                .experiment(experiment)
                .slot(request.slot())
                .objective(request.objective().trim())
                .primaryMetric(request.primaryMetric().trim())
                .script(request.scriptText().trim())
                .prompt(buildVeoPromptSnapshot(request))
                .provider("VEO")
                .model(Optional.ofNullable(trimToNull(request.providerName())).orElse("VEO"))
                .status(ExperimentVideoStatus.GENERATING)
                .reviewStatus(ExperimentVideoReviewStatus.PENDING)
                .requiredForRelease(request.requiredForRelease())
                .salesVideoProfile(resolveProfile(profile.getId()))
                .salesVideoJob(resolveJob(job.getId()))
                .build();
        return toDto(repository.save(videoAsset));
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

    /** Monta um resumo auditável do prompt enviado ao fluxo VEO. */
    private String buildVeoPromptSnapshot(RequestExperimentVeoVideoRequest request) {
        return """
                Provider: VEO
                Objetivo: %s
                Métrica primária: %s
                Script:
                %s
                """.formatted(request.objective().trim(), request.primaryMetric().trim(), request.scriptText().trim());
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
