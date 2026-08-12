package com.marketinghub.experimentstrategist.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
  private static final long STALE_EXECUTION_SECONDS = 120L;
  private static final String LEASE_RECOVERED = "LEASE_RECOVERED_ONCE";
  private final ExperimentStrategistExecutionRepository repository;
  private final CommercialPlanService plans;
  private final ExperimentStrategistContextService contexts;
  private final ObjectMapper json;
  private final ApplicationEventPublisher events;

  /** Configura as fontes e a persistencia da execucao estrategica. */
  public ExperimentStrategistExecutionService(
      ExperimentStrategistExecutionRepository repository,
      CommercialPlanService plans,
      ExperimentStrategistContextService contexts,
      ObjectMapper json,
      ApplicationEventPublisher events) {
    this.repository = repository;
    this.plans = plans;
    this.contexts = contexts;
    this.json = json;
    this.events = events;
  }

  /** Mantém a construção direta usada pelos testes legados sem barramento de eventos. */
  ExperimentStrategistExecutionService(
      ExperimentStrategistExecutionRepository repository,
      CommercialPlanService plans,
      ExperimentStrategistContextService contexts,
      ObjectMapper json) {
    this(repository, plans, contexts, json, event -> {});
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
        value.getCreatedAt());
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
      Instant createdAt) {}
}
