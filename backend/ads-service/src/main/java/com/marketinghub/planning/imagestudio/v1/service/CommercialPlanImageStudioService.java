package com.marketinghub.planning.imagestudio.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.creative.dto.CreativeImprovementResultRequest;
import com.marketinghub.creative.service.CreativeService;
import com.marketinghub.media.Asset;
import com.marketinghub.media.AssetStatus;
import com.marketinghub.media.AssetType;
import com.marketinghub.media.MediaProvider;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.CommercialPlanVisualAsset;
import com.marketinghub.planning.CommercialPlanVisualAssetStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioJob;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanVisualAssetReviewStatus;
import com.marketinghub.repository.jpa.media.AssetRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanImageStudioJobSummary;
import com.marketinghub.repository.jpa.planning.CommercialPlanVisualAssetRepository;
import com.marketinghub.storage.AssetStorageService;
import com.marketinghub.storage.AssetUploadCategory;
import com.marketinghub.storage.AssetUploadContext;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** Responsabilidade: orquestrar criação, edição, persistência e revisão de imagens por Têmis. */
@Service
public class CommercialPlanImageStudioService {
  private static final Logger log = LoggerFactory.getLogger(CommercialPlanImageStudioService.class);
  private static final Set<String> PURPOSES = Set.of("DELIVERY", "LANDING", "ADS", "SOCIAL");
  private static final Set<String> SIZES =
      Set.of("1024x1024", "1024x1536", "1536x1024", "2048x2048", "2048x1152", "1152x2048");
  private static final Set<String> QUALITIES = Set.of("medium", "high");
  private static final Duration EXECUTION_LEASE = Duration.ofMinutes(50);
  private final com.marketinghub.planning.service.CommercialPlanService planService;
  private final CommercialPlanImageStudioJobRepository jobRepository;
  private final CommercialPlanVisualAssetRepository visualAssetRepository;
  private final AssetStorageService assetStorageService;
  private final AssetRepository assetRepository;
  private final ObjectMapper objectMapper;
  private final CreativeService creativeService;

  /** Inicializa o estúdio com as fontes de verdade de plano, fila, biblioteca e storage. */
  public CommercialPlanImageStudioService(
      com.marketinghub.planning.service.CommercialPlanService planService,
      CommercialPlanImageStudioJobRepository jobRepository,
      CommercialPlanVisualAssetRepository visualAssetRepository,
      AssetStorageService assetStorageService,
      AssetRepository assetRepository,
      ObjectMapper objectMapper,
      CreativeService creativeService) {
    this.planService = planService;
    this.jobRepository = jobRepository;
    this.visualAssetRepository = visualAssetRepository;
    this.assetStorageService = assetStorageService;
    this.assetRepository = assetRepository;
    this.objectMapper = objectMapper;
    this.creativeService = creativeService;
  }

  /** Cria uma pendência sem consumir modelo e sem substituir qualquer arquivo existente. */
  @Transactional
  public CommercialPlanImageStudioJobDto create(
      Long planId, CreateCommercialPlanImageStudioJobRequest request) {
    CommercialPlan plan = planService.getPlan(planId);
    CommercialPlanImageStudioOperation operation =
        Objects.requireNonNullElse(request.operation(), CommercialPlanImageStudioOperation.CREATE);
    String prompt = requireText(request.prompt(), "prompt");
    String label = requireText(request.label(), "label");
    List<String> purposes = normalizePurposes(request.purposes());
    CommercialPlanVisualAsset source = resolveSource(planId, operation, request.sourceAssetId());
    List<Long> references = normalizeReferences(planId, source, request.referenceAssetIds());
    Optional<CommercialPlanImageStudioJobSummary> equivalent =
        jobRepository
            .findEquivalentSummaries(
                planId,
                source == null ? null : source.getId(),
                operation,
                label,
                prompt,
                CommercialPlanImageStudioStatus.FAILED)
            .stream()
            .findFirst();
    if (equivalent.isPresent()) {
      return dto(equivalent.get());
    }
    CommercialPlanImageStudioJob job = new CommercialPlanImageStudioJob();
    job.setCommercialPlan(plan);
    job.setSourceVisualAsset(source);
    job.setOperation(operation);
    job.setStatus(CommercialPlanImageStudioStatus.PENDING);
    job.setPrompt(prompt);
    job.setLabel(label);
    job.setPurposesJson(writeJson(purposes));
    job.setReferenceAssetIdsJson(writeJson(references));
    job.setSize(normalizeOption(request.size(), SIZES, "1024x1536", "size"));
    job.setQuality(normalizeOption(request.quality(), QUALITIES, "high", "quality"));
    return dto(jobRepository.save(job));
  }

