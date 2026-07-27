package com.marketinghub.mds.productevidence.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.hypothesis.pain.HypothesisPainStageExecution;
import com.marketinghub.mds.productevidence.v1.MdsProductEvidenceStageExecution;
import com.marketinghub.niche.MarketNiche;
import com.marketinghub.repository.jpa.hypothesis.HypothesisPainStageExecutionRepository;
import com.marketinghub.repository.jpa.hypothesis.HypothesisRepository;
import com.marketinghub.repository.jpa.mds.MdsProductEvidenceStageExecutionRepository;
import com.marketinghub.repository.jpa.niche.MarketNicheRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: orquestrar no backend o pacote científico obrigatório antes da oferta. */
@Service
public class ProductEvidenceWorkflowService {
  private static final String SOURCE_DISCOVERY = "source-discovery";
  private static final String EVIDENCE_SYNTHESIS = "evidence-synthesis";
  private static final String DELIVERABLE_COMPOSER = "deliverable-composer";
  private static final String STATUS_PENDING = "PENDENTE";
  private static final String STATUS_PROCESSING = "PROCESSANDO";
  private static final String STATUS_COMPLETED = "CONCLUIDO";
  private static final String STATUS_BLOCKED = "BLOQUEADO";
  private static final String STATUS_FAILED = "FALHA";
  private static final String HYPOTHESIS_PAIN = "hypothesis-pain";
  private static final String HYPOTHESIS_RESULT = "hypothesis-result";
  private static final String HYPOTHESIS_MECHANISM = "hypothesis-mechanism";
  private static final String HYPOTHESIS_PROOF = "hypothesis-proof";
  private static final List<String> ACTIVE_STATUSES = List.of(STATUS_PENDING, STATUS_PROCESSING);
  private static final List<String> ALREADY_STARTED_STATUSES =
      List.of(STATUS_PENDING, STATUS_PROCESSING, STATUS_COMPLETED, STATUS_BLOCKED, STATUS_FAILED);

  private final MdsProductEvidenceStageExecutionRepository executionRepository;
  private final HypothesisPainStageExecutionRepository hypothesisExecutionRepository;
  private final HypothesisRepository hypothesisRepository;
  private final MarketNicheRepository marketNicheRepository;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com repositórios canônicos e serializador de contratos. */
  public ProductEvidenceWorkflowService(
      MdsProductEvidenceStageExecutionRepository executionRepository,
      HypothesisPainStageExecutionRepository hypothesisExecutionRepository,
      HypothesisRepository hypothesisRepository,
      MarketNicheRepository marketNicheRepository,
      ObjectMapper objectMapper) {
    this.executionRepository = executionRepository;
    this.hypothesisExecutionRepository = hypothesisExecutionRepository;
    this.hypothesisRepository = hypothesisRepository;
    this.marketNicheRepository = marketNicheRepository;
    this.objectMapper = objectMapper;
  }

  /** Garante que existe uma pesquisa científica iniciada para o nicho informado. */
  @Transactional
  public void ensureProductEvidenceStarted(Long marketNicheId) {
    if (hasApprovedEvidencePack(marketNicheId) || hasActiveEvidenceExecution(marketNicheId)) {
      return;
    }
    MarketNiche niche =
        marketNicheRepository
            .findById(marketNicheId)
            .orElseThrow(
                () -> new EntityNotFoundException("Market niche not found: " + marketNicheId));
    Map<String, Object> input = buildInitialInput(marketNicheId, niche);
    executionRepository.save(
        newExecution(
            marketNicheId,
            SOURCE_DISCOVERY,
            "mds-product-evidence-" + marketNicheId,
            productIdea(input),
            scientificQuestion(input),
            input));
  }

  /** Bloqueia o avanço comercial quando o pacote científico ainda não foi aprovado. */
  @Transactional
  public void requireApprovedEvidencePack(Long marketNicheId) {
    if (hasApprovedEvidencePack(marketNicheId)) {
      return;
    }
    ensureProductEvidenceStarted(marketNicheId);
    throw new IllegalStateException(
        "A base científica precisa estar concluída pelo scientific-research-worker antes de iniciar Oferta para o nicho: "
            + marketNicheId);
  }

  /** Informa se o nicho já possui pacote científico final concluído. */
  @Transactional(readOnly = true)
  public boolean hasApprovedEvidencePack(Long marketNicheId) {
    return executionRepository
        .findTopByMarketNicheIdAndStageCodeAndStatusOrderByCreatedAtDesc(
            marketNicheId, DELIVERABLE_COMPOSER, STATUS_COMPLETED)
        .isPresent();
  }

