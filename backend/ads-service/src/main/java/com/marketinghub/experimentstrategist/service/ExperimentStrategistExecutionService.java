package com.marketinghub.experimentstrategist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experimentstrategist.BehavioralSnapshotDimension;
import com.marketinghub.experimentstrategist.ExperimentStrategistBehavioralSnapshot;
import com.marketinghub.experimentstrategist.ExperimentStrategistBehavioralSnapshotStatus;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistBehavioralSnapshotRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: controlar a fila e os resultados do Estrategista sem executar recomendacoes.
 */
@Service
public class ExperimentStrategistExecutionService {
  private static final Logger log =
      LoggerFactory.getLogger(ExperimentStrategistExecutionService.class);
  private static final long STALE_EXECUTION_SECONDS = 120L;
  private static final String LEASE_RECOVERED = "LEASE_RECOVERED_ONCE";
  private static final String CLARITY_PROVIDER = "MICROSOFT_CLARITY_MCP";
  private static final long MAX_CLARITY_QUERIES_PER_EXECUTION = 3L;
  private static final long MAX_CLARITY_QUERIES_PER_UTC_DAY = 9L;
  private static final Pattern FORBIDDEN_BEHAVIORAL_FIELD =
      Pattern.compile(
          "session[_ -]?id|visitor[_ -]?id|user[_ -]?id|recording(?:url|link|id)?|individual[_ -]?timeline",
          Pattern.CASE_INSENSITIVE);
  private final ExperimentStrategistExecutionRepository repository;
  private final ExperimentStrategistBehavioralSnapshotRepository behavioralSnapshots;
  private final CommercialPlanService plans;
  private final ExperimentStrategistContextService contexts;
  private final ObjectMapper json;
  private final ApplicationEventPublisher events;

  /** Configura as fontes e a persistencia da execucao estrategica. */
  @Autowired
  public ExperimentStrategistExecutionService(
      ExperimentStrategistExecutionRepository repository,
      ExperimentStrategistBehavioralSnapshotRepository behavioralSnapshots,
      CommercialPlanService plans,
      ExperimentStrategistContextService contexts,
      ObjectMapper json,
      ApplicationEventPublisher events) {
    this.repository = repository;
    this.behavioralSnapshots = behavioralSnapshots;
    this.plans = plans;
    this.contexts = contexts;
    this.json = json;
    this.events = events;
  }

  /** Mantém a construção direta usada pelos testes legados sem barramento de eventos. */
  ExperimentStrategistExecutionService(
      ExperimentStrategistExecutionRepository repository,
      ExperimentStrategistBehavioralSnapshotRepository behavioralSnapshots,
      CommercialPlanService plans,
      ExperimentStrategistContextService contexts,
      ObjectMapper json) {
    this(repository, behavioralSnapshots, plans, contexts, json, event -> {});
  }

  /** Enfileira Atena para propor as premissas ausentes antes da validação de Plutus. */
  @Transactional
  public ExecutionResponse startCommercialAssumptions(Long planId) {
    CommercialPlan plan = plans.getPlan(planId);
    ExperimentStrategistExecution value = new ExperimentStrategistExecution();
    value.setCommercialPlan(plan);
    value.setStatus(ExperimentStrategistExecutionStatus.PENDING);
    value.setAuthorityMode("COMMERCIAL_ASSUMPTIONS_PROPOSAL");
    value.setResearchQuestion(
        "Definir, com evidências e faixas conservadoras, as premissas comerciais ausentes do plano "
            + plan.getName());
    value.setEvidenceSnapshot(serialize(contexts.researchContext(planId)));
    ExperimentStrategistExecution saved = repository.save(value);
    repository.attachCurrentAgentVersion(saved.getId());
    return response(saved);
  }

