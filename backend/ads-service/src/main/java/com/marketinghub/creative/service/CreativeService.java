package com.marketinghub.creative.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agentlearning.v1.TemisVisualLearningService;
import com.marketinghub.agentlearning.v1.TemisVisualPlaybookService;
import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.creative.*;
import com.marketinghub.creative.convergence.v1.CreativeConvergenceReport;
import com.marketinghub.creative.convergence.v1.CreativeConvergenceService;
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
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.planning.imagestudio.v1.service.TemisVisualPlaybookDto;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.creative.label.AngleRepository;
import com.marketinghub.repository.jpa.creative.label.EmotionalTriggerRepository;
import com.marketinghub.repository.jpa.creative.label.VisualProofRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
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
import java.util.ArrayList;
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
  private static final int MAX_AGENT_IMPROVEMENT_ATTEMPTS = 8;
  private static final int MAX_AGENT_REVIEW_RECOVERIES = 2;
  private static final Duration AGENT_REVIEW_LEASE_TIMEOUT = Duration.ofMinutes(50);

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
  private final CreativeConvergenceService convergenceService;
  private final CommercialPlanRepository commercialPlanRepository;
  private final CommercialPlanVisualAssetRepository commercialPlanVisualAssetRepository;
  private final CommercialPlanImageStudioJobRepository commercialPlanImageStudioJobRepository;
  private final TemisVisualPlaybookService temisVisualPlaybookService;
  private final TemisVisualLearningService temisVisualLearningService;

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
      cancelSupersededAncestorImprovements(saved, status);
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
   * Encerra retrabalhos antigos da mesma linhagem quando a versão final recebe aprovação humana.
   */
  private void cancelSupersededAncestorImprovements(Creative creative, CreativeStatus status) {
    if (status != CreativeStatus.READY) {
      return;
    }
    java.util.Set<Long> visited = new java.util.HashSet<>();
    Creative ancestor = creative.getSourceCreative();
    while (ancestor != null && ancestor.getId() != null && visited.add(ancestor.getId())) {
      if (ancestor.getAgentImprovementStatus() == CreativeImprovementStatus.PENDING
          || ancestor.getAgentImprovementStatus() == CreativeImprovementStatus.PROCESSING) {
        ancestor.setAgentImprovementStatus(CreativeImprovementStatus.FAILED);
        ancestor.setAgentImprovementError(
            "Ciclo encerrado pela aprovação da versão final #" + creative.getId());
        repository.save(ancestor);
      }
      ancestor = ancestor.getSourceCreative();
    }
  }

  /** Lista e assume anúncios pendentes para impedir processamento concorrente pelo agente. */
  @Transactional
  public List<CreativeAgentReviewPendingDto> claimAgentReviewQueue(int limit) {
    recoverExpiredAgentReviewLeases(Instant.now());
    return repository.findAgentReviewQueue(CreativeAgentReviewStatus.PENDING).stream()
        .limit(Math.max(1, limit))
        .map(
            creative -> {
              creative.setAgentReviewStatus(CreativeAgentReviewStatus.PROCESSING);
              creative.setAgentReviewStartedAt(Instant.now());
              repository.save(creative);
              return toAgentReviewContext(creative);
            })
        .toList();
  }

  /** Recupera leases órfãos com limite de repetição e trilha persistida no próprio criativo. */
  private void recoverExpiredAgentReviewLeases(Instant now) {
    Instant cutoff = now.minus(AGENT_REVIEW_LEASE_TIMEOUT);
    repository
        .findExpiredAgentReviewLeases(CreativeAgentReviewStatus.PROCESSING, cutoff)
        .forEach(
            creative -> {
              int recoveries =
                  Objects.requireNonNullElse(creative.getAgentReviewRecoveryCount(), 0);
              creative.setAgentReviewRecoveryCount(recoveries + 1);
              creative.setAgentReviewLastRecoveredAt(now);
              creative.setAgentReviewStartedAt(null);
              if (recoveries >= MAX_AGENT_REVIEW_RECOVERIES) {
                creative.setAgentReviewStatus(CreativeAgentReviewStatus.FAILED);
                creative.setAgentReviewResponseJson(
                    "{\"error\":\"Lease do Aprovador expirou após o limite de recuperações\"}");
                creative.setAgentReviewedAt(now);
                log.error(
                    "Lease órfão do Aprovador encerrado após limite. creativeId={} experimentId={} recoveries={}",
                    creative.getId(),
                    creative.getExperiment().getId(),
                    recoveries + 1);
              } else {
                creative.setAgentReviewStatus(CreativeAgentReviewStatus.PENDING);
                log.warn(
                    "Lease órfão do Aprovador recuperado. creativeId={} experimentId={} recovery={}",
                    creative.getId(),
                    creative.getExperiment().getId(),
                    recoveries + 1);
              }
              repository.save(creative);
            });
  }

  /** Retorna ao MCP o mesmo snapshot efetivo usado na reserva, sem alterar a fila. */
  @Transactional(readOnly = true)
  public CreativeAgentReviewPendingDto getAgentReviewContext(Long id, Long experimentId) {
    Creative creative = repository.findByIdWithExperiment(id).orElseThrow();
    if (!creative.getExperiment().getId().equals(experimentId)) {
      throw new IllegalArgumentException("Criativo não pertence ao experimento informado");
    }
    return toAgentReviewContext(creative);
  }

  /** Monta o contexto canônico do Aprovador com mídia e landing efetivas. */
  private CreativeAgentReviewPendingDto toAgentReviewContext(Creative creative) {
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
    Product desireMapProduct = niche == null ? null : uniqueProductForDesireMap(niche.getId());
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
        desireMapProduct != null ? desireMapProduct.getDesireAssociationMapVersion() : null,
        desireMapProduct != null ? desireMapProduct.getDesireAssociationMapJson() : null);
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
    creative.setAgentReviewStartedAt(null);
    creative.setAgentReviewRecoveryCount(0);
    creative.setAgentReviewLastRecoveredAt(null);
    return repository.save(creative);
  }

  /** Retorna o relatório funcional do ciclo de convergência da linhagem. */
  @Transactional(readOnly = true)
  public CreativeConvergenceReport getConvergenceReport(Long id) {
    return convergenceService.report(repository.findByIdWithExperiment(id).orElseThrow());
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
    validateReviewerDidNotCreateReplacement(request);
    creative.setAgentReviewStatus(decision);
    creative.setAgentReviewJson(toAgentReviewJson(request));
    creative.setAgentReviewRequestJson(request.requestJson());
    creative.setAgentReviewResponseJson(request.responseJson());
    creative.setAgentReviewModel(request.model());
    creative.setAgentReviewedAt(Instant.now());
    creative.setAgentReviewStartedAt(null);
    convergenceService.registerReview(creative, request);
    scheduleAgentImprovement(creative, request, decision);
    if (decision != CreativeAgentReviewStatus.APPROVED
        && creative.getStatus() == CreativeStatus.READY) {
      creative.setStatus(CreativeStatus.DRAFT);
      creative.setReviewedAt(null);
    }
    Creative saved = repository.save(creative);
    temisVisualLearningService.recordCreativeReview(saved, request);
    refreshExperimentApproval(saved.getExperiment());
    return saved;
  }

  /** Impede que a execução revisora de Têmis entregue copy, CTA ou prompt substitutos. */
  private void validateReviewerDidNotCreateReplacement(CreativeAgentReviewResultRequest request) {
    if (java.util.stream.Stream.of(
            request.revisedHeadline(),
            request.revisedPrimaryText(),
            request.revisedDescription(),
            request.revisedCta(),
            request.revisedImagePrompt())
        .anyMatch(StringUtils::hasText)) {
      throw new IllegalArgumentException(
          "Têmis deve informar causa e critério de aceite, sem criar conteúdo substituto.");
    }
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
              com.marketinghub.planning.CommercialPlan plan =
                  commercialPlanRepository
                      .findByExperimentReference(creative.getExperiment().getId())
                      .stream()
                      .findFirst()
                      .orElseThrow(
                          () ->
                              new IllegalStateException(
                                  "Retrabalho visual exige plano comercial vinculado"));
              String size =
                  Objects.toString(creative.getFormat(), "")
                          .toLowerCase(java.util.Locale.ROOT)
                          .contains("story")
                      ? "1152x2048"
                      : "2048x2048";
              TemisVisualPlaybookDto playbook =
                  temisVisualPlaybookService.resolve(
                      plan, creative.getFormat(), List.of("ADS"), size);
              if (correction instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
                object.put("playbookVersion", playbook.version());
                object.put("playbookContextKey", playbook.contextKey());
                object.set("visualPlaybook", objectMapper.valueToTree(playbook));
                creative.setAgentImprovementJson(object.toString());
                repository.save(creative);
              }
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
                  approvedCreativeReferenceUrls(creative.getExperiment().getId()),
                  creative.getAgentReviewJson(),
                  playbook);
            })
        .toList();
  }

  /**
   * Entrega ao retrabalho somente imagens aprovadas do plano comercial que governa o experimento.
   */
  private List<String> approvedCreativeReferenceUrls(Long experimentId) {
    return commercialPlanRepository.findByExperimentReference(experimentId).stream()
        .findFirst()
        .map(
            plan -> {
              List<com.marketinghub.planning.CommercialPlanVisualAsset> approved =
                  commercialPlanVisualAssetRepository
                      .findByCommercialPlanIdAndStatusOrderByCreatedAtAsc(
                          plan.getId(), CommercialPlanVisualAssetStatus.APPROVED)
                      .stream()
                      .filter(asset -> "IMAGE".equalsIgnoreCase(asset.getMediaType()))
                      .filter(asset -> assetHasPurpose(asset, "ADS"))
                      .filter(
                          asset ->
                              asset.getAgentReviewStatus()
                                  == CommercialPlanVisualAssetReviewStatus.APPROVED)
                      .toList();
              List<com.marketinghub.planning.CommercialPlanVisualAsset> balanced =
                  new ArrayList<>();
              approved.stream()
                  .filter(asset -> assetLabelContains(asset, "post"))
                  .findFirst()
                  .ifPresent(balanced::add);
              approved.stream()
                  .filter(asset -> assetLabelContains(asset, "story"))
                  .findFirst()
                  .ifPresent(balanced::add);
              approved.stream()
                  .filter(asset -> !balanced.contains(asset))
                  .limit(Math.max(0, 3 - balanced.size()))
                  .forEach(balanced::add);
              return balanced.stream()
                  .map(com.marketinghub.planning.CommercialPlanVisualAsset::getAssetUrl)
                  .filter(StringUtils::hasText)
                  .map(String::trim)
                  .distinct()
                  .limit(3)
                  .toList();
            })
        .orElseGet(List::of);
  }

  /** Identifica o formato funcional documentado no rótulo sem inferir pelo arquivo remoto. */
  private boolean assetLabelContains(CommercialPlanVisualAsset asset, String marker) {
    return StringUtils.hasText(asset.getLabel())
        && asset.getLabel().toLowerCase(java.util.Locale.ROOT).contains(marker);
  }

  /** Reconhece finalidades múltiplas preservando compatibilidade com o campo singular legado. */
  private boolean assetHasPurpose(CommercialPlanVisualAsset asset, String purpose) {
    if (purpose.equalsIgnoreCase(asset.getPurpose())) {
      return true;
    }
    if (!StringUtils.hasText(asset.getPurposesJson())) {
      return false;
    }
    try {
      JsonNode values = objectMapper.readTree(asset.getPurposesJson());
      if (!values.isArray()) {
        return false;
      }
      for (JsonNode value : values) {
        if (purpose.equalsIgnoreCase(value.asText())) {
          return true;
        }
      }
      return false;
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao ler finalidades da Biblioteca Audiovisual. assetId={} purpose={}",
          asset.getId(),
          purpose,
          ex);
      return false;
    }
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

  /** Recebe a arte materializada pelo recurso de Dédalo e abre sua revisão independente. */
  @Transactional
  public Creative uploadAgentImprovementArtifact(
      Long id,
      MultipartFile file,
      String model,
      String producerExecutionId,
      String requestJson,
      String responseJson,
      String usageJson,
      BigDecimal costUsd)
      throws IOException {
    Creative source = repository.findByIdWithExperiment(id).orElseThrow();
    com.marketinghub.planning.CommercialPlan plan =
        commercialPlanRepository.findByExperimentReference(source.getExperiment().getId()).stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Retrabalho visual exige plano comercial vinculado ao experimento"));
    if (!StringUtils.hasText(producerExecutionId)) {
      throw new IllegalArgumentException("Retrabalho visual exige execução produtora de Dédalo");
    }
    AssetUploadResponse asset =
        uploadImage(
            file,
            model,
            requestJson,
            "Arte materializada pelo recurso técnico de Dédalo para a correção do criativo #" + id,
            AssetUploadCategory.EXPERIMENT_CREATIVE,
            source.getExperiment().getId(),
            null,
            null);
    assetRepository.findByUrlIn(List.of(asset.url())).stream()
        .findFirst()
        .ifPresent(
            media -> {
              media.setProvider(MediaProvider.OPENAI);
              assetRepository.save(media);
            });

    CommercialPlanVisualAsset visual = new CommercialPlanVisualAsset();
    visual.setCommercialPlan(plan);
    visual.setSourceVisualAsset(findLibrarySource(plan.getId(), source.getImageUrl()));
    visual.setAssetUrl(asset.url());
    visual.setMediaType("IMAGE");
    visual.setLabel("Entregável premium para o criativo #" + id);
    visual.setPurpose("DELIVERY");
    visual.setPurposesJson("[\"DELIVERY\",\"LANDING\",\"ADS\",\"SOCIAL\"]");
    visual.setOrigin("Dédalo / recurso técnico GPT Image 2");
    visual.setRightsStatement("Gerado para uso comercial e entrega deste produto");
    visual.setVersionNumber(
        visual.getSourceVisualAsset() == null
            ? 1
            : Objects.requireNonNullElse(visual.getSourceVisualAsset().getVersionNumber(), 1) + 1);
    visual.setStatus(CommercialPlanVisualAssetStatus.DRAFT);
    visual.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.PENDING);
    commercialPlanVisualAssetRepository.save(visual);

    CommercialPlanImageStudioJob job = new CommercialPlanImageStudioJob();
    job.setCommercialPlan(plan);
    job.setSourceVisualAsset(visual.getSourceVisualAsset());
    job.setResultVisualAsset(visual);
    job.setSourceCreative(source);
    job.setOperation(CommercialPlanImageStudioOperation.EDIT);
    job.setStatus(CommercialPlanImageStudioStatus.COMPLETED);
    job.setLabel(visual.getLabel());
    job.setPrompt(requestJson == null ? "Retrabalho visual de Dédalo" : requestJson);
    job.setPurposesJson(visual.getPurposesJson());
    job.setReferenceAssetIdsJson("[]");
    String improvementSize =
        Objects.toString(source.getFormat(), "")
                .toLowerCase(java.util.Locale.ROOT)
                .contains("story")
            ? "1152x2048"
            : "2048x2048";
    job.setSize(improvementSize);
    job.setQuality("high");
    job.setModel(model);
    JsonNode frozenCorrection = readImprovementJson(source);
    job.setPlaybookVersion(
        frozenCorrection.path("playbookVersion").asText("temis-visual-playbook-v1"));
    job.setPlaybookContextKey(
        frozenCorrection
            .path("playbookContextKey")
            .asText(
                temisVisualPlaybookService.contextKey(
                    plan, source.getFormat(), List.of("ADS"), improvementSize)));
    job.setPlaybookJson(frozenCorrection.path("visualPlaybook").toString());
    job.setProducerExecutionId(producerExecutionId.trim());
    job.setRequestJson(requestJson);
    job.setResponseJson(responseJson);
    job.setUsageJson(usageJson);
    job.setCostUsd(costUsd);
    job.setStartedAt(Instant.now());
    job.setFinishedAt(Instant.now());
    commercialPlanImageStudioJobRepository.save(job);
    repository.save(source);
    return source;
  }

  /** Localiza na biblioteca a imagem original do criativo sem cruzar planos. */
  private CommercialPlanVisualAsset findLibrarySource(Long planId, String imageUrl) {
    if (!StringUtils.hasText(imageUrl)) {
      return null;
    }
    return commercialPlanVisualAssetRepository
        .findByCommercialPlanIdOrderByCreatedAtAsc(planId)
        .stream()
        .filter(asset -> imageUrl.trim().equals(asset.getAssetUrl()))
        .findFirst()
        .orElse(null);
  }

  /** Promove o arquivo aprovado pela Biblioteca para uma nova versão do criativo. */
  @Transactional
  public Creative completeApprovedLibraryImprovement(
      Long creativeId,
      CreativeImprovementResultRequest result,
      String usageJson,
      String reviewSummary) {
    Creative revision = completeAgentImprovement(creativeId, result);
    Creative source = repository.findByIdWithExperiment(creativeId).orElseThrow();
    source.setAgentImprovementJson(
        improvementAuditJson(
            readImprovementJson(source), result, usageJson, reviewSummary, "APPROVED"));
    repository.save(source);
    return revision;
  }

  /** Devolve à fila o retrabalho reprovado com a causa visual acrescentada ao prompt. */
  @Transactional
  public void requeueLibraryImprovement(Long creativeId, String reviewSummary) {
    Creative source = repository.findByIdWithExperiment(creativeId).orElseThrow();
    JsonNode correction = readImprovementJson(source);
    if (correction instanceof com.fasterxml.jackson.databind.node.ObjectNode object) {
      String prior = correction.path("imagePrompt").asText("");
      object.put(
          "imagePrompt",
          prior + "\n\nCORREÇÃO OBRIGATÓRIA DA REVISÃO DA BIBLIOTECA: " + reviewSummary);
    }
    source.setAgentImprovementJson(correction.toString());
    source.setAgentImprovementStatus(CreativeImprovementStatus.PENDING);
    source.setAgentImprovementError(reviewSummary);
    repository.save(source);
  }

  /** Encerra uma revisão técnica falha sem liberar o criativo ou apagar o diagnóstico. */
  @Transactional
  public void failLibraryImprovement(Long creativeId, String error) {
    Creative source = repository.findByIdWithExperiment(creativeId).orElseThrow();
    source.setAgentImprovementStatus(CreativeImprovementStatus.FAILED);
    source.setAgentImprovementError(
        StringUtils.hasText(error) ? error.trim() : "Revisão da Biblioteca falhou tecnicamente");
    repository.save(source);
  }

  /** Preserva o contrato funcional, interação bruta e parecer no mesmo ciclo. */
  private String improvementAuditJson(
      JsonNode correction,
      CreativeImprovementResultRequest result,
      String usageJson,
      String reviewSummary,
      String status) {
    try {
      Map<String, Object> audit = new LinkedHashMap<>();
      audit.put("correction", correction);
      audit.put("executor", "DEDALO_TECHNICAL_RESOURCE");
      audit.put("status", status);
      audit.put("requestJson", Objects.toString(result.requestJson(), ""));
      audit.put("responseJson", Objects.toString(result.responseJson(), ""));
      audit.put("usageJson", Objects.toString(usageJson, ""));
      audit.put("costUsd", result.costUsd());
      audit.put("reviewSummary", Objects.toString(reviewSummary, ""));
      return objectMapper.writeValueAsString(audit);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao auditar melhoria visual de Dédalo. status={}", status, ex);
      throw new IllegalStateException("Falha ao auditar melhoria visual de Dédalo", ex);
    }
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
      creative.setAgentImprovementError("Limite seguro de oito correções automáticas atingido");
      return;
    }
    if (!hasCorrectionTarget(request, "CREATIVE_MEDIA")) {
      creative.setAgentImprovementStatus(CreativeImprovementStatus.DELEGATED);
      creative.setAgentImprovementError(null);
      return;
    }
    if (normalizedItems(request.mandatoryVisualRequirements()).isEmpty()
        || normalizedItems(request.visualAcceptanceCriteria()).isEmpty()) {
      creative.setAgentImprovementStatus(CreativeImprovementStatus.FAILED);
      creative.setAgentImprovementError(
          "Agente não forneceu requisitos e critérios visuais verificáveis");
      return;
    }
    creative.setAgentImprovementJson(toImprovementJson(creative, request));
    creative.setAgentImprovementStatus(CreativeImprovementStatus.PENDING);
    creative.setAgentImprovementError(null);
  }

  /** Serializa o briefing de Dédalo sem converter o parecer de Têmis em conteúdo substituto. */
  private String toImprovementJson(Creative creative, CreativeAgentReviewResultRequest request) {
    try {
      Map<String, Object> correction = new LinkedHashMap<>();
      correction.put("headline", Objects.requireNonNullElse(creative.getHeadline(), ""));
      correction.put("primaryText", Objects.requireNonNullElse(creative.getPrimaryText(), ""));
      correction.put("description", Objects.requireNonNullElse(creative.getDescription(), ""));
      correction.put(
          "cta", Objects.requireNonNullElse(creative.getCta(), DEFAULT_META_CALL_TO_ACTION));
      correction.put("imagePrompt", dedaloImageBrief(request));
      correction.put(
          "mandatoryVisualRequirements", normalizedItems(request.mandatoryVisualRequirements()));
      correction.put("forbiddenVisualElements", normalizedItems(request.forbiddenVisualElements()));
      correction.put(
          "visualAcceptanceCriteria", normalizedItems(request.visualAcceptanceCriteria()));
      correction.put("correctionTargets", correctionTargets(request));
      return objectMapper.writeValueAsString(correction);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar correção do agente. creativeId desconhecido", ex);
      throw new IllegalStateException("Correção do agente inválida", ex);
    }
  }

  /** Confirma que o parecer realmente delegou a materialização visual a Dédalo. */
  private boolean hasCorrectionTarget(CreativeAgentReviewResultRequest request, String target) {
    return correctionTargets(request).stream()
        .filter(Objects::nonNull)
        .anyMatch(item -> target.equals(item.target()));
  }

  /** Converte requisitos de integridade em briefing técnico sem inventar a solução criativa. */
  private String dedaloImageBrief(CreativeAgentReviewResultRequest request) {
    StringBuilder brief =
        new StringBuilder(
            "Materialize uma nova imagem do criativo sob o contrato PDE_CONSTRUCTION. "
                + "Escolha a solução visual sem alterar estratégia, oferta, preço ou prova.");
    correctionTargets(request).stream()
        .filter(Objects::nonNull)
        .filter(item -> "CREATIVE_MEDIA".equals(item.target()))
        .forEach(
            item ->
                brief
                    .append("\n\nFalha ")
                    .append(item.issueCode())
                    .append(": ")
                    .append(item.requirement())
                    .append(" Critério de aceite: ")
                    .append(item.acceptanceCriterion()));
    return brief.toString();
  }

  /** Preserva o tipo dos alvos ao normalizar contratos legados sem a lista de correções. */
  private List<CreativeAgentReviewResultRequest.ConvergenceCorrectionTarget> correctionTargets(
      CreativeAgentReviewResultRequest request) {
    return request.correctionTargets() == null ? List.of() : request.correctionTargets();
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

  /**
   * Impede aprovação sem notas mínimas e pareceres especialistas sobre as três dimensões
   * comerciais.
   */
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