  /** Lista pendências por etapa e marca como em processamento para evitar captura duplicada. */
  @Transactional
  public List<ProductEvidenceStagePendingResponse> listPending(String stageCode, int limit) {
    String normalizedStage = normalizeStageCode(stageCode);
    Instant now = Instant.now();
    return executionRepository
        .findByStageCodeAndStatusOrderByCreatedAtAsc(
            normalizedStage, STATUS_PENDING, PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
        .stream()
        .map(execution -> markProcessingAndMap(execution, now))
        .toList();
  }

  /** Registra o callback de resultado ou falha enviado pelo scientific-research-worker. */
  @Transactional
  public void receiveCallback(
      String stageCode, Long executionId, ProductEvidenceStageCallbackRequest request) {
    MdsProductEvidenceStageExecution execution =
        executionRepository
            .findByIdAndStageCode(executionId, normalizeStageCode(stageCode))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "scientific stage execution not found"));
    if (StringUtils.hasText(request.errorType())) {
      markFailed(execution, request);
      return;
    }
    markFunctionalResult(execution, request);
  }

  /** Verifica se já existe execução científica aberta para o nicho. */
  private boolean hasActiveEvidenceExecution(Long marketNicheId) {
    return executionRepository
        .findTopByMarketNicheIdAndStatusInOrderByCreatedAtDesc(marketNicheId, ACTIVE_STATUSES)
        .isPresent();
  }

  /** Marca uma execução como em processamento e monta o contrato do worker. */
  private ProductEvidenceStagePendingResponse markProcessingAndMap(
      MdsProductEvidenceStageExecution execution, Instant now) {
    execution.setStatus(STATUS_PROCESSING);
    execution.setProcessingStartedAt(now);
    return new ProductEvidenceStagePendingResponse(
        execution.getJobId(),
        String.valueOf(execution.getId()),
        "market-niche-" + execution.getMarketNicheId(),
        execution.getProductIdea(),
        execution.getScientificQuestion(),
        readMap(execution.getInputPayload()),
        "/api/internal/scientific-research/product-evidence/v1/"
            + execution.getStageCode()
            + "/stage-executions/"
            + execution.getId()
            + "/callback");
  }

  /** Marca a execução como falha técnica reportada pelo worker. */
  private void markFailed(
      MdsProductEvidenceStageExecution execution, ProductEvidenceStageCallbackRequest request) {
    execution.setStatus(STATUS_FAILED);
    execution.setErrorType(request.errorType());
    execution.setErrorMessage(request.errorMessage());
    execution.setCompletedAt(Instant.now());
  }

  /** Persiste o resultado funcional e enfileira a próxima etapa quando aplicável. */
  private void markFunctionalResult(
      MdsProductEvidenceStageExecution execution, ProductEvidenceStageCallbackRequest request) {
    execution.setOutputPayload(toJson(request.output() == null ? Map.of() : request.output()));
    execution.setArtifactsPayload(
        toJson(request.artifacts() == null ? List.of() : request.artifacts()));
    execution.setRootCause(request.rootCause());
    execution.setCommercialImpact(request.commercialImpact());
    execution.setRecommendedAction(request.recommendedAction());
    execution.setCompletedAt(Instant.now());
    String status = normalizeResultStatus(request.status());
    execution.setStatus(status);
    if (STATUS_COMPLETED.equals(status) && StringUtils.hasText(request.nextStageCode())) {
      createNextExecution(execution, request);
    }
  }

  /** Cria a próxima etapa científica usando a saída funcional da etapa atual como entrada. */
  private void createNextExecution(
      MdsProductEvidenceStageExecution previous, ProductEvidenceStageCallbackRequest request) {
    String nextStageCode = normalizeStageCode(request.nextStageCode());
    if (hasStageAlreadyStarted(previous.getMarketNicheId(), nextStageCode)) {
      return;
    }
    executionRepository.save(
        newExecution(
            previous.getMarketNicheId(),
            nextStageCode,
            previous.getJobId(),
            previous.getProductIdea(),
            previous.getScientificQuestion(),
            request.output() == null ? Map.of() : request.output()));
  }

  /** Verifica se a próxima etapa já foi criada para manter callback idempotente. */
  private boolean hasStageAlreadyStarted(Long marketNicheId, String stageCode) {
    return executionRepository
        .findTopByMarketNicheIdAndStageCodeAndStatusInOrderByCreatedAtDesc(
            marketNicheId, stageCode, ALREADY_STARTED_STATUSES)
        .isPresent();
  }

  /** Monta uma nova entidade de etapa científica pronta para fila. */
  private MdsProductEvidenceStageExecution newExecution(
      Long marketNicheId,
      String stageCode,
      String jobId,
      String productIdea,
      String scientificQuestion,
      Map<String, Object> input) {
    MdsProductEvidenceStageExecution execution = new MdsProductEvidenceStageExecution();
    execution.setMarketNicheId(marketNicheId);
    execution.setStageCode(stageCode);
    execution.setJobId(jobId);
    execution.setStatus(STATUS_PENDING);
    execution.setProductIdea(productIdea);
    execution.setScientificQuestion(scientificQuestion);
    execution.setInputPayload(toJson(input));
    execution.setCreatedAt(Instant.now());
    return execution;
  }

  /** Monta a entrada inicial da pesquisa científica a partir das etapas concluídas da hipótese. */
  private Map<String, Object> buildInitialInput(Long marketNicheId, MarketNiche niche) {
    Optional<Hypothesis> latestHypothesis = latestHypothesis(marketNicheId);
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("marketNicheId", marketNicheId);
    input.put("marketName", niche.getName());
    input.put(
        "hypothesisId", latestHypothesis.map(Hypothesis::getId).map(String::valueOf).orElse(""));
    input.put("hypothesisTitle", latestHypothesis.map(Hypothesis::getTitle).orElse(""));
    input.put("persona", latestHypothesis.map(Hypothesis::getPersona).orElse(""));
    input.put(
        "pain",
        firstUseful(
            latestStageText(marketNicheId, HYPOTHESIS_PAIN),
            latestHypothesis.map(Hypothesis::getProblem).orElse("")));
    input.put(
        "result",
        firstUseful(
            latestStageText(marketNicheId, HYPOTHESIS_RESULT),
            latestHypothesis.map(Hypothesis::getPromise).orElse("")));
    input.put(
        "mechanism",
        firstUseful(
            latestStageText(marketNicheId, HYPOTHESIS_MECHANISM),
            latestHypothesis.map(this::resolveMechanism).orElse("")));
    input.put(
        "proof",
        firstUseful(
            latestStageText(marketNicheId, HYPOTHESIS_PROOF),
            latestHypothesis.map(Hypothesis::getEntrega).orElse("")));
    return input;
  }

  /**
   * Busca a hipótese manual/sistêmica mais recente do nicho para preencher pesquisa sem pipeline
   * completo.
   */
  private Optional<Hypothesis> latestHypothesis(Long marketNicheId) {
    return hypothesisRepository.findByMarketNicheId(marketNicheId).stream()
        .max(
            Comparator.comparing(
                Hypothesis::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
  }

  /** Resolve o mecanismo mais específico disponível na hipótese. */
  private String resolveMechanism(Hypothesis hypothesis) {
    return firstUseful(hypothesis.getUniqueMechanism(), hypothesis.getMechanism());
  }

  /** Retorna o primeiro texto preenchido para compor o contrato científico. */
  private String firstUseful(String first, String second) {
    if (StringUtils.hasText(first)) {
      return first;
    }
    return StringUtils.hasText(second) ? second : "";
  }

  /** Retorna a resposta concluída mais recente de uma etapa da hipótese. */
  private String latestStageText(Long marketNicheId, String stageCode) {
    return hypothesisExecutionRepository
        .findTopByMarketNicheIdAndStageCodeAndStatusAndHypothesisIdIsNullOrderByExecutionRequestedAtDesc(
            marketNicheId, stageCode, "CONCLUIDO")
        .map(HypothesisPainStageExecution::getModelResponse)
        .filter(StringUtils::hasText)
        .orElse("");
  }

  /** Deriva a ideia de produto a partir dos blocos do framework comercial. */
  private String productIdea(Map<String, Object> input) {
    return "Produto digital para "
        + input.getOrDefault("marketName", "nicho")
        + " que entrega "
        + input.getOrDefault("result", "")
        + " por meio de "
        + input.getOrDefault("mechanism", "");
  }

  /** Deriva a pergunta científica que orienta a busca de artigos. */
  private String scientificQuestion(Map<String, Object> input) {
    return "Quais evidências científicas sustentam ou limitam o mecanismo: "
        + input.getOrDefault("mechanism", "")
        + " para resolver a dor: "
        + input.getOrDefault("pain", "")
        + "?";
  }

  /** Normaliza códigos de etapa recebidos por enum Java ou por slug externo. */
  private String normalizeStageCode(String stageCode) {
    if (!StringUtils.hasText(stageCode)) {
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "stageCode is required");
    }
    String normalized = stageCode.trim();
    return switch (normalized.toUpperCase(Locale.ROOT)) {
      case "SOURCE_DISCOVERY" -> SOURCE_DISCOVERY;
      case "EVIDENCE_SYNTHESIS" -> EVIDENCE_SYNTHESIS;
      case "DELIVERABLE_COMPOSER" -> DELIVERABLE_COMPOSER;
      default -> normalized.toLowerCase(Locale.ROOT);
    };
  }

  /** Normaliza o status funcional retornado pelo worker. */
  private String normalizeResultStatus(String status) {
    if ("BLOCKED".equalsIgnoreCase(status) || "BLOQUEADO".equalsIgnoreCase(status)) {
      return STATUS_BLOCKED;
    }
    if ("COMPLETED".equalsIgnoreCase(status) || "CONCLUIDO".equalsIgnoreCase(status)) {
      return STATUS_COMPLETED;
    }
    return STATUS_FAILED;
  }

  /** Serializa payload estruturado para persistência auditável. */
  private String toJson(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "invalid product evidence payload", ex);
    }
  }

  /** Lê um objeto JSON persistido como mapa funcional. */
  private Map<String, Object> readMap(String json) {
    if (!StringUtils.hasText(json)) {
      return Map.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "invalid product evidence input", ex);
    }
  }
}