  /** Lista a prestação de contas do estúdio para a tela do plano. */
  @Transactional(readOnly = true)
  public List<CommercialPlanImageStudioJobDto> list(Long planId) {
    planService.getPlan(planId);
    return jobRepository.findSummariesByCommercialPlanId(planId).stream().map(this::dto).toList();
  }

  /** Reserva jobs para Têmis e entrega apenas referências autorizadas do próprio plano. */
  @Transactional
  public List<CommercialPlanImageStudioPendingDto> claimPending(int limit) {
    return jobRepository
        .findClaimable(
            CommercialPlanImageStudioStatus.PENDING,
            CommercialPlanImageStudioStatus.PROCESSING,
            Instant.now().minus(EXECUTION_LEASE))
        .stream()
        .limit(Math.max(1, limit))
        .map(this::claimSafely)
        .flatMap(Optional::stream)
        .toList();
  }

  /** Conclui a produção, cria um novo entregável DRAFT e abre revisão independente. */
  @Transactional
  public CommercialPlanImageStudioJobDto complete(
      Long jobId,
      String producerExecutionId,
      MultipartFile file,
      String model,
      String requestJson,
      String responseJson,
      String usageJson,
      BigDecimal costUsd)
      throws IOException {
    CommercialPlanImageStudioJob job = processingJob(jobId, producerExecutionId);
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Imagem produzida não foi enviada");
    }
    AssetStorageService.StoredObject stored =
        assetStorageService.store(
            file,
            new AssetUploadContext(
                AssetUploadCategory.COMMERCIAL_PLAN_DELIVERABLE,
                job.getCommercialPlan().getExperiment() != null
                    ? job.getCommercialPlan().getExperiment().getId()
                    : null,
                null,
                "commercial-plan-" + job.getCommercialPlan().getId()));
    Asset media =
        Asset.builder()
            .type(AssetType.IMAGE)
            .provider(MediaProvider.OPENAI)
            .status(AssetStatus.READY)
            .url(stored.publicUrl())
            .externalId(stored.storedFileName())
            .model(clean(model))
            .prompt(job.getPrompt())
            .payload(assetPayload(job, stored, requestJson, responseJson, usageJson))
            .build();
    assetRepository.save(media);

