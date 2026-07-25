package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.*;
import com.marketinghub.creative.dto.AssetUploadResponse;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.dto.CreativeVideoReviewDto;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.creative.label.VisualProofRepository;
import com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import com.marketinghub.storage.StorageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Responsabilidade: centralizar as operações de criativos vinculados a experimentos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreativeService {
    private static final int META_CALL_TO_ACTION_MAX_LENGTH = 32;
    private static final String DEFAULT_META_CALL_TO_ACTION = "LEARN_MORE";

    private final CreativeRepository repository;
    private final ExperimentRepository experimentRepository;
    private final AngleRepository angleRepository;
    private final VisualProofRepository visualProofRepository;
    private final EmotionalTriggerRepository emotionalTriggerRepository;
    private final AssetRepository assetRepository;
    private final ExperimentVideoAssetRepository experimentVideoAssetRepository;
    private final HttpClient httpClient;
    private final CostAttributionService costAttributionService;
    private final AssetStorageService assetStorageService;
    private final ObjectMapper objectMapper;

    /**
     * Cria e persiste um criativo para o experimento informado.
     */
    @Transactional
    public Creative create(Long experimentId, CreateCreativeRequest request) {
        try {
            Experiment exp = experimentRepository.findById(experimentId).orElseThrow();
            validateReadyCreativeHasImage(request);
            Creative creative = Creative.builder()
                    .experiment(exp)
                    .format(request.getFormat())
                    .headline(request.getHeadline())
                    .primaryText(request.getPrimaryText())
                    .imageUrl(request.getImageUrl())
                    .videoId(request.getVideoId())
                    .videoUrl(request.getVideoUrl())
                    .costUsd(request.getCostUsd())
                    .description(request.getDescription())
                    .cta(normalizeMetaCallToAction(request.getCta()))
                    .destinationUrl(request.getDestinationUrl())
                    .leadGenFormId(request.getLeadGenFormId())
                    .instagramUserId(request.getInstagramUserId())
                    .status(request.getStatus())
                    .rejectionReason(null)
                    .build();
            Creative saved = repository.save(creative);
            applyGenerationCost(exp, request.getCostUsd());
            refreshExperimentApproval(exp);
            return saved;
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao criar criativo no backend. classe={} operacao=createCreative experimentId={} "
                            + "requestFormat={} requestStatus={} headline='{}' imageUrl='{}' erro='{}'",
                    getClass().getSimpleName(),
                    experimentId,
                    request != null ? request.getFormat() : null,
                    request != null ? request.getStatus() : null,
                    sanitizeForLog(request != null ? request.getHeadline() : null),
                    sanitizeForLog(request != null ? request.getImageUrl() : null),
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    /**
     * Atualiza um criativo existente.
     */
    @Transactional
    public Creative update(Long id, CreateCreativeRequest request) {
        Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
        validateReadyCreativeHasImage(request);
        creative.setFormat(request.getFormat());
        creative.setHeadline(request.getHeadline());
        creative.setPrimaryText(request.getPrimaryText());
        creative.setImageUrl(request.getImageUrl());
        creative.setVideoId(request.getVideoId());
        creative.setVideoUrl(request.getVideoUrl());
        creative.setCostUsd(request.getCostUsd());
        creative.setDescription(request.getDescription());
        creative.setCta(normalizeMetaCallToAction(request.getCta()));
        creative.setDestinationUrl(request.getDestinationUrl());
        creative.setLeadGenFormId(request.getLeadGenFormId());
        creative.setInstagramUserId(request.getInstagramUserId());
        creative.setStatus(request.getStatus());
        creative.setRejectionReason(null);
        Creative saved = repository.save(creative);
        refreshExperimentApproval(saved.getExperiment());
        return saved;
    }

    /**
     * Lista criativos de vídeo publicáveis para aprovação operacional.
     */
    public List<CreativeVideoReviewDto> listVideoReviewQueue(CreativeStatus status) {
        List<Creative> creatives = status == null
                ? repository.findVideoCreativesForReview()
                : repository.findVideoCreativesForReviewByStatus(status);
        ExperimentVideoReviewStatus reviewStatus = toExperimentVideoReviewStatus(status);
        List<ExperimentVideoAsset> experimentVideos = reviewStatus == null
                ? experimentVideoAssetRepository.findReadyExperimentVideosForReview(ExperimentVideoStatus.READY)
                : experimentVideoAssetRepository.findReadyExperimentVideosForReviewByReviewStatus(
                        ExperimentVideoStatus.READY,
                        reviewStatus);
        return java.util.stream.Stream.concat(
                        creatives.stream().map(this::toVideoReviewDto),
                        experimentVideos.stream().map(this::toVideoReviewDto))
                .sorted(Comparator
                        .comparing(CreativeVideoReviewDto::experimentId,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CreativeVideoReviewDto::id,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /**
     * Atualiza a revisão de um item da fila única de vídeos pela origem persistida.
     */
    @Transactional
    public CreativeVideoReviewDto updateVideoReviewStatus(CreativeVideoReviewSourceType sourceType,
                                                          Long id,
                                                          CreativeStatus status,
                                                          String rejectionReason) {
        if (sourceType == null || sourceType == CreativeVideoReviewSourceType.CREATIVE) {
            return toVideoReviewDto(updateStatus(id, status, rejectionReason));
        }
        return updateExperimentVideoReviewStatus(id, status, rejectionReason);
    }

    /**
     * Atualiza somente o status de revisão do criativo.
     */
    @Transactional
    public Creative updateStatus(Long id, CreativeStatus status) {
        return updateStatus(id, status, null);
    }

    /**
     * Atualiza o status de revisão do criativo e persiste o motivo quando ele for reprovado.
     */
    @Transactional
    public Creative updateStatus(Long id, CreativeStatus status, String rejectionReason) {
        try {
            if (status == null) {
                throw new IllegalArgumentException("Status do criativo é obrigatório.");
            }
            Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
            validateReadyCreativeHasMedia(creative, status);
            String normalizedRejectionReason = normalizeRejectionReason(status, rejectionReason);
            creative.setStatus(status);
            creative.setRejectionReason(normalizedRejectionReason);
            applyCreativeReviewTimestamp(creative, status);
            Creative saved = repository.save(creative);
            refreshExperimentApproval(saved.getExperiment());
            return saved;
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao atualizar status do criativo no backend. classe={} operacao=updateCreativeStatus "
                            + "creativeId={} status={} rejectionReasonPresent={} erro='{}'",
                    getClass().getSimpleName(),
                    id,
                    status,
                    StringUtils.hasText(rejectionReason),
                    ex.getMessage(),
                    ex);
            throw ex;
        }
    }

    /**
     * Normaliza e valida o motivo obrigatório para reprovação comercial.
     */
    private String normalizeRejectionReason(CreativeStatus status, String rejectionReason) {
        if (status != CreativeStatus.REJECTED) {
            return null;
        }
        if (!StringUtils.hasText(rejectionReason)) {
            throw new IllegalArgumentException("Informe o motivo da reprovação do vídeo.");
        }
        return rejectionReason.trim();
    }

    /**
     * Carimba a data da decisão humana ou limpa o carimbo quando o item volta para revisão.
     */
    private void applyCreativeReviewTimestamp(Creative creative, CreativeStatus status) {
        if (status == CreativeStatus.READY || status == CreativeStatus.REJECTED) {
            creative.setReviewedAt(Instant.now());
            return;
        }
        creative.setReviewedAt(null);
    }

    /**
     * Atualiza a revisão humana de um vídeo de experimento e preserva o motivo da reprovação.
     */
    private CreativeVideoReviewDto updateExperimentVideoReviewStatus(Long id,
                                                                     CreativeStatus status,
                                                                     String rejectionReason) {
        if (status == null) {
            throw new IllegalArgumentException("Status do vídeo é obrigatório.");
        }
        ExperimentVideoAsset videoAsset = experimentVideoAssetRepository.findById(id).orElseThrow();
        if (status == CreativeStatus.READY && !hasPublicExperimentVideoUrl(videoAsset)) {
            throw new IllegalArgumentException("Vídeo de experimento aprovado precisa ter URL pública.");
        }
        if (status == CreativeStatus.READY && !Boolean.TRUE.equals(videoAsset.getHasAudio())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vídeo de experimento aprovado precisa ter áudio validado.");
        }
        if (status == CreativeStatus.READY) {
            validateExperimentVideoVisualSourceDiversity(videoAsset);
        }
        ExperimentVideoReviewStatus reviewStatus = toExperimentVideoReviewStatus(status);
        if (reviewStatus == ExperimentVideoReviewStatus.REJECTED && !StringUtils.hasText(rejectionReason)) {
            throw new IllegalArgumentException("Informe o motivo da reprovação do vídeo.");
        }
        videoAsset.setReviewStatus(Objects.requireNonNull(reviewStatus));
        videoAsset.setReviewedBy("Marketing Hub");
        videoAsset.setReviewedAt(reviewStatus == ExperimentVideoReviewStatus.PENDING ? null : Instant.now());
        if (reviewStatus == ExperimentVideoReviewStatus.REJECTED) {
            videoAsset.setRejectionReason(rejectionReason.trim());
        } else if (reviewStatus == ExperimentVideoReviewStatus.APPROVED) {
            videoAsset.setRejectionReason(null);
        }
        return toVideoReviewDto(experimentVideoAssetRepository.save(videoAsset));
    }

    /**
     * Bloqueia aprovação de anúncio e hero com a mesma origem visual sem justificativa comercial.
     */
    private void validateExperimentVideoVisualSourceDiversity(ExperimentVideoAsset videoAsset) {
        Experiment experiment = videoAsset.getExperiment();
        String visualSourceKey = normalizeVisualSourceKey(videoAsset.getVisualSourceKey());
        if (experiment == null || experiment.getId() == null || visualSourceKey == null || !isAdOrHero(videoAsset.getSlot())) {
            return;
        }
        boolean hasConflictingSlot = experimentVideoAssetRepository.findByExperimentIdAndVisualSourceKey(
                        experiment.getId(),
                        visualSourceKey)
                .stream()
                .filter(candidate -> videoAsset.getId() == null || !videoAsset.getId().equals(candidate.getId()))
                .filter(candidate -> candidate.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED)
                .anyMatch(candidate -> isAdHeroPair(videoAsset.getSlot(), candidate.getSlot()));
        if (hasConflictingSlot && !StringUtils.hasText(videoAsset.getVisualSimilarityOverrideReason())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aprovação bloqueada: vídeo de campanha e hero do PDE usam a mesma origem visual. "
                            + "Informe uma justificativa de exceção ou gere uma variação visual distinta.");
        }
    }

    /**
     * Identifica slots que exigem diversidade visual entre mídia paga e hero de página.
     */
    private boolean isAdOrHero(ExperimentVideoSlot slot) {
        return slot == ExperimentVideoSlot.AD || slot == ExperimentVideoSlot.LANDING_HERO;
    }

    /**
     * Verifica se dois slots formam colisão entre anúncio e hero de landing.
     */
    private boolean isAdHeroPair(ExperimentVideoSlot currentSlot, ExperimentVideoSlot candidateSlot) {
        return (currentSlot == ExperimentVideoSlot.AD && candidateSlot == ExperimentVideoSlot.LANDING_HERO)
                || (currentSlot == ExperimentVideoSlot.LANDING_HERO && candidateSlot == ExperimentVideoSlot.AD);
    }

    /**
     * Mantem o CTA compatível com a coluna e com o tipo canônico aceito pela Meta.
     */
    private String normalizeMetaCallToAction(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        if (normalized.length() <= META_CALL_TO_ACTION_MAX_LENGTH) {
            return normalized;
        }
        return DEFAULT_META_CALL_TO_ACTION;
    }

    /**
     * Remove um criativo e atualiza o estado de aprovação do experimento.
     */
    @Transactional
    public void delete(Long id) {
        Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
        Experiment experiment = creative.getExperiment();
        repository.delete(creative);
        refreshExperimentApproval(experiment);
    }

    /**
     * Lista os criativos vinculados ao experimento informado.
     */
    public Iterable<Creative> listByExperiment(Long experimentId) {
        return repository.findByExperimentId(experimentId);
    }

    /**
     * Atualiza os rótulos comerciais do criativo.
     */
    @Transactional
    public Creative updateLabels(Long id, Long angleId,
                                 Long proofId,
                                 Long triggerId) {
        Creative creative = repository.findById(id).orElseThrow();
        if (angleId != null) {
            creative.setAngles(java.util.Set.of(angleRepository.findById(angleId).orElseThrow()));
        }
        if (proofId != null) {
            creative.setVisualProofs(java.util.Set.of(visualProofRepository.findById(proofId).orElseThrow()));
        }
        if (triggerId != null) {
            creative.setEmotionalTriggers(java.util.Set.of(emotionalTriggerRepository.findById(triggerId).orElseThrow()));
        }
        return creative;
    }

    /**
     * Recalcula se o experimento possui criativos aprovados.
     */
    private void refreshExperimentApproval(Experiment experiment) {
        boolean hasApprovedCreatives = repository.existsByExperimentIdAndStatusAndUsableMedia(
                experiment.getId(), CreativeStatus.READY);
        experiment.setCreativeApproved(hasApprovedCreatives);
        experimentRepository.save(experiment);
    }

    /**
     * Impede que criativo aprovado siga sem mídia compatível com o formato escolhido.
     */
    private void validateReadyCreativeHasImage(CreateCreativeRequest request) {
        if (request == null || request.getStatus() != CreativeStatus.READY) {
            return;
        }
        String format = StringUtils.hasText(request.getFormat()) ? request.getFormat().trim() : "IMAGE";
        if ("IMAGE".equalsIgnoreCase(format) && !StringUtils.hasText(request.getImageUrl())) {
            throw new IllegalArgumentException("Criativo de imagem aprovado precisa ter imagem gerada.");
        }
        if ("VIDEO".equalsIgnoreCase(format)
                && !StringUtils.hasText(request.getVideoId())
                && !StringUtils.hasText(request.getVideoUrl())) {
            throw new IllegalArgumentException("Criativo de vídeo aprovado precisa ter videoId da Meta ou videoUrl público.");
        }
    }

    /**
     * Impede aprovação por status quando a mídia do criativo ainda não é publicável.
     */
    private void validateReadyCreativeHasMedia(Creative creative, CreativeStatus status) {
        if (status != CreativeStatus.READY) {
            return;
        }
        String format = StringUtils.hasText(creative.getFormat()) ? creative.getFormat().trim() : "IMAGE";
        if ("IMAGE".equalsIgnoreCase(format) && !StringUtils.hasText(creative.getImageUrl())) {
            throw new IllegalArgumentException("Criativo de imagem aprovado precisa ter imagem gerada.");
        }
        if ("VIDEO".equalsIgnoreCase(format)
                && !StringUtils.hasText(creative.getVideoId())
                && !StringUtils.hasText(creative.getVideoUrl())) {
            throw new IllegalArgumentException("Criativo de vídeo aprovado precisa ter videoId da Meta ou videoUrl público.");
        }
    }

    /**
     * Converte o criativo de vídeo em contrato de revisão com hipótese, nicho e dados de custo.
     */
    private CreativeVideoReviewDto toVideoReviewDto(Creative creative) {
        Experiment experiment = creative.getExperiment();
        Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
        MarketNiche niche = hypothesis != null && hypothesis.getMarketNiche() != null
                ? hypothesis.getMarketNiche()
                : experiment != null ? experiment.getNiche() : null;
        return new CreativeVideoReviewDto(
                creative.getId(),
                CreativeVideoReviewSourceType.CREATIVE,
                ExperimentVideoSlot.AD,
                experiment != null ? experiment.getId() : null,
                experiment != null ? experiment.getName() : null,
                experiment != null ? experiment.getStatus() : null,
                hypothesis != null ? hypothesis.getId() : null,
                hypothesis != null ? hypothesis.getTitle() : null,
                hypothesis != null ? hypothesis.getStatus() : null,
                niche != null ? niche.getId() : null,
                niche != null ? niche.getName() : null,
                creative.getFormat(),
                creative.getHeadline(),
                creative.getPrimaryText(),
                creative.getVideoId(),
                creative.getVideoUrl(),
                creative.getDescription(),
                creative.getCta(),
                creative.getDestinationUrl(),
                creative.getStatus(),
                creative.getRejectionReason(),
                creative.getReviewedAt(),
                null,
                creative.getCostUsd(),
                null,
                creative.getCostUsd(),
                null,
                null,
                null,
                null);
    }

    /**
     * Converte vídeo de experimento em item da fila única de aprovação comercial com data de criação.
     */
    private CreativeVideoReviewDto toVideoReviewDto(ExperimentVideoAsset videoAsset) {
        Experiment experiment = videoAsset.getExperiment();
        Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
        MarketNiche niche = hypothesis != null && hypothesis.getMarketNiche() != null
                ? hypothesis.getMarketNiche()
                : experiment != null ? experiment.getNiche() : null;
        return new CreativeVideoReviewDto(
                videoAsset.getId(),
                CreativeVideoReviewSourceType.EXPERIMENT_VIDEO_ASSET,
                videoAsset.getSlot(),
                experiment != null ? experiment.getId() : null,
                experiment != null ? experiment.getName() : null,
                experiment != null ? experiment.getStatus() : null,
                hypothesis != null ? hypothesis.getId() : null,
                hypothesis != null ? hypothesis.getTitle() : null,
                hypothesis != null ? hypothesis.getStatus() : null,
                niche != null ? niche.getId() : null,
                niche != null ? niche.getName() : null,
                "VIDEO",
                resolveExperimentVideoHeadline(videoAsset),
                videoAsset.getScript(),
                null,
                resolveExperimentVideoUrl(videoAsset),
                videoAsset.getPrompt(),
                experiment != null ? experiment.getPrimaryCta() : null,
                null,
                toCreativeStatus(videoAsset.getReviewStatus()),
                videoAsset.getRejectionReason(),
                videoAsset.getReviewedAt(),
                videoAsset.getCreatedAt(),
                videoAsset.getCost(),
                videoAsset.getAudioCost(),
                totalProductionCost(videoAsset.getCost(), videoAsset.getAudioCost()),
                videoAsset.getVisualSourceType(),
                videoAsset.getVisualSourceKey(),
                videoAsset.getVisualSourceDescription(),
                videoAsset.getVisualSimilarityOverrideReason());
    }

    /**
     * Mapeia o filtro da tela para o status de revisão dos vídeos de experimento.
     */
    private ExperimentVideoReviewStatus toExperimentVideoReviewStatus(CreativeStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case DRAFT -> ExperimentVideoReviewStatus.PENDING;
            case READY -> ExperimentVideoReviewStatus.APPROVED;
            case REJECTED -> ExperimentVideoReviewStatus.REJECTED;
        };
    }

    /**
     * Mapeia o status de revisão do vídeo de experimento para o contrato visual da fila.
     */
    private CreativeStatus toCreativeStatus(ExperimentVideoReviewStatus status) {
        if (status == ExperimentVideoReviewStatus.APPROVED) {
            return CreativeStatus.READY;
        }
        if (status == ExperimentVideoReviewStatus.REJECTED) {
            return CreativeStatus.REJECTED;
        }
        return CreativeStatus.DRAFT;
    }

    /**
     * Soma custo de vídeo e áudio separado preservando nulo quando não há custo registrado.
     */
    private BigDecimal totalProductionCost(BigDecimal videoCost, BigDecimal audioCost) {
        if (videoCost == null && audioCost == null) {
            return null;
        }
        return BigDecimal.ZERO
                .add(videoCost != null ? videoCost : BigDecimal.ZERO)
                .add(audioCost != null ? audioCost : BigDecimal.ZERO);
    }

    /**
     * Normaliza chave visual declarada para comparação consistente na fila humana.
     */
    private String normalizeVisualSourceKey(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }

    /**
     * Resolve o título comercial exibido na revisão do vídeo de experimento.
     */
    private String resolveExperimentVideoHeadline(ExperimentVideoAsset videoAsset) {
        if (StringUtils.hasText(videoAsset.getObjective())) {
            return videoAsset.getObjective();
        }
        if (videoAsset.getExperiment() != null && StringUtils.hasText(videoAsset.getExperiment().getName())) {
            return videoAsset.getExperiment().getName();
        }
        return "Vídeo de experimento";
    }

    /**
     * Resolve a URL pública do vídeo de experimento para prévia e aprovação.
     */
    private String resolveExperimentVideoUrl(ExperimentVideoAsset videoAsset) {
        if (StringUtils.hasText(videoAsset.getAssetUrl())) {
            return videoAsset.getAssetUrl();
        }
        Asset asset = videoAsset.getAsset();
        return asset != null ? asset.getUrl() : null;
    }

    /**
     * Verifica se o vídeo de experimento tem mídia pública antes de aprovar.
     */
    private boolean hasPublicExperimentVideoUrl(ExperimentVideoAsset videoAsset) {
        return StringUtils.hasText(resolveExperimentVideoUrl(videoAsset));
    }

    /**
     * Atribui o custo da geração ao experimento e à hierarquia comercial.
     */
    private void applyGenerationCost(Experiment experiment, BigDecimal costUsd) {
        if (experiment == null) {
            return;
        }
        costAttributionService.addUsdCostToExperimentHierarchy(experiment, costUsd);
    }

    /**
     * Reduz textos longos para manter o log legível e preservar o diagnóstico.
     */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500) + "...";
    }

    /**
     * Salva a imagem enviada e retorna os metadados de armazenamento.
     */
    public AssetUploadResponse uploadImage(MultipartFile file,
                                                                         String model,
                                                                         String prompt,
                                                                         String intermediatePrompt,
                                                                         AssetUploadCategory category,
                                                                         Long experimentId,
                                                                         Long flowId,
                                                                         String flowSlug) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        AssetUploadCategory resolvedCategory = category != null ? category : AssetUploadCategory.GENERIC;
        AssetUploadContext context = new AssetUploadContext(resolvedCategory, experimentId, flowId, flowSlug);
        AssetStorageService.StoredObject storedObject = assetStorageService.store(file, context);
        String cleanedModel = StringUtils.hasText(model) ? model.trim() : null;
        String cleanedPrompt = StringUtils.hasText(prompt) ? prompt.trim() : null;
        String cleanedIntermediatePrompt =
                StringUtils.hasText(intermediatePrompt) ? intermediatePrompt.trim() : null;
        Asset asset = Asset.builder()
                .type(AssetType.IMAGE)
                .provider(MediaProvider.USER_UPLOAD)
                .status(AssetStatus.READY)
                .url(storedObject.publicUrl())
                .externalId(storedObject.storedFileName())
                .model(cleanedModel)
                .prompt(cleanedPrompt)
                .promptIntermediate(cleanedIntermediatePrompt)
                .payload(buildAssetPayload(storedObject, resolvedCategory, experimentId, flowId, flowSlug))
                .build();
        assetRepository.save(asset);
        return new AssetUploadResponse(storedObject.publicUrl(),
                storedObject.storedFileName(),
                resolvedCategory);
    }

    /**
     * Monta o payload auditável do asset armazenado.
     */
    private String buildAssetPayload(AssetStorageService.StoredObject storedObject,
                                            AssetUploadCategory category,
                                            Long experimentId,
                                            Long flowId,
                                            String flowSlug) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("category", category.name());
        payload.put("stored_file_name", storedObject.storedFileName());
        payload.put("public_url", storedObject.publicUrl());
        payload.put("storage_medium", storedObject.storedInBucket() ? "CLOUDFLARE_R2" : "LOCAL_FS");
        if (experimentId != null) {
            payload.put("experiment_id", experimentId);
        }
        if (flowId != null) {
            payload.put("flow_id", flowId);
        }
        if (StringUtils.hasText(flowSlug)) {
            payload.put("flow_slug", flowSlug.trim());
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new StorageException("Falha ao serializar metadados do asset", ex);
        }
    }

    /**
     * Busca o HTML de prévia do criativo na API de Marketing do Facebook.
     */
    public String preview(Long creativeId) throws IOException, InterruptedException {
        String token = System.getProperty("FB_ACCESS_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getenv("FB_ACCESS_TOKEN");
        }
        if (token == null || token.isBlank()) {
            return "";
        }
        String url = "https://graph.facebook.com/v19.0/adcreatives/" + creativeId
                + "/previews?access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = objectMapper.readTree(resp.body());
        if (node.has("data") && node.get("data").isArray() && node.get("data").size() > 0) {
            return node.get("data").get(0).get("body").asText();
        }
        return "";
    }
}
