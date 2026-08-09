package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.creative.*;
import com.marketinghub.creative.dto.AssetUploadResponse;
import com.marketinghub.creative.dto.CreateCreativeRequest;
import com.marketinghub.creative.dto.CreativeAgentReviewPendingDto;
import com.marketinghub.creative.dto.CreativeAgentReviewResultRequest;
import com.marketinghub.creative.dto.CreativeImprovementPendingDto;
import com.marketinghub.creative.dto.CreativeImprovementResultRequest;
import com.marketinghub.creative.dto.CreativeVideoReviewDto;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository;
import com.marketinghub.repository.jpa.creative.label.VisualProofRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import com.marketinghub.storage.StorageException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: centralizar as operações de criativos vinculados a experimentos. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreativeService {
  private static final int META_CALL_TO_ACTION_MAX_LENGTH = 32;
  private static final String DEFAULT_META_CALL_TO_ACTION = "LEARN_MORE";
  private static final int MAX_AGENT_IMPROVEMENT_ATTEMPTS = 3;

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
  private final ProductRepository productRepository;

  /** Cria e persiste um criativo para o experimento informado. */
  @Transactional
  public Creative create(Long experimentId, CreateCreativeRequest request) {
    try {
      Experiment exp = experimentRepository.findById(experimentId).orElseThrow();
      validateReadyCreativeHasImage(request);
      Creative creative =
          Creative.builder()
              .experiment(exp)
              .versionNumber(1)
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
              .status(
                  request.getStatus() == CreativeStatus.REJECTED
                      ? CreativeStatus.REJECTED
                      : CreativeStatus.DRAFT)
              .agentReviewStatus(CreativeAgentReviewStatus.PENDING)
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

  /** Cria uma revisão editável preservando integralmente o criativo de origem. */
  @Transactional
  public Creative createVersion(Long sourceCreativeId, CreateCreativeRequest request) {
    try {
      Creative source = repository.findByIdWithExperiment(sourceCreativeId).orElseThrow();
      validateReadyCreativeHasImage(request);
      Creative revision =
          Creative.builder()
              .sourceCreative(source)
              .versionNumber(Objects.requireNonNullElse(source.getVersionNumber(), 1) + 1)
              .experiment(source.getExperiment())
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
              .status(CreativeStatus.DRAFT)
              .agentReviewStatus(CreativeAgentReviewStatus.PENDING)
              .build();
      Creative saved = repository.save(revision);
      applyGenerationCost(source.getExperiment(), request.getCostUsd());
      refreshExperimentApproval(source.getExperiment());
      return saved;
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao versionar criativo no backend. classe={} operacao=createCreativeVersion sourceCreativeId={} erro='{}'",
          getClass().getSimpleName(),
          sourceCreativeId,
          ex.getMessage(),
          ex);
      throw ex;
    }
  }

  /**
   * Reutiliza um anúncio aprovado em outro experimento do mesmo nicho como nova revisão auditável.
   */
  @Transactional
  public Creative reuseInExperiment(Long targetExperimentId, Long sourceCreativeId) {
    try {
      Creative source = repository.findByIdWithExperiment(sourceCreativeId).orElseThrow();
      Experiment target = experimentRepository.findById(targetExperimentId).orElseThrow();
      if (source.getExperiment().getNiche() == null
          || target.getNiche() == null
          || !Objects.equals(
              source.getExperiment().getNiche().getId(), target.getNiche().getId())) {
        throw new IllegalArgumentException(
            "O anúncio de origem não pertence ao mesmo produto/nicho do experimento");
      }
      if (source.getStatus() != CreativeStatus.READY
          || source.getAgentReviewStatus() != CreativeAgentReviewStatus.APPROVED) {
        throw new IllegalArgumentException(
            "Somente anúncios aprovados pelo agente e pela revisão humana podem ser reutilizados");
      }
      Creative reused =
          Creative.builder()
              .sourceCreative(source)
              .versionNumber(Objects.requireNonNullElse(source.getVersionNumber(), 1) + 1)
              .experiment(target)
              .format(source.getFormat())
              .headline(source.getHeadline())
              .primaryText(source.getPrimaryText())
              .imageUrl(source.getImageUrl())
              .videoId(source.getVideoId())
              .videoUrl(source.getVideoUrl())
              .description(source.getDescription())
              .cta(source.getCta())
              .destinationUrl(source.getDestinationUrl())
              .leadGenFormId(source.getLeadGenFormId())
              .instagramUserId(source.getInstagramUserId())
              .status(CreativeStatus.DRAFT)
              .agentReviewStatus(CreativeAgentReviewStatus.PENDING)
              .build();
      Creative saved = repository.save(reused);
      refreshExperimentApproval(target);
      return saved;
    } catch (RuntimeException ex) {
      log.error(
          "Falha ao reutilizar anúncio no experimento. classe={} operacao=reuseCreative targetExperimentId={} sourceCreativeId={} erro='{}'",
          getClass().getSimpleName(),
          targetExperimentId,
          sourceCreativeId,
          ex.getMessage(),
          ex);
      throw ex;
    }
  }

  /** Atualiza um criativo existente. */
  @Transactional
  public Creative update(Long id, CreateCreativeRequest request) {
    Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
    validateReadyCreativeHasImage(request);
    validateAgentReviewGate(creative, request.getStatus());
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
    creative.setAgentReviewStatus(CreativeAgentReviewStatus.PENDING);
    creative.setAgentReviewJson(null);
    creative.setAgentReviewRequestJson(null);
    creative.setAgentReviewResponseJson(null);
    creative.setAgentReviewedAt(null);
    if (request.getStatus() == CreativeStatus.READY) {
      creative.setStatus(CreativeStatus.DRAFT);
    }
    creative.setRejectionReason(null);
    Creative saved = repository.save(creative);
    refreshExperimentApproval(saved.getExperiment());
    return saved;
  }

  /** Lista criativos de vídeo publicáveis para aprovação operacional. */
  public List<CreativeVideoReviewDto> listVideoReviewQueue(CreativeStatus status) {
    List<Creative> creatives =
        status == null
            ? repository.findVideoCreativesForReview()
            : repository.findVideoCreativesForReviewByStatus(status);
    ExperimentVideoReviewStatus reviewStatus = toExperimentVideoReviewStatus(status);
    List<ExperimentVideoAsset> experimentVideos =
        reviewStatus == null
            ? experimentVideoAssetRepository.findReadyExperimentVideosForReview(
                ExperimentVideoStatus.READY)
            : experimentVideoAssetRepository.findReadyExperimentVideosForReviewByReviewStatus(
                ExperimentVideoStatus.READY, reviewStatus);
    return java.util.stream.Stream.concat(
            creatives.stream().map(this::toVideoReviewDto),
            experimentVideos.stream().map(this::toVideoReviewDto))
        .sorted(
            Comparator.comparing(
                    CreativeVideoReviewDto::experimentId,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(
                    CreativeVideoReviewDto::id, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  /** Atualiza a revisão de um item da fila única de vídeos pela origem persistida. */
  @Transactional
  public CreativeVideoReviewDto updateVideoReviewStatus(
      CreativeVideoReviewSourceType sourceType,
      Long id,
      CreativeStatus status,
      String rejectionReason) {
    if (sourceType == null || sourceType == CreativeVideoReviewSourceType.CREATIVE) {
      return toVideoReviewDto(updateStatus(id, status, rejectionReason));
    }
    return updateExperimentVideoReviewStatus(id, status, rejectionReason);
  }

  /** Atualiza somente o status de revisão do criativo. */
  @Transactional
  public Creative updateStatus(Long id, CreativeStatus status) {
    return updateStatus(id, status, null);
  }

  /** Atualiza o status de revisão do criativo e persiste o motivo quando ele for reprovado. */
  @Transactional
  public Creative updateStatus(Long id, CreativeStatus status, String rejectionReason) {
    try {
      if (status == null) {
        throw new IllegalArgumentException("Status do criativo é obrigatório.");
      }
      Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
      validateReadyCreativeHasMedia(creative, status);
      validateAgentReviewGate(creative, status);
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

  /** Lista e assume anúncios pendentes para impedir processamento concorrente pelo agente. */
  @Transactional
  public List<CreativeAgentReviewPendingDto> claimAgentReviewQueue(int limit) {
    return repository.findAgentReviewQueue(CreativeAgentReviewStatus.PENDING).stream()
        .limit(Math.max(1, limit))
        .map(
            creative -> {
              creative.setAgentReviewStatus(CreativeAgentReviewStatus.PROCESSING);
              repository.save(creative);
              Experiment experiment = creative.getExperiment();
              Hypothesis hypothesis = experiment.getHypothesisRef();
              MarketNiche niche =
                  hypothesis != null && hypothesis.getMarketNiche() != null
                      ? hypothesis.getMarketNiche()
                      : experiment.getNiche();
              String mediaUrl =
                  "VIDEO".equalsIgnoreCase(creative.getFormat())
                      ? creative.getVideoUrl()
                      : creative.getImageUrl();
              Product desireMapProduct =
                  niche == null ? null : uniqueProductForDesireMap(niche.getId());
              return new CreativeAgentReviewPendingDto(
                  creative.getId(),
                  experiment.getId(),
                  experiment.getName(),
                  niche != null ? niche.getName() : null,
                  hypothesis != null ? hypothesis.getTitle() : experiment.getHypothesis(),
                  creative.getFormat(),
                  creative.getHeadline(),
                  creative.getPrimaryText(),
                  creative.getDescription(),
                  creative.getCta(),
                  resolveAgentReviewDestinationUrl(creative, experiment),
                  mediaUrl,
                  desireMapProduct != null
                      ? desireMapProduct.getDesireAssociationMapVersion()
                      : null,
                  desireMapProduct != null ? desireMapProduct.getDesireAssociationMapJson() : null);
            })
        .toList();
  }

  /** Resolve a landing pública do experimento quando o criativo legado não gravou seu destino. */
  private String resolveAgentReviewDestinationUrl(Creative creative, Experiment experiment) {
    if (StringUtils.hasText(creative.getDestinationUrl())) {
      return creative.getDestinationUrl().trim();
    }
    boolean publishedLanding =
        experiment.getLeadPortalFlow() != null && experiment.getLeadPortalFlow().isApproved();
    return publishedLanding && StringUtils.hasText(experiment.getFollowUpActionUrl())
        ? experiment.getFollowUpActionUrl().trim()
        : null;
  }

  /** Enfileira explicitamente um criativo legado ou com falha para nova revisão do agente. */
  @Transactional
  public Creative requestAgentReview(Long id) {
    Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
    CreativeAgentReviewStatus current = creative.getAgentReviewStatus();
    if (current == CreativeAgentReviewStatus.PENDING
        || current == CreativeAgentReviewStatus.PROCESSING) {
      return creative;
    }
    creative.setAgentReviewStatus(CreativeAgentReviewStatus.PENDING);
    creative.setAgentReviewJson(null);
    creative.setAgentReviewRequestJson(null);
    creative.setAgentReviewResponseJson(null);
    creative.setAgentReviewModel(null);
    creative.setAgentReviewedAt(null);
    return repository.save(creative);
  }

  /** Resolve mapa apenas quando o nicho identifica um único produto, evitando contexto cruzado. */
  private Product uniqueProductForDesireMap(Long marketNicheId) {
    List<Product> products = productRepository.findAllByMarketNiche_Id(marketNicheId);
    return products.size() == 1 ? products.getFirst() : null;
  }

  /** Persiste o parecer auditável do agente e mantém o anúncio bloqueado quando não aprovado. */
  @Transactional
  public Creative applyAgentReview(Long id, CreativeAgentReviewResultRequest request) {
    Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
    CreativeAgentReviewStatus decision = Objects.requireNonNull(request.decision());
    if (decision != CreativeAgentReviewStatus.APPROVED
        && decision != CreativeAgentReviewStatus.ADJUST
        && decision != CreativeAgentReviewStatus.REJECTED
        && decision != CreativeAgentReviewStatus.FAILED) {
      throw new IllegalArgumentException("Decisão final inválida para revisão do agente.");
    }
    validateScore(request.attentionScore());
    validateScore(request.clarityScore());
    validateScore(request.desireScore());
    validateScore(request.credibilityScore());
    validateScore(request.actionScore());
    validateSpecialistApprovalContract(request, decision);
    creative.setAgentReviewStatus(decision);
    creative.setAgentReviewJson(toAgentReviewJson(request));
    creative.setAgentReviewRequestJson(request.requestJson());
    creative.setAgentReviewResponseJson(request.responseJson());
    creative.setAgentReviewModel(request.model());
    creative.setAgentReviewedAt(Instant.now());
    scheduleAgentImprovement(creative, request, decision);
    if (decision != CreativeAgentReviewStatus.APPROVED
        && creative.getStatus() == CreativeStatus.READY) {
      creative.setStatus(CreativeStatus.DRAFT);
      creative.setReviewedAt(null);
    }
    Creative saved = repository.save(creative);
    refreshExperimentApproval(saved.getExperiment());
    return saved;
  }

  /** Lista e assume correções pendentes, mantendo o backend como controlador do ciclo. */
  @Transactional
  public List<CreativeImprovementPendingDto> claimAgentImprovementQueue(int limit) {
    return repository.findAgentImprovementQueue(CreativeImprovementStatus.PENDING).stream()
        .limit(Math.max(1, limit))
        .map(
            creative -> {
              creative.setAgentImprovementStatus(CreativeImprovementStatus.PROCESSING);
              repository.save(creative);
              JsonNode correction = readImprovementJson(creative);
              return new CreativeImprovementPendingDto(
                  creative.getId(),
                  creative.getExperiment().getId(),
                  Objects.requireNonNullElse(creative.getVersionNumber(), 1) + 1,
                  creative.getFormat(),
                  correction.path("headline").asText(),
                  correction.path("primaryText").asText(),
                  correction.path("description").asText(),
                  correction.path("cta").asText(),
                  creative.getDestinationUrl(),
                  correction.path("imagePrompt").asText(),
                  stringList(correction.path("mandatoryVisualRequirements")),
                  stringList(correction.path("forbiddenVisualElements")),
                  stringList(correction.path("visualAcceptanceCriteria")),
                  creative.getAgentReviewJson());
            })
        .toList();
  }

  /** Cria a nova versão gerada e a devolve automaticamente ao gate do agente. */
  @Transactional
  public Creative completeAgentImprovement(Long id, CreativeImprovementResultRequest result) {
    Creative source = repository.findByIdWithExperiment(id).orElseThrow();
    if (!StringUtils.hasText(result.imageUrl())) {
      source.setAgentImprovementStatus(CreativeImprovementStatus.FAILED);
      source.setAgentImprovementError(
          StringUtils.hasText(result.error())
              ? result.error().trim()
              : "Imagem corrigida não gerada");
      return repository.save(source);
    }
    JsonNode correction = readImprovementJson(source);
    CreateCreativeRequest request = new CreateCreativeRequest();
    request.setFormat(source.getFormat());
    request.setHeadline(correction.path("headline").asText(source.getHeadline()));
    request.setPrimaryText(correction.path("primaryText").asText(source.getPrimaryText()));
    request.setDescription(correction.path("description").asText(source.getDescription()));
    request.setCta(correction.path("cta").asText(source.getCta()));
    request.setDestinationUrl(source.getDestinationUrl());
    request.setImageUrl(result.imageUrl());
    request.setCostUsd(result.costUsd());
    Creative revision = createVersion(id, request);
    revision.setAgentImprovementAttempts(
        Objects.requireNonNullElse(source.getAgentImprovementAttempts(), 0) + 1);
    source.setAgentImprovementStatus(CreativeImprovementStatus.COMPLETED);
    source.setAgentImprovementError(null);
    repository.save(source);
    return repository.save(revision);
  }

  /** Agenda uma correção quando o parecer reprova e ainda existe orçamento de tentativas. */
  private void scheduleAgentImprovement(
      Creative creative,
      CreativeAgentReviewResultRequest request,
      CreativeAgentReviewStatus decision) {
    if (decision == CreativeAgentReviewStatus.APPROVED) {
      creative.setAgentImprovementStatus(null);
      creative.setAgentImprovementError(null);
      return;
    }
    if (decision != CreativeAgentReviewStatus.ADJUST
        && decision != CreativeAgentReviewStatus.REJECTED) {
      creative.setAgentImprovementStatus(CreativeImprovementStatus.FAILED);
      return;
    }
    int attempts = Objects.requireNonNullElse(creative.getAgentImprovementAttempts(), 0);
    if (attempts >= MAX_AGENT_IMPROVEMENT_ATTEMPTS) {
      creative.setAgentImprovementStatus(CreativeImprovementStatus.LIMIT_REACHED);
      creative.setAgentImprovementError("Limite de três correções automáticas atingido");
      return;
    }
    if (!StringUtils.hasText(request.revisedImagePrompt())) {
      creative.setAgentImprovementStatus(CreativeImprovementStatus.FAILED);
      creative.setAgentImprovementError("Agente não forneceu prompt visual corrigido");
      return;
    }
    if (normalizedItems(request.mandatoryVisualRequirements()).isEmpty()
        || normalizedItems(request.visualAcceptanceCriteria()).isEmpty()) {
      creative.setAgentImprovementStatus(CreativeImprovementStatus.FAILED);
      creative.setAgentImprovementError(
          "Agente não forneceu requisitos e critérios visuais verificáveis");
      return;
    }
    creative.setAgentImprovementJson(toImprovementJson(request));
    creative.setAgentImprovementStatus(CreativeImprovementStatus.PENDING);
    creative.setAgentImprovementError(null);
  }

  /** Serializa o contrato funcional da correção separadamente do parecer técnico. */
  private String toImprovementJson(CreativeAgentReviewResultRequest request) {
    try {
      Map<String, Object> correction = new LinkedHashMap<>();
      correction.put("headline", Objects.requireNonNullElse(request.revisedHeadline(), ""));
      correction.put("primaryText", Objects.requireNonNullElse(request.revisedPrimaryText(), ""));
      correction.put("description", Objects.requireNonNullElse(request.revisedDescription(), ""));
      correction.put(
          "cta", Objects.requireNonNullElse(request.revisedCta(), DEFAULT_META_CALL_TO_ACTION));
      correction.put("imagePrompt", Objects.requireNonNullElse(request.revisedImagePrompt(), ""));
      correction.put(
          "mandatoryVisualRequirements", normalizedItems(request.mandatoryVisualRequirements()));
      correction.put("forbiddenVisualElements", normalizedItems(request.forbiddenVisualElements()));
      correction.put(
          "visualAcceptanceCriteria", normalizedItems(request.visualAcceptanceCriteria()));
      return objectMapper.writeValueAsString(correction);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar correção do agente. creativeId desconhecido", ex);
      throw new IllegalStateException("Correção do agente inválida", ex);
    }
  }

  /** Normaliza listas do contrato para impedir instruções vazias ou duplicadas. */
  private List<String> normalizedItems(List<String> items) {
    if (items == null) {
      return List.of();
    }
    return items.stream().filter(StringUtils::hasText).map(String::trim).distinct().toList();
  }

  /** Converte um array JSON persistido em lista textual segura para o executor. */
  private List<String> stringList(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<String> values = new java.util.ArrayList<>();
    node.forEach(
        item -> {
          if (StringUtils.hasText(item.asText())) {
            values.add(item.asText().trim());
          }
        });
    return List.copyOf(values);
  }

  /** Lê o contrato de correção persistido e falha sem executar geração genérica. */
  private JsonNode readImprovementJson(Creative creative) {
    try {
      return objectMapper.readTree(creative.getAgentImprovementJson());
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      log.error("Falha ao ler correção do agente. creativeId={}", creative.getId(), ex);
      throw new IllegalStateException("Contrato de correção do agente inválido", ex);
    }
  }

  /** Bloqueia aprovação humana enquanto o agente não aprovar o anúncio. */
  private void validateAgentReviewGate(Creative creative, CreativeStatus status) {
    if (status == CreativeStatus.READY
        && creative.getAgentReviewStatus() != null
        && creative.getAgentReviewStatus() != CreativeAgentReviewStatus.APPROVED) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Aprovação bloqueada: o Agente Especialista em Anúncios ainda não aprovou o criativo.");
    }
  }

  /** Valida que cada dimensão do parecer usa a escala auditável de zero a cem. */
  private void validateScore(Integer score) {
    if (score != null && (score < 0 || score > 100)) {
      throw new IllegalArgumentException("Score do agente deve estar entre 0 e 100.");
    }
  }

  /** Impede aprovação sem notas mínimas e pareceres especialistas sobre as três dimensões comerciais. */
  private void validateSpecialistApprovalContract(
      CreativeAgentReviewResultRequest request, CreativeAgentReviewStatus decision) {
    if (decision != CreativeAgentReviewStatus.APPROVED) {
      return;
    }
    List<Integer> scores =
        List.of(
            Objects.requireNonNullElse(request.attentionScore(), 0),
            Objects.requireNonNullElse(request.clarityScore(), 0),
            Objects.requireNonNullElse(request.desireScore(), 0),
            Objects.requireNonNullElse(request.credibilityScore(), 0),
            Objects.requireNonNullElse(request.actionScore(), 0));
    if (scores.stream().anyMatch(score -> score < 80)) {
      throw new IllegalArgumentException(
          "Aprovação do agente exige nota mínima 80 em todas as dimensões.");
    }
    if (!StringUtils.hasText(request.copyAssessment())
        || !StringUtils.hasText(request.commercialAestheticAssessment())
        || !StringUtils.hasText(request.destinationIntegrationAssessment())) {
      throw new IllegalArgumentException(
          "Aprovação do agente exige pareceres de copy, estética comercial e integração com a landing.");
    }
  }

  /** Serializa o parecer funcional sem misturá-lo ao request e response brutos. */
  private String toAgentReviewJson(CreativeAgentReviewResultRequest request) {
    try {
      return objectMapper.writeValueAsString(
          Map.ofEntries(
              Map.entry("decision", request.decision()),
              Map.entry("attentionScore", Objects.requireNonNullElse(request.attentionScore(), 0)),
              Map.entry("clarityScore", Objects.requireNonNullElse(request.clarityScore(), 0)),
              Map.entry("desireScore", Objects.requireNonNullElse(request.desireScore(), 0)),
              Map.entry(
                  "credibilityScore", Objects.requireNonNullElse(request.credibilityScore(), 0)),
              Map.entry("actionScore", Objects.requireNonNullElse(request.actionScore(), 0)),
              Map.entry("copyAssessment", Objects.requireNonNullElse(request.copyAssessment(), "")),
              Map.entry(
                  "commercialAestheticAssessment",
                  Objects.requireNonNullElse(request.commercialAestheticAssessment(), "")),
              Map.entry(
                  "destinationIntegrationAssessment",
                  Objects.requireNonNullElse(request.destinationIntegrationAssessment(), "")),
              Map.entry("summary", Objects.requireNonNullElse(request.summary(), "")),
              Map.entry("issues", Objects.requireNonNullElse(request.issuesJson(), "[]")),
              Map.entry(
                  "recommendations",
                  Objects.requireNonNullElse(request.recommendationsJson(), "[]")),
              Map.entry("inputTokens", Objects.requireNonNullElse(request.inputTokens(), 0)),
              Map.entry("outputTokens", Objects.requireNonNullElse(request.outputTokens(), 0)),
              Map.entry("costUsd", Objects.requireNonNullElse(request.costUsd(), BigDecimal.ZERO)),
              Map.entry("error", Objects.requireNonNullElse(request.error(), ""))));
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar parecer do agente. creativeId desconhecido", ex);
      throw new IllegalStateException("Parecer do agente inválido", ex);
    }
  }

  /** Normaliza e valida o motivo obrigatório para reprovação comercial. */
  private String normalizeRejectionReason(CreativeStatus status, String rejectionReason) {
    if (status != CreativeStatus.REJECTED) {
      return null;
    }
    if (!StringUtils.hasText(rejectionReason)) {
      throw new IllegalArgumentException("Informe o motivo da reprovação do vídeo.");
    }
    return rejectionReason.trim();
  }

  /** Carimba a data da decisão humana ou limpa o carimbo quando o item volta para revisão. */
  private void applyCreativeReviewTimestamp(Creative creative, CreativeStatus status) {
    if (status == CreativeStatus.READY || status == CreativeStatus.REJECTED) {
      creative.setReviewedAt(Instant.now());
      return;
    }
    creative.setReviewedAt(null);
  }

  /** Atualiza a revisão humana de um vídeo de experimento e preserva o motivo da reprovação. */
  private CreativeVideoReviewDto updateExperimentVideoReviewStatus(
      Long id, CreativeStatus status, String rejectionReason) {
    if (status == null) {
      throw new IllegalArgumentException("Status do vídeo é obrigatório.");
    }
    ExperimentVideoAsset videoAsset = experimentVideoAssetRepository.findById(id).orElseThrow();
    if (status == CreativeStatus.READY && !hasPublicExperimentVideoUrl(videoAsset)) {
      throw new IllegalArgumentException("Vídeo de experimento aprovado precisa ter URL pública.");
    }
    if (status == CreativeStatus.READY && !Boolean.TRUE.equals(videoAsset.getHasAudio())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Vídeo de experimento aprovado precisa ter áudio validado.");
    }
    if (status == CreativeStatus.READY) {
      validateExperimentVideoVisualSourceDiversity(videoAsset);
    }
    ExperimentVideoReviewStatus reviewStatus = toExperimentVideoReviewStatus(status);
    if (reviewStatus == ExperimentVideoReviewStatus.REJECTED
        && !StringUtils.hasText(rejectionReason)) {
      throw new IllegalArgumentException("Informe o motivo da reprovação do vídeo.");
    }
    videoAsset.setReviewStatus(Objects.requireNonNull(reviewStatus));
    videoAsset.setReviewedBy("Marketing Hub");
    videoAsset.setReviewedAt(
        reviewStatus == ExperimentVideoReviewStatus.PENDING ? null : Instant.now());
    if (reviewStatus == ExperimentVideoReviewStatus.REJECTED) {
      videoAsset.setRejectionReason(rejectionReason.trim());
    } else if (reviewStatus == ExperimentVideoReviewStatus.APPROVED) {
      videoAsset.setRejectionReason(null);
    }
    return toVideoReviewDto(experimentVideoAssetRepository.save(videoAsset));
  }

  /** Bloqueia aprovação de anúncio e hero com a mesma origem visual sem justificativa comercial. */
  private void validateExperimentVideoVisualSourceDiversity(ExperimentVideoAsset videoAsset) {
    Experiment experiment = videoAsset.getExperiment();
    String visualSourceKey = normalizeVisualSourceKey(videoAsset.getVisualSourceKey());
    if (experiment == null
        || experiment.getId() == null
        || visualSourceKey == null
        || !isAdOrHero(videoAsset.getSlot())) {
      return;
    }
    boolean hasConflictingSlot =
        experimentVideoAssetRepository
            .findByExperimentIdAndVisualSourceKey(experiment.getId(), visualSourceKey)
            .stream()
            .filter(
                candidate ->
                    videoAsset.getId() == null || !videoAsset.getId().equals(candidate.getId()))
            .filter(
                candidate -> candidate.getReviewStatus() == ExperimentVideoReviewStatus.APPROVED)
            .anyMatch(candidate -> isAdHeroPair(videoAsset.getSlot(), candidate.getSlot()));
    if (hasConflictingSlot
        && !StringUtils.hasText(videoAsset.getVisualSimilarityOverrideReason())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Aprovação bloqueada: vídeo de campanha e hero do PDE usam a mesma origem visual. "
              + "Informe uma justificativa de exceção ou gere uma variação visual distinta.");
    }
  }

  /** Identifica slots que exigem diversidade visual entre mídia paga e hero de página. */
  private boolean isAdOrHero(ExperimentVideoSlot slot) {
    return slot == ExperimentVideoSlot.AD || slot == ExperimentVideoSlot.LANDING_HERO;
  }

  /** Verifica se dois slots formam colisão entre anúncio e hero de landing. */
  private boolean isAdHeroPair(ExperimentVideoSlot currentSlot, ExperimentVideoSlot candidateSlot) {
    return (currentSlot == ExperimentVideoSlot.AD
            && candidateSlot == ExperimentVideoSlot.LANDING_HERO)
        || (currentSlot == ExperimentVideoSlot.LANDING_HERO
            && candidateSlot == ExperimentVideoSlot.AD);
  }

  /** Mantem o CTA compatível com a coluna e com o tipo canônico aceito pela Meta. */
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

  /** Remove um criativo e atualiza o estado de aprovação do experimento. */
  @Transactional
  public void delete(Long id) {
    Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
    Experiment experiment = creative.getExperiment();
    repository.delete(creative);
    refreshExperimentApproval(experiment);
  }

  /** Lista os criativos vinculados ao experimento informado. */
  public Iterable<Creative> listByExperiment(Long experimentId) {
    return repository.findByExperimentId(experimentId);
  }

  /** Atualiza os rótulos comerciais do criativo. */
  @Transactional
  public Creative updateLabels(Long id, Long angleId, Long proofId, Long triggerId) {
    Creative creative = repository.findById(id).orElseThrow();
    if (angleId != null) {
      creative.setAngles(java.util.Set.of(angleRepository.findById(angleId).orElseThrow()));
    }
    if (proofId != null) {
      creative.setVisualProofs(
          java.util.Set.of(visualProofRepository.findById(proofId).orElseThrow()));
    }
    if (triggerId != null) {
      creative.setEmotionalTriggers(
          java.util.Set.of(emotionalTriggerRepository.findById(triggerId).orElseThrow()));
    }
    return creative;
  }

  /** Recalcula se o experimento possui criativos aprovados. */
  private void refreshExperimentApproval(Experiment experiment) {
    boolean hasApprovedCreatives =
        repository.existsByExperimentIdAndStatusAndUsableMedia(
            experiment.getId(), CreativeStatus.READY);
    experiment.setCreativeApproved(hasApprovedCreatives);
    experimentRepository.save(experiment);
  }

  /** Impede que criativo aprovado siga sem mídia compatível com o formato escolhido. */
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
      throw new IllegalArgumentException(
          "Criativo de vídeo aprovado precisa ter videoId da Meta ou videoUrl público.");
    }
  }

  /** Impede aprovação por status quando a mídia do criativo ainda não é publicável. */
  private void validateReadyCreativeHasMedia(Creative creative, CreativeStatus status) {
    if (status != CreativeStatus.READY) {
      return;
    }
    String format =
        StringUtils.hasText(creative.getFormat()) ? creative.getFormat().trim() : "IMAGE";
    if ("IMAGE".equalsIgnoreCase(format) && !StringUtils.hasText(creative.getImageUrl())) {
      throw new IllegalArgumentException("Criativo de imagem aprovado precisa ter imagem gerada.");
    }
    if ("VIDEO".equalsIgnoreCase(format)
        && !StringUtils.hasText(creative.getVideoId())
        && !StringUtils.hasText(creative.getVideoUrl())) {
      throw new IllegalArgumentException(
          "Criativo de vídeo aprovado precisa ter videoId da Meta ou videoUrl público.");
    }
  }

  /** Converte o criativo de vídeo em contrato de revisão com hipótese, nicho e dados de custo. */
  private CreativeVideoReviewDto toVideoReviewDto(Creative creative) {
    Experiment experiment = creative.getExperiment();
    Hypothesis hypothesis = experiment != null ? experiment.getHypothesisRef() : null;
    MarketNiche niche =
        hypothesis != null && hypothesis.getMarketNiche() != null
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
    MarketNiche niche =
        hypothesis != null && hypothesis.getMarketNiche() != null
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

  /** Mapeia o filtro da tela para o status de revisão dos vídeos de experimento. */
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

  /** Mapeia o status de revisão do vídeo de experimento para o contrato visual da fila. */
  private CreativeStatus toCreativeStatus(ExperimentVideoReviewStatus status) {
    if (status == ExperimentVideoReviewStatus.APPROVED) {
      return CreativeStatus.READY;
    }
    if (status == ExperimentVideoReviewStatus.REJECTED) {
      return CreativeStatus.REJECTED;
    }
    return CreativeStatus.DRAFT;
  }

  /** Soma custo de vídeo e áudio separado preservando nulo quando não há custo registrado. */
  private BigDecimal totalProductionCost(BigDecimal videoCost, BigDecimal audioCost) {
    if (videoCost == null && audioCost == null) {
      return null;
    }
    return BigDecimal.ZERO
        .add(videoCost != null ? videoCost : BigDecimal.ZERO)
        .add(audioCost != null ? audioCost : BigDecimal.ZERO);
  }

  /** Normaliza chave visual declarada para comparação consistente na fila humana. */
  private String normalizeVisualSourceKey(String value) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
  }

  /** Resolve o título comercial exibido na revisão do vídeo de experimento. */
  private String resolveExperimentVideoHeadline(ExperimentVideoAsset videoAsset) {
    if (StringUtils.hasText(videoAsset.getObjective())) {
      return videoAsset.getObjective();
    }
    if (videoAsset.getExperiment() != null
        && StringUtils.hasText(videoAsset.getExperiment().getName())) {
      return videoAsset.getExperiment().getName();
    }
    return "Vídeo de experimento";
  }

  /** Resolve a URL pública do vídeo de experimento para prévia e aprovação. */
  private String resolveExperimentVideoUrl(ExperimentVideoAsset videoAsset) {
    if (StringUtils.hasText(videoAsset.getAssetUrl())) {
      return videoAsset.getAssetUrl();
    }
    Asset asset = videoAsset.getAsset();
    return asset != null ? asset.getUrl() : null;
  }

  /** Verifica se o vídeo de experimento tem mídia pública antes de aprovar. */
  private boolean hasPublicExperimentVideoUrl(ExperimentVideoAsset videoAsset) {
    return StringUtils.hasText(resolveExperimentVideoUrl(videoAsset));
  }

  /** Atribui o custo da geração ao experimento e à hierarquia comercial. */
  private void applyGenerationCost(Experiment experiment, BigDecimal costUsd) {
    if (experiment == null) {
      return;
    }
    costAttributionService.addUsdCostToExperimentHierarchy(experiment, costUsd);
  }

  /** Reduz textos longos para manter o log legível e preservar o diagnóstico. */
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

  /** Salva a imagem enviada e retorna os metadados de armazenamento. */
  public AssetUploadResponse uploadImage(
      MultipartFile file,
      String model,
      String prompt,
      String intermediatePrompt,
      AssetUploadCategory category,
      Long experimentId,
      Long flowId,
      String flowSlug)
      throws IOException {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File must not be empty");
    }
    AssetUploadCategory resolvedCategory =
        category != null ? category : AssetUploadCategory.GENERIC;
    AssetUploadContext context =
        new AssetUploadContext(resolvedCategory, experimentId, flowId, flowSlug);
    AssetStorageService.StoredObject storedObject = assetStorageService.store(file, context);
    String cleanedModel = StringUtils.hasText(model) ? model.trim() : null;
    String cleanedPrompt = StringUtils.hasText(prompt) ? prompt.trim() : null;
    String cleanedIntermediatePrompt =
        StringUtils.hasText(intermediatePrompt) ? intermediatePrompt.trim() : null;
    Asset asset =
        Asset.builder()
            .type(AssetType.IMAGE)
            .provider(MediaProvider.USER_UPLOAD)
            .status(AssetStatus.READY)
            .url(storedObject.publicUrl())
            .externalId(storedObject.storedFileName())
            .model(cleanedModel)
            .prompt(cleanedPrompt)
            .promptIntermediate(cleanedIntermediatePrompt)
            .payload(
                buildAssetPayload(storedObject, resolvedCategory, experimentId, flowId, flowSlug))
            .build();
    assetRepository.save(asset);
    return new AssetUploadResponse(
        storedObject.publicUrl(), storedObject.storedFileName(), resolvedCategory);
  }

  /** Monta o payload auditável do asset armazenado. */
  private String buildAssetPayload(
      AssetStorageService.StoredObject storedObject,
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

  /** Busca o HTML de prévia do criativo na API de Marketing do Facebook. */
  public String preview(Long creativeId) throws IOException, InterruptedException {
    String token = System.getProperty("FB_ACCESS_TOKEN");
    if (token == null || token.isBlank()) {
      token = System.getenv("FB_ACCESS_TOKEN");
    }
    if (token == null || token.isBlank()) {
      return "";
    }
    String url =
        "https://graph.facebook.com/v19.0/adcreatives/"
            + creativeId
            + "/previews?access_token="
            + URLEncoder.encode(token, StandardCharsets.UTF_8);
    HttpRequest request =
        HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();
    HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode node = objectMapper.readTree(resp.body());
    if (node.has("data") && node.get("data").isArray() && node.get("data").size() > 0) {
      return node.get("data").get(0).get("body").asText();
    }
    return "";
  }
}