    CommercialPlanVisualAsset visual = new CommercialPlanVisualAsset();
    visual.setCommercialPlan(job.getCommercialPlan());
    visual.setSourceVisualAsset(job.getSourceVisualAsset());
    visual.setAssetUrl(stored.publicUrl());
    visual.setMediaType("IMAGE");
    visual.setLabel(job.getLabel());
    List<String> purposes = readStrings(job.getPurposesJson());
    visual.setPurpose(purposes.getFirst());
    visual.setPurposesJson(job.getPurposesJson());
    visual.setOrigin("Têmis / GPT Image 2");
    visual.setRightsStatement("Gerado para uso comercial e entrega deste produto");
    visual.setVersionNumber(nextVersion(job));
    visual.setStatus(CommercialPlanVisualAssetStatus.DRAFT);
    visual.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.PENDING);
    visualAssetRepository.save(visual);

    job.setResultVisualAsset(visual);
    job.setStatus(CommercialPlanImageStudioStatus.COMPLETED);
    job.setModel(clean(model));
    job.setRequestJson(requestJson);
    job.setResponseJson(responseJson);
    job.setUsageJson(usageJson);
    job.setCostUsd(costUsd);
    job.setError(null);
    job.setFinishedAt(Instant.now());
    return dto(jobRepository.save(job));
  }

  /** Registra falha técnica sem apagar a origem nem promover um entregável incompleto. */
  @Transactional
  public CommercialPlanImageStudioJobDto fail(
      Long jobId,
      String producerExecutionId,
      String error,
      String requestJson,
      String responseJson) {
    CommercialPlanImageStudioJob job = processingJob(jobId, producerExecutionId);
    job.setStatus(CommercialPlanImageStudioStatus.FAILED);
    job.setError(requireText(error, "error"));
    job.setRequestJson(requestJson);
    job.setResponseJson(responseJson);
    job.setFinishedAt(Instant.now());
    return dto(jobRepository.save(job));
  }

  /** Reserva entregáveis DRAFT para uma execução de revisão distinta da produção. */
  @Transactional
  public List<CommercialPlanVisualAssetReviewPendingDto> claimReviews(int limit) {
    return visualAssetRepository
        .findClaimableReviews(
            CommercialPlanVisualAssetReviewStatus.PENDING,
            CommercialPlanVisualAssetReviewStatus.PROCESSING,
            Instant.now().minus(EXECUTION_LEASE))
        .stream()
        .limit(Math.max(1, limit))
        .map(this::claimReview)
        .toList();
  }

  /** Retorna ao MCP o snapshot vigente sem alterar a fila ou a decisão. */
  @Transactional(readOnly = true)
  public CommercialPlanVisualAssetReviewPendingDto reviewContext(Long assetId, Long planId) {
    CommercialPlanVisualAsset asset = visualAssetRepository.findById(assetId).orElseThrow();
    if (!planId.equals(asset.getCommercialPlan().getId())) {
      throw new IllegalArgumentException("Entregável não pertence ao plano informado");
    }
    return reviewSnapshot(asset, jobFor(assetId));
  }

  /** Aplica o parecer independente e só aprova a biblioteca quando o gate retornar APPROVED. */
  @Transactional
  public void review(Long assetId, CommercialPlanVisualAssetReviewResultRequest request) {
    CommercialPlanVisualAsset asset = visualAssetRepository.findById(assetId).orElseThrow();
    CommercialPlanImageStudioJob job = jobFor(assetId);
    if (!StringUtils.hasText(request.reviewerExecutionId())
        || request.reviewerExecutionId().equals(job.getProducerExecutionId())) {
      throw new IllegalArgumentException("A revisão deve usar execução diferente da produção");
    }
    CommercialPlanVisualAssetReviewStatus decision = Objects.requireNonNull(request.decision());
    if (decision != CommercialPlanVisualAssetReviewStatus.APPROVED
        && decision != CommercialPlanVisualAssetReviewStatus.ADJUST
        && decision != CommercialPlanVisualAssetReviewStatus.FAILED) {
      throw new IllegalArgumentException("Decisão de revisão inválida");
    }
    if ((decision == CommercialPlanVisualAssetReviewStatus.APPROVED
            || decision == CommercialPlanVisualAssetReviewStatus.ADJUST)
        && !StringUtils.hasText(request.summary())) {
      throw new IllegalArgumentException("Aprovação ou ajuste exige parecer funcional");
    }
    asset.setReviewerExecutionId(request.reviewerExecutionId());
    asset.setAgentReviewStartedAt(null);
    asset.setAgentReviewStatus(decision);
    asset.setAgentReviewJson(
        writeJson(
            Map.of("decision", decision, "summary", Objects.toString(request.summary(), ""))));
    asset.setAgentReviewRequestJson(request.requestJson());
    asset.setAgentReviewResponseJson(request.responseJson());
    asset.setStatus(
        decision == CommercialPlanVisualAssetReviewStatus.APPROVED
            ? CommercialPlanVisualAssetStatus.APPROVED
            : CommercialPlanVisualAssetStatus.DRAFT);
    visualAssetRepository.save(asset);
    if (decision == CommercialPlanVisualAssetReviewStatus.APPROVED
        && job.getSourceCreative() != null) {
      creativeService.completeApprovedLibraryImprovement(
          job.getSourceCreative().getId(),
          new CreativeImprovementResultRequest(
              asset.getAssetUrl(),
              job.getCostUsd(),
              job.getRequestJson(),
              job.getResponseJson(),
              null),
          job.getUsageJson(),
          request.summary());
    } else if (decision == CommercialPlanVisualAssetReviewStatus.ADJUST
        && job.getSourceCreative() != null) {
      creativeService.requeueLibraryImprovement(job.getSourceCreative().getId(), request.summary());
    } else if (decision == CommercialPlanVisualAssetReviewStatus.FAILED
        && job.getSourceCreative() != null) {
      creativeService.failLibraryImprovement(job.getSourceCreative().getId(), request.error());
    }
  }

  /** Converte um job reservado em contrato executável do módulo Têmis. */
  private CommercialPlanImageStudioPendingDto claim(CommercialPlanImageStudioJob job) {
    List<String> references = referenceUrls(job);
    job.setStatus(CommercialPlanImageStudioStatus.PROCESSING);
    job.setProducerExecutionId(UUID.randomUUID().toString());
    job.setStartedAt(Instant.now());
    job.setError(null);
    jobRepository.save(job);
    return new CommercialPlanImageStudioPendingDto(
        job.getId(),
        job.getCommercialPlan().getId(),
        job.getOperation(),
        job.getPrompt(),
        job.getLabel(),
        readStrings(job.getPurposesJson()),
        job.getSize(),
        job.getQuality(),
        references,
        job.getProducerExecutionId());
  }

  /** Falha uma reserva cuja referência perdeu validade sem degradar edição para geração livre. */
  private Optional<CommercialPlanImageStudioPendingDto> claimSafely(
      CommercialPlanImageStudioJob job) {
    try {
      return Optional.of(claim(job));
    } catch (RuntimeException ex) {
      log.error(
          "Job visual bloqueado antes do consumo por referência inválida. jobId={} planId={}",
          job.getId(),
          job.getCommercialPlan().getId(),
          ex);
      job.setStatus(CommercialPlanImageStudioStatus.FAILED);
      job.setError("Referência visual deixou de ser válida: " + rootMessage(ex));
      job.setFinishedAt(Instant.now());
      jobRepository.save(job);
      return Optional.empty();
    }
  }

  /** Marca o asset como PROCESSING e monta o snapshot comercial congelado para o revisor. */
  private CommercialPlanVisualAssetReviewPendingDto claimReview(CommercialPlanVisualAsset asset) {
    CommercialPlanImageStudioJob job = jobFor(asset.getId());
    asset.setAgentReviewStatus(CommercialPlanVisualAssetReviewStatus.PROCESSING);
    asset.setAgentReviewStartedAt(Instant.now());
    visualAssetRepository.save(asset);
    return reviewSnapshot(asset, job);
  }

  /** Monta o mesmo snapshot para reserva e consulta do MCP independente. */
  private CommercialPlanVisualAssetReviewPendingDto reviewSnapshot(
      CommercialPlanVisualAsset asset, CommercialPlanImageStudioJob job) {
    CommercialPlan plan = asset.getCommercialPlan();
    return new CommercialPlanVisualAssetReviewPendingDto(
        asset.getId(),
        job.getId(),
        plan.getId(),
        plan.getName(),
        plan.getMainOffer(),
        plan.getTargetAudience(),
        asset.getAssetUrl(),
        asset.getLabel(),
        readStrings(asset.getPurposesJson()),
        job.getProducerExecutionId());
  }

  /** Revalida as referências no consumo e impede qualquer downgrade silencioso da edição. */
  private List<String> referenceUrls(CommercialPlanImageStudioJob job) {
    List<String> result = new ArrayList<>();
    for (Long id : readLongs(job.getReferenceAssetIdsJson())) {
      CommercialPlanVisualAsset asset =
          visualAssetRepository
              .findById(id)
              .orElseThrow(() -> new IllegalArgumentException("Referência não encontrada: " + id));
      boolean source =
          job.getSourceVisualAsset() != null && id.equals(job.getSourceVisualAsset().getId());
      if (!asset.getCommercialPlan().getId().equals(job.getCommercialPlan().getId())
          || !"IMAGE".equals(asset.getMediaType())
          || asset.getStatus() == CommercialPlanVisualAssetStatus.RETIRED
          || (!source && asset.getStatus() != CommercialPlanVisualAssetStatus.APPROVED)
          || !StringUtils.hasText(asset.getAssetUrl())) {
        throw new IllegalArgumentException("Referência sem aprovação vigente: " + id);
      }
      if (!result.contains(asset.getAssetUrl())) {
        result.add(asset.getAssetUrl());
      }
    }
    if (job.getOperation() == CommercialPlanImageStudioOperation.EDIT && result.isEmpty()) {
      throw new IllegalArgumentException("Edição perdeu sua imagem de origem");
    }
    return result;
  }

  /** Valida a operação EDIT e impede referência cruzada entre planos. */
  private CommercialPlanVisualAsset resolveSource(
      Long planId, CommercialPlanImageStudioOperation operation, Long sourceAssetId) {
    if (operation == CommercialPlanImageStudioOperation.CREATE) {
      if (sourceAssetId != null) {
        throw new IllegalArgumentException("Criação não aceita imagem de origem; use referências");
      }
      return null;
    }
    if (sourceAssetId == null) {
      throw new IllegalArgumentException("Edição exige uma imagem de origem");
    }
    CommercialPlanVisualAsset source = visualAssetRepository.findById(sourceAssetId).orElseThrow();
    if (!planId.equals(source.getCommercialPlan().getId())
        || !"IMAGE".equals(source.getMediaType())
        || source.getStatus() == CommercialPlanVisualAssetStatus.RETIRED) {
      throw new IllegalArgumentException(
          "Imagem de origem não pertence à biblioteca ativa do plano");
    }
    return source;
  }

  /** Normaliza a composição e aceita como apoio apenas referências aprovadas do mesmo plano. */
  private List<Long> normalizeReferences(
      Long planId, CommercialPlanVisualAsset source, List<Long> requested) {
    List<Long> ids = new ArrayList<>();
    if (source != null) {
      ids.add(source.getId());
    }
    if (requested != null) {
      requested.stream().filter(Objects::nonNull).distinct().limit(4).forEach(ids::add);
    }
    List<Long> unique = ids.stream().distinct().limit(4).toList();
    unique.forEach(
        id -> {
          CommercialPlanVisualAsset asset = visualAssetRepository.findById(id).orElseThrow();
          if (!planId.equals(asset.getCommercialPlan().getId())
              || !"IMAGE".equals(asset.getMediaType())
              || asset.getStatus() == CommercialPlanVisualAssetStatus.RETIRED
              || (!id.equals(source == null ? null : source.getId())
                  && asset.getStatus() != CommercialPlanVisualAssetStatus.APPROVED)) {
            throw new IllegalArgumentException("Referência visual inválida para este plano");
          }
        });
    return unique;
  }

  /** Exige DELIVERY e restringe o reuso às finalidades canônicas do produto. */
  private List<String> normalizePurposes(List<String> values) {
    List<String> normalized =
        values == null
            ? List.of()
            : values.stream()
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toUpperCase())
                .distinct()
                .toList();
    if (!normalized.contains("DELIVERY")
        || normalized.stream().anyMatch(value -> !PURPOSES.contains(value))) {
      throw new IllegalArgumentException(
          "Entregáveis de Têmis exigem DELIVERY e aceitam apenas DELIVERY, LANDING, ADS e SOCIAL");
    }
    return normalized;
  }

  /** Garante que o callback pertence à reserva vigente do backend. */
  private CommercialPlanImageStudioJob processingJob(Long jobId, String producerExecutionId) {
    CommercialPlanImageStudioJob job = jobRepository.findById(jobId).orElseThrow();
    if (job.getStatus() != CommercialPlanImageStudioStatus.PROCESSING
        || !Objects.equals(job.getProducerExecutionId(), producerExecutionId)) {
      throw new IllegalArgumentException("Execução de produção não corresponde à reserva vigente");
    }
    return job;
  }

  /** Localiza o job produtor canônico de um asset gerado. */
  private CommercialPlanImageStudioJob jobFor(Long assetId) {
    return jobRepository.findByResultVisualAssetId(assetId).orElseThrow();
  }

  /** Calcula a versão sem sobrescrever a origem aprovada. */
  private int nextVersion(CommercialPlanImageStudioJob job) {
    return job.getSourceVisualAsset() == null
        ? 1
        : Objects.requireNonNullElse(job.getSourceVisualAsset().getVersionNumber(), 1) + 1;
  }

  /** Monta auditoria do arquivo armazenado e da chamada ao modelo. */
  private String assetPayload(
      CommercialPlanImageStudioJob job,
      AssetStorageService.StoredObject stored,
      String requestJson,
      String responseJson,
      String usageJson) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("imageStudioJobId", job.getId());
    payload.put("commercialPlanId", job.getCommercialPlan().getId());
    payload.put("producerExecutionId", job.getProducerExecutionId());
    payload.put("storedFileName", stored.storedFileName());
    payload.put("requestJson", Objects.toString(requestJson, ""));
    payload.put("responseJson", Objects.toString(responseJson, ""));
    payload.put("usageJson", Objects.toString(usageJson, ""));
    return writeJson(payload);
  }

  /** Converte a entidade para o contrato administrativo. */
  private CommercialPlanImageStudioJobDto dto(CommercialPlanImageStudioJob job) {
    return new CommercialPlanImageStudioJobDto(
        job.getId(),
        job.getCommercialPlan().getId(),
        job.getSourceVisualAsset() != null ? job.getSourceVisualAsset().getId() : null,
        job.getResultVisualAsset() != null ? job.getResultVisualAsset().getId() : null,
        job.getOperation(),
        job.getStatus(),
        job.getLabel(),
        job.getPrompt(),
        readStrings(job.getPurposesJson()),
        job.getSize(),
        job.getQuality(),
        job.getModel(),
        job.getCostUsd(),
        job.getError(),
        job.getStartedAt(),
        job.getFinishedAt(),
        job.getCreatedAt());
  }

  /** Converte a projeção leve sem materializar payloads brutos antigos no heap. */
  private CommercialPlanImageStudioJobDto dto(CommercialPlanImageStudioJobSummary job) {
    return new CommercialPlanImageStudioJobDto(
        job.id(),
        job.commercialPlanId(),
        job.sourceAssetId(),
        job.resultAssetId(),
        job.operation(),
        job.status(),
        job.label(),
        job.prompt(),
        readStrings(job.purposesJson()),
        job.size(),
        job.quality(),
        job.model(),
        job.costUsd(),
        job.error(),
        job.startedAt(),
        job.finishedAt(),
        job.createdAt());
  }

  /** Serializa dados estruturados preservando o contrato auditável. */
  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Contrato JSON do Estúdio de Imagens inválido", ex);
    }
  }

  /** Lê uma lista textual persistida pelo backend. */
  private List<String> readStrings(String value) {
    try {
      return objectMapper.readValue(
          value, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Lista textual do Estúdio de Imagens inválida", ex);
    }
  }

  /** Lê identificadores de referência persistidos pelo backend. */
  private List<Long> readLongs(String value) {
    try {
      return objectMapper.readValue(
          value, objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Referências do Estúdio de Imagens inválidas", ex);
    }
  }

  /** Valida e normaliza opções fechadas da API de imagens. */
  private String normalizeOption(String value, Set<String> allowed, String fallback, String field) {
    String normalized = StringUtils.hasText(value) ? value.trim().toLowerCase() : fallback;
    if (!allowed.contains(normalized)) {
      throw new IllegalArgumentException(field + " inválido");
    }
    return normalized;
  }

  /** Exige texto operacional preenchido. */
  private String requireText(String value, String field) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(field + " é obrigatório");
    }
    return value.trim();
  }

  /** Limpa metadados opcionais antes da persistência. */
  private String clean(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Extrai a causa específica preservando o stack trace completo no log anterior. */
  private String rootMessage(Throwable error) {
    Throwable current = error;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return Objects.toString(current.getMessage(), current.getClass().getSimpleName());
  }
}