  /** Enfileira uma pesquisa e congela suas evidencias antes da execucao. */
  @Transactional
  public ExecutionResponse start(Long planId, StartRequest request) {
    if (request == null || blank(request.researchQuestion()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a pergunta de pesquisa.");
    CommercialPlan plan = plans.getPlan(planId);
    ExperimentStrategistExecution value = new ExperimentStrategistExecution();
    value.setCommercialPlan(plan);
    value.setStatus(ExperimentStrategistExecutionStatus.PENDING);
    value.setAuthorityMode("READ_ONLY_RESEARCH");
    value.setResearchQuestion(request.researchQuestion().trim());
    value.setEvidenceSnapshot(serialize(contexts.researchContext(planId)));
    ExperimentStrategistExecution saved = repository.save(value);
    repository.attachCurrentAgentVersion(saved.getId());
    return response(saved);
  }

  /** Lista o historico de pesquisas de um planejamento. */
  @Transactional(readOnly = true)
  public List<ExecutionResponse> list(Long planId) {
    plans.getPlan(planId);
    return repository.findByCommercialPlanIdOrderByCreatedAtDesc(planId).stream()
        .map(this::response)
        .toList();
  }

  /** Reserva uma unica pesquisa pendente para o worker. */
  @Transactional
  public ExecutionResponse claim() {
    recoverExpiredLeases();
    ExperimentStrategistExecution value =
        repository
            .findByStatusOrderByCreatedAtAsc(
                ExperimentStrategistExecutionStatus.PENDING, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    value.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    value.setStartedAt(Instant.now());
    value.setErrorMessage(null);
    return response(repository.save(value));
  }

  /** Retoma uma lease órfã uma vez e encerra reincidência para impedir loop infinito. */
  private void recoverExpiredLeases() {
    Instant cutoff = Instant.now().minusSeconds(STALE_EXECUTION_SECONDS);
    for (ExperimentStrategistExecution value :
        repository.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(
            ExperimentStrategistExecutionStatus.RUNNING, cutoff)) {
      if (repository.countRecentActiveTelemetry(value.getId(), cutoff) > 0) continue;
      if (LEASE_RECOVERED.equals(value.getErrorMessage())) {
        value.setStatus(ExperimentStrategistExecutionStatus.FAILED);
        value.setFinishedAt(Instant.now());
        value.setErrorMessage("Lease expirou novamente após a retomada automática.");
      } else {
        value.setStatus(ExperimentStrategistExecutionStatus.PENDING);
        value.setStartedAt(null);
        value.setErrorMessage(LEASE_RECOVERED);
      }
      repository.save(value);
    }
  }

  /** Entrega ao MCP as evidencias congeladas da pesquisa reservada. */
  @Transactional(readOnly = true)
  public ExecutionResponse getExecution(Long id) {
    return response(running(id));
  }

  /** Reserva uma consulta agregada do Clarity respeitando segregação e cotas. */
  @Transactional
  public BehavioralSnapshotResponse reserveBehavioralSnapshot(
      Long executionId, ReserveBehavioralSnapshotRequest request) {
    ExperimentStrategistExecution execution = running(executionId);
    if (request == null || request.experimentId() == null)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe o experimento do snapshot comportamental.");
    BehavioralSnapshotDimension dimension = parseDimension(request.dimension());
    int windowDays = request.windowDays() == null ? 0 : request.windowDays();
    if (windowDays < 1 || windowDays > 3)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A janela do Clarity deve ter de 1 a 3 dias.");
    Experiment experiment =
        experimentFromPlan(execution.getCommercialPlan(), request.experimentId());
    if (behavioralSnapshots.countByExecutionId(executionId) >= MAX_CLARITY_QUERIES_PER_EXECUTION)
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Limite de três snapshots por execução atingido.");
    Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
    if (behavioralSnapshots.countByProviderAndRequestedAtGreaterThanEqual(
            CLARITY_PROVIDER, dayStart)
        >= MAX_CLARITY_QUERIES_PER_UTC_DAY)
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Cota diária segura do Clarity atingida.");

    ExperimentStrategistBehavioralSnapshot snapshot = new ExperimentStrategistBehavioralSnapshot();
    snapshot.setExecution(execution);
    snapshot.setExperiment(experiment);
    snapshot.setProvider(CLARITY_PROVIDER);
    snapshot.setDimension(dimension);
    snapshot.setWindowDays(windowDays);
    snapshot.setQueryText(clarityQuery(experiment.getId(), dimension, windowDays));
    snapshot.setStatus(ExperimentStrategistBehavioralSnapshotStatus.RESERVED);
    snapshot.setEstimatedCostUsd(BigDecimal.ZERO);
    snapshot.setRequestedAt(Instant.now());
    return behavioralSnapshotResponse(behavioralSnapshots.save(snapshot));
  }

  /** Persiste a resposta bruta agregada devolvida pelo MCP oficial do Clarity. */
  @Transactional
  public BehavioralSnapshotResponse completeBehavioralSnapshot(
      Long executionId, Long snapshotId, CompleteBehavioralSnapshotRequest request) {
    running(executionId);
    ExperimentStrategistBehavioralSnapshot snapshot = reservedSnapshot(executionId, snapshotId);
    if (request == null || blank(request.rawResponse()))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Informe a resposta agregada do Clarity.");
    if (request.rawResponse().length() > 500_000)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Resposta agregada do Clarity excedeu o limite seguro.");
    if (FORBIDDEN_BEHAVIORAL_FIELD.matcher(request.rawResponse()).find())
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Resposta do Clarity contém campo individual proibido e foi bloqueada.");
    snapshot.setRawResponse(request.rawResponse());
    snapshot.setStatus(ExperimentStrategistBehavioralSnapshotStatus.COMPLETED);
    snapshot.setFinishedAt(Instant.now());
    return behavioralSnapshotResponse(behavioralSnapshots.save(snapshot));
  }

  /** Persiste a causa completa da falha de uma consulta agregada do Clarity. */
  @Transactional
  public BehavioralSnapshotResponse failBehavioralSnapshot(
      Long executionId, Long snapshotId, FailBehavioralSnapshotRequest request) {
    running(executionId);
    ExperimentStrategistBehavioralSnapshot snapshot = reservedSnapshot(executionId, snapshotId);
    snapshot.setErrorMessage(
        request == null || blank(request.errorMessage())
            ? "Falha sem detalhe informada pelo adaptador Clarity."
            : request.errorMessage());
    snapshot.setStatus(ExperimentStrategistBehavioralSnapshotStatus.FAILED);
    snapshot.setFinishedAt(Instant.now());
    return behavioralSnapshotResponse(behavioralSnapshots.save(snapshot));
  }

  /** Persiste o parecer estruturado sem aplicar a recomendacao. */
  @Transactional
  public ExecutionResponse complete(Long id, CompleteRequest request) {
    ExperimentStrategistExecution value = running(id);
    if (request == null
        || blank(request.alternativesJson())
        || blank(request.recommendationJson())
        || blank(request.publicSourcesJson())
        || blank(request.rawModelResponse()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parecer estrategico incompleto.");
    validateMarketStrategicContract(value, request);
    value.setAlternativesJson(request.alternativesJson());
    value.setRecommendationJson(request.recommendationJson());
    value.setPublicSourcesJson(request.publicSourcesJson());
    value.setRawModelResponse(request.rawModelResponse());
    value.setModelName(request.modelName());
    value.setEstimatedCost(request.estimatedCost());
    value.setStatus(ExperimentStrategistExecutionStatus.COMPLETED);
    value.setFinishedAt(Instant.now());
    ExperimentStrategistExecution saved = repository.save(value);
    if ("COMMERCIAL_ASSUMPTIONS_PROPOSAL".equals(saved.getAuthorityMode())) {
      events.publishEvent(
          new CommercialAssumptionsProposed(
              saved.getCommercialPlan().getId(), saved.getId(), saved.getRecommendationJson()));
    }
    return response(saved);
  }

  /** Impede concluir pesquisa v2 sem autoria, evidências e fronteira estratégica verificáveis. */
  private void validateMarketStrategicContract(
      ExperimentStrategistExecution execution, CompleteRequest request) {
    if (!"READ_ONLY_RESEARCH".equals(execution.getAuthorityMode())) return;
    try {
      var alternatives = json.readTree(request.alternativesJson());
      var sources = json.readTree(request.publicSourcesJson());
      var recommendation = json.readTree(request.recommendationJson());
      var contract = recommendation.path("marketStrategicContract");
      var evidenceClasses = new HashSet<String>();
      if (sources.isArray()) {
        sources.forEach(
            source -> {
              String evidenceClass = source.path("evidenceClass").asText();
              if (!evidenceClass.isBlank()) evidenceClasses.add(evidenceClass);
            });
      }
      if (!alternatives.isArray()
          || alternatives.size() != 3
          || !sources.isArray()
          || sources.size() < 2
          || evidenceClasses.size() < 2
          || !"MARKET_STRATEGY_V2".equals(contract.path("contractVersion").asText())
          || !List.of("READY_FOR_OPERATION", "INSUFFICIENT_EVIDENCE")
              .contains(contract.path("status").asText())
          || contract.path("evidenceReferences").size() < 2
          || !"ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH"
              .equals(contract.path("operatorBoundary").asText())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Contrato Estratégico de Mercado v2 inválido.");
      }
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao validar contrato estratégico concluído por Atena. executionId={} planId={}",
          execution.getId(),
          execution.getCommercialPlan().getId(),
          ex);
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Contrato Estratégico de Mercado deve ser JSON válido.", ex);
    }
  }

  /** Persiste a causa completa de uma falha tecnica. */
  @Transactional
  public ExecutionResponse fail(Long id, FailRequest request) {
    ExperimentStrategistExecution value = running(id);
    value.setStatus(ExperimentStrategistExecutionStatus.FAILED);
    value.setErrorMessage(
        request == null || blank(request.errorMessage())
            ? "Falha sem detalhe informada pelo worker."
            : request.errorMessage());
    value.setFinishedAt(Instant.now());
    return response(repository.save(value));
  }

  /** Exige uma execucao existente em andamento. */
  private ExperimentStrategistExecution running(Long id) {
    ExperimentStrategistExecution value =
        repository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (value.getStatus() != ExperimentStrategistExecutionStatus.RUNNING)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Execucao nao esta em andamento.");
    return value;
  }

  /** Busca um snapshot reservado e segregado pela execução ativa. */
  private ExperimentStrategistBehavioralSnapshot reservedSnapshot(
      Long executionId, Long snapshotId) {
    ExperimentStrategistBehavioralSnapshot snapshot =
        behavioralSnapshots
            .findByIdAndExecutionId(snapshotId, executionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (snapshot.getStatus() != ExperimentStrategistBehavioralSnapshotStatus.RESERVED)
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Snapshot comportamental já foi encerrado.");
    return snapshot;
  }

  /** Valida a dimensão agregada permitida no contrato. */
  private BehavioralSnapshotDimension parseDimension(String raw) {
    try {
      return BehavioralSnapshotDimension.valueOf(raw == null ? "" : raw.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Dimensão comportamental permitida: PAGE, SOURCE ou DEVICE.", ex);
    }
  }

  /** Confirma que o experimento solicitado pertence ao portfólio do plano da execução. */
  private Experiment experimentFromPlan(CommercialPlan plan, Long experimentId) {
    if (plan.getExperiment() != null && experimentId.equals(plan.getExperiment().getId()))
      return plan.getExperiment();
    return plan.getExperiments().stream()
        .filter(experiment -> experimentId.equals(experiment.getId()))
        .findFirst()
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Experimento não pertence ao planejamento desta execução."));
  }

  /** Monta a consulta imutável que impede retorno individual ou mistura entre experimentos. */
  private String clarityQuery(
      Long experimentId, BehavioralSnapshotDimension dimension, int windowDays) {
    String grouping =
        switch (dimension) {
          case PAGE -> "URL da página";
          case SOURCE -> "origem de tráfego";
          case DEVICE -> "tipo de dispositivo";
        };
    return "Nos últimos "
        + windowDays
        + " dias, retorne somente métricas agregadas para URLs que contenham /flows/exp-"
        + experimentId
        + "-. Agrupe apenas por "
        + grouping
        + ". Para PAGE, normalize a URL sem query string. Inclua, quando disponíveis, sessões, visualizações, profundidade de scroll, tempo de engajamento, rage clicks, dead clicks, quick backs, erros JavaScript e desempenho. Não retorne gravações, sessionId, visitorId, userId nem timelines individuais.";
  }

  /** Serializa o snapshot auditavel. */
  private String serialize(Map<String, Object> value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao congelar evidencias.", ex);
    }
  }

  /** Converte a entidade no contrato da API. */
  private ExecutionResponse response(ExperimentStrategistExecution value) {
    return new ExecutionResponse(
        value.getId(),
        value.getCommercialPlan().getId(),
        value.getStatus(),
        value.getAuthorityMode(),
        value.getResearchQuestion(),
        value.getEvidenceSnapshot(),
        value.getAlternativesJson(),
        value.getRecommendationJson(),
        value.getPublicSourcesJson(),
        value.getModelName(),
        value.getEstimatedCost(),
        value.getErrorMessage(),
        value.getStartedAt(),
        value.getFinishedAt(),
        value.getCreatedAt(),
        behavioralSnapshots.findByExecutionIdOrderByRequestedAtAsc(value.getId()).stream()
            .map(this::behavioralSnapshotResponse)
            .toList());
  }

  /** Converte a auditoria Clarity no contrato persistido para o frontend e o worker. */
  private BehavioralSnapshotResponse behavioralSnapshotResponse(
      ExperimentStrategistBehavioralSnapshot value) {
    return new BehavioralSnapshotResponse(
        value.getId(),
        value.getExperiment().getId(),
        value.getProvider(),
        value.getDimension(),
        value.getWindowDays(),
        value.getQueryText(),
        value.getStatus(),
        value.getRawResponse(),
        value.getErrorMessage(),
        value.getEstimatedCostUsd(),
        value.getRequestedAt(),
        value.getFinishedAt());
  }

  /** Verifica ausencia de texto obrigatorio. */
  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  /** Contrato de abertura da pesquisa. */
  public record StartRequest(String researchQuestion) {}

  /** Contrato de conclusao do worker. */
  public record CompleteRequest(
      String alternativesJson,
      String recommendationJson,
      String publicSourcesJson,
      String rawModelResponse,
      String modelName,
      BigDecimal estimatedCost) {}

  /** Contrato de falha do worker. */
  public record FailRequest(String errorMessage) {}

  /** Contrato de reserva de snapshot agregado. */
  public record ReserveBehavioralSnapshotRequest(
      Long experimentId, String dimension, Integer windowDays) {}

  /** Contrato de recebimento da resposta bruta agregada. */
  public record CompleteBehavioralSnapshotRequest(String rawResponse) {}

  /** Contrato de recebimento da falha do adaptador Clarity. */
  public record FailBehavioralSnapshotRequest(String errorMessage) {}

  /** Contrato de auditoria de uma consulta comportamental agregada. */
  public record BehavioralSnapshotResponse(
      Long id,
      Long experimentId,
      String provider,
      BehavioralSnapshotDimension dimension,
      Integer windowDays,
      String queryText,
      ExperimentStrategistBehavioralSnapshotStatus status,
      String rawResponse,
      String errorMessage,
      BigDecimal estimatedCostUsd,
      Instant requestedAt,
      Instant finishedAt) {}

  /** Evento que entrega a proposta auditável de Atena à validação financeira de Plutus. */
  public record CommercialAssumptionsProposed(
      Long commercialPlanId, Long strategistExecutionId, String recommendationJson) {}

  /** Contrato de visualizacao e consumo da execucao. */
  public record ExecutionResponse(
      Long id,
      Long commercialPlanId,
      ExperimentStrategistExecutionStatus status,
      String authorityMode,
      String researchQuestion,
      String evidenceSnapshot,
      String alternativesJson,
      String recommendationJson,
      String publicSourcesJson,
      String modelName,
      BigDecimal estimatedCost,
      String errorMessage,
      Instant startedAt,
      Instant finishedAt,
      Instant createdAt,
      List<BehavioralSnapshotResponse> behavioralSnapshots) {}
}
