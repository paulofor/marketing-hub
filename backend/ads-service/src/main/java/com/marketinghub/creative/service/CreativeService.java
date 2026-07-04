package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.*;
import com.marketinghub.creative.dto.AssetUploadResponse;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.creative.label.VisualProofRepository;
import com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

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
            Creative creative = Creative.builder()
                    .experiment(exp)
                    .format(request.getFormat())
                    .headline(request.getHeadline())
                    .primaryText(request.getPrimaryText())
                    .imageUrl(request.getImageUrl())
                    .description(request.getDescription())
                    .cta(normalizeMetaCallToAction(request.getCta()))
                    .destinationUrl(request.getDestinationUrl())
                    .leadGenFormId(request.getLeadGenFormId())
                    .instagramUserId(request.getInstagramUserId())
                    .status(request.getStatus())
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
        creative.setFormat(request.getFormat());
        creative.setHeadline(request.getHeadline());
        creative.setPrimaryText(request.getPrimaryText());
        creative.setImageUrl(request.getImageUrl());
        creative.setDescription(request.getDescription());
        creative.setCta(normalizeMetaCallToAction(request.getCta()));
        creative.setDestinationUrl(request.getDestinationUrl());
        creative.setLeadGenFormId(request.getLeadGenFormId());
        creative.setInstagramUserId(request.getInstagramUserId());
        creative.setStatus(request.getStatus());
        Creative saved = repository.save(creative);
        refreshExperimentApproval(saved.getExperiment());
        return saved;
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
        boolean hasApprovedCreatives = repository.existsByExperimentIdAndStatus(
                experiment.getId(), CreativeStatus.READY);
        experiment.setCreativeApproved(hasApprovedCreatives);
        experimentRepository.save(experiment);
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
