package com.marketinghub.businessprocess.independent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.independent.IndependentBusinessProcessExecution;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessCatalogResponse;
import com.marketinghub.businessprocess.independent.service.catalog.IndependentBusinessProcessInputFieldResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessActivityResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessExecutionSummaryResponse;
import com.marketinghub.businessprocess.independent.service.executions.IndependentBusinessProcessTaskResponse;
import com.marketinghub.businessprocess.independent.service.startExecution.StartIndependentBusinessProcessExecutionRequest;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.IndependentBusinessProcessExecutionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: governar o início e a leitura de processos que não dependem de produto. */
@Service
public class IndependentBusinessProcessExecutionService {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(IndependentBusinessProcessExecutionService.class);
  private static final List<String> INDEPENDENT_SCOPES =
      List.of("INDEPENDENT", "PRODUCT_OR_INDEPENDENT");
  private final IndependentBusinessProcessExecutionRepository executionRepository;
  private final BusinessProcessDefinitionRepository processRepository;
  private final BusinessProcessActivityDefinitionRepository activityRepository;
  private final AgentTaskRepository taskRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Map<String, IndependentBusinessProcessExecutionHandler> handlers;
  private final Map<String, IndependentBusinessProcessExecutionReportProvider> reportProviders;

  /** Configura persistência, adaptadores e consolidação auditável das execuções. */
  @Autowired
  public IndependentBusinessProcessExecutionService(
      IndependentBusinessProcessExecutionRepository executionRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper,
      List<IndependentBusinessProcessExecutionHandler> handlers,
      List<IndependentBusinessProcessExecutionReportProvider> reportProviders) {
    this(
        executionRepository,
        processRepository,
        activityRepository,
        taskRepository,
        objectMapper,
        handlers,
        reportProviders,
        Clock.systemUTC());
  }

  /** Permite testes determinísticos sem alterar o relógio operacional da aplicação. */
  IndependentBusinessProcessExecutionService(
      IndependentBusinessProcessExecutionRepository executionRepository,
      BusinessProcessDefinitionRepository processRepository,
      BusinessProcessActivityDefinitionRepository activityRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper,
      List<IndependentBusinessProcessExecutionHandler> handlers,
      List<IndependentBusinessProcessExecutionReportProvider> reportProviders,
      Clock clock) {
    this.executionRepository = executionRepository;
    this.processRepository = processRepository;
    this.activityRepository = activityRepository;
    this.taskRepository = taskRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.handlers =
        handlers.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    IndependentBusinessProcessExecutionHandler::processCode, Function.identity()));
    this.reportProviders =
        reportProviders.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    IndependentBusinessProcessExecutionReportProvider::processCode,
                    Function.identity()));
  }

  /** Lista o catálogo publicado e informa quando ainda falta um adaptador operacional. */
  @Transactional(readOnly = true)
  public List<IndependentBusinessProcessCatalogResponse> catalog() {
    return processRepository
        .findAllByStatusAndExecutionScopeInOrderByNameAscVersionNumberDesc(
            "PUBLISHED", INDEPENDENT_SCOPES)
        .stream()
        .map(this::catalogResponse)
        .toList();
  }

  /** Lista as cinquenta solicitações materializadas mais recentes com estado consolidado. */
  @Transactional(readOnly = true)
  public List<IndependentBusinessProcessExecutionSummaryResponse> list() {
    return executionRepository
        .findTop50BySourceReferenceIsNotNullOrderByCreatedAtDescIdDesc()
        .stream()
        .map(this::summary)
        .toList();
  }

  /** Exibe entradas, atividades, tentativas, falhas e consumo de uma execução específica. */
  @Transactional(readOnly = true)
  public IndependentBusinessProcessExecutionResponse get(Long executionId) {
    IndependentBusinessProcessExecution execution = requiredExecution(executionId);
    return detail(execution);
  }

  /** Inicia uma execução idempotente por seu adaptador canônico e confirma a tarefa BPM criada. */
  @Transactional
  public IndependentBusinessProcessExecutionResponse start(
      StartIndependentBusinessProcessExecutionRequest request) {
    String requestKey = request.requestKey().toString();
    var existing = executionRepository.findByRequestKey(requestKey);
    if (existing.isPresent()) {
      if (!Objects.equals(
          existing.get().getProcessDefinition().getId(), request.processDefinitionId())) {
        throw conflict("A chave idempotente já pertence a outro processo.");
      }
      return detail(existing.get());
    }

    BusinessProcessDefinition process = requiredRunnableProcess(request.processDefinitionId());
    IndependentBusinessProcessExecutionHandler handler =
        handler(process)
            .orElseThrow(
                () -> conflict("Este processo ainda não possui adaptador de execução disponível."));
    ObjectNode input = normalizeInput(request.input(), handler.inputFields());

    IndependentBusinessProcessExecution execution = new IndependentBusinessProcessExecution();
    execution.setRequestKey(requestKey);
    execution.setProcessDefinition(process);
    execution.setDisplayName(process.getName());
    execution.setRequestedByName(request.requestedByName().trim());
    execution.setInputJson(write(input));
    execution.setCreatedAt(Instant.now(clock));
    executionRepository.saveAndFlush(execution);

    IndependentBusinessProcessStartedExecution started = handler.start(input);
    execution.setSourceReference(requiredStartedText(started.sourceReference(), "referência"));
    execution.setDisplayName(limitDisplayName(started.displayName(), process.getName()));
    IndependentBusinessProcessExecution saved = executionRepository.save(execution);
    if (taskRepository
        .findBySourceReferenceOrderByCreatedAtAscIdAsc(saved.getSourceReference())
        .isEmpty()) {
      throw new IllegalStateException(
          "O adaptador criou a entidade técnica sem registrar a tarefa BPM correspondente.");
    }
    return detail(saved);
  }

  /** Converte a definição publicada no contrato dinâmico da tela. */
  private IndependentBusinessProcessCatalogResponse catalogResponse(
      BusinessProcessDefinition process) {
    var handler = handler(process);
    return new IndependentBusinessProcessCatalogResponse(
        process.getId(),
        process.getProcessCode(),
        process.getName(),
        process.getPurpose(),
        process.getOwnerName(),
        process.getTriggerDescription(),
        process.getOutcomeDescription(),
        process.getVersionNumber(),
        handler.isPresent(),
        handler.isPresent()
            ? "Pronto para iniciar sem produto."
            : "Adaptador de execução ainda não implementado para este processo.",
        handler.map(IndependentBusinessProcessExecutionHandler::inputFields).orElseGet(List::of));
  }

  /** Localiza o adaptador pela identidade estável do processo, sem acoplamento no controller. */
  private java.util.Optional<IndependentBusinessProcessExecutionHandler> handler(
      BusinessProcessDefinition process) {
    return java.util.Optional.ofNullable(handlers.get(process.getProcessCode()));
  }

  /** Exige publicação e escopo independente antes de criar qualquer entidade operacional. */
  private BusinessProcessDefinition requiredRunnableProcess(Long processDefinitionId) {
    BusinessProcessDefinition process =
        processRepository
            .findById(processDefinitionId)
            .orElseThrow(() -> notFound("Processo não encontrado."));
    if (!"PUBLISHED".equals(process.getStatus())) {
      throw conflict("Somente uma versão publicada pode ser executada.");
    }
    String scope = process.getExecutionScope() == null ? "PRODUCT" : process.getExecutionScope();
    if (!INDEPENDENT_SCOPES.contains(scope)) {
      throw conflict("Este processo exige vínculo com produto.");
    }
    return process;
  }

  /** Valida tipos, campos, obrigatoriedade e limites antes de chamar o adaptador específico. */
  private ObjectNode normalizeInput(
      JsonNode rawInput, List<IndependentBusinessProcessInputFieldResponse> fields) {
    if (!rawInput.isObject()) {
      throw invalid("A entrada do processo deve ser um objeto JSON.");
    }
    Set<String> knownFields =
        fields.stream()
            .map(IndependentBusinessProcessInputFieldResponse::key)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    rawInput
        .fieldNames()
        .forEachRemaining(
            key -> {
              if (!knownFields.contains(key)) {
                throw invalid("Campo de entrada não reconhecido: " + key + ".");
              }
            });

    ObjectNode normalized = objectMapper.createObjectNode();
    for (IndependentBusinessProcessInputFieldResponse field : fields) {
      JsonNode supplied = rawInput.get(field.key());
      String value = null;
      if (supplied != null && !supplied.isNull()) {
        if (!supplied.isTextual()) {
          throw invalid("O campo " + field.label() + " deve ser textual.");
        }
        value = supplied.asText().trim();
      }
      if ((value == null || value.isBlank()) && field.defaultValue() != null) {
        value = field.defaultValue();
      }
      if (field.required() && (value == null || value.isBlank())) {
        throw invalid("O campo " + field.label() + " é obrigatório.");
      }
      if (value != null && field.maxLength() != null && value.length() > field.maxLength()) {
        throw invalid(
            "O campo " + field.label() + " aceita até " + field.maxLength() + " caracteres.");
      }
      if (value != null && field.options() != null && !field.options().isEmpty()) {
        boolean validOption = false;
        for (var option : field.options()) {
          if (option.value().equals(value)) {
            validOption = true;
            break;
          }
        }
        if (!validOption) {
          throw invalid("O campo " + field.label() + " possui uma opção inválida.");
        }
      }
      if (value != null && !value.isBlank()) normalized.put(field.key(), value);
    }
    return normalized;
  }

  /** Monta o detalhe a partir das tarefas persistidas na mesma referência operacional. */
  private IndependentBusinessProcessExecutionResponse detail(
      IndependentBusinessProcessExecution execution) {
    List<AgentTask> tasks = tasks(execution);
    List<IndependentBusinessProcessActivityResponse> activities = activities(execution, tasks);
    return new IndependentBusinessProcessExecutionResponse(
        summary(execution, tasks, activities), activities, processReport(execution));
  }

  /** Resolve o relatório especializado sem exigir heurísticas no frontend. */
  private com.marketinghub.businessprocess.independent.service.executions
          .IndependentBusinessProcessFlowReportResponse
      processReport(IndependentBusinessProcessExecution execution) {
    IndependentBusinessProcessExecutionReportProvider provider =
        reportProviders.get(execution.getProcessDefinition().getProcessCode());
    return provider == null ? null : provider.report(execution.getSourceReference());
  }

  /** Consolida o resumo quando a listagem não precisa devolver cada tentativa. */
  private IndependentBusinessProcessExecutionSummaryResponse summary(
      IndependentBusinessProcessExecution execution) {
    List<AgentTask> tasks = tasks(execution);
    List<IndependentBusinessProcessActivityResponse> activities = activities(execution, tasks);
    return summary(execution, tasks, activities);
  }

  /** Consolida progresso, consumo e estado terminal sem atribuir métricas comerciais. */
  private IndependentBusinessProcessExecutionSummaryResponse summary(
      IndependentBusinessProcessExecution execution,
      List<AgentTask> tasks,
      List<IndependentBusinessProcessActivityResponse> activities) {
    String status = businessStatus(execution, aggregateExecutionStatus(activities));
    int completed =
        (int) activities.stream().filter(item -> "COMPLETED".equals(item.status())).count();
    BigDecimal cost =
        tasks.stream()
            .map(AgentTask::getEstimatedCostUsd)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO.setScale(8), BigDecimal::add);
    boolean hasCost = tasks.stream().anyMatch(task -> task.getEstimatedCostUsd() != null);
    boolean completeCost =
        !tasks.isEmpty()
            && tasks.stream()
                .allMatch(
                    task ->
                        "ESTIMATED".equals(task.getCostEstimationStatus())
                            || "NOT_APPLICABLE".equals(task.getCostEstimationStatus()));
    return new IndependentBusinessProcessExecutionSummaryResponse(
        execution.getId(),
        UUID.fromString(execution.getRequestKey()),
        execution.getProcessDefinition().getId(),
        execution.getProcessDefinition().getProcessCode(),
        execution.getProcessDefinition().getName(),
        execution.getProcessDefinition().getVersionNumber(),
        execution.getSourceReference(),
        execution.getDisplayName(),
        execution.getRequestedByName(),
        read(execution.getInputJson(), "entrada", execution.getId()),
        status,
        activities.size(),
        completed,
        sumLong(tasks, AgentTask::getInputTokens),
        sumLong(tasks, AgentTask::getCachedInputTokens),
        sumLong(tasks, AgentTask::getOutputTokens),
        hasCost ? cost.setScale(8) : null,
        completeCost ? "COMPLETE" : hasCost ? "PARTIAL" : "NOT_REPORTED",
        latestError(tasks),
        execution.getCreatedAt(),
        tasks.stream()
            .map(AgentTask::getReceivedAt)
            .filter(Objects::nonNull)
            .min(Instant::compareTo)
            .orElse(null),
        isTerminal(status)
            ? tasks.stream()
                .map(AgentTask::getUpdatedAt)
                .filter(Objects::nonNull)
                .max(Instant::compareTo)
                .orElse(null)
            : null);
  }

  /** Ordena as tentativas reais e preserva atividades ainda não iniciadas no relatório. */
  private List<IndependentBusinessProcessActivityResponse> activities(
      IndependentBusinessProcessExecution execution, List<AgentTask> tasks) {
    Map<String, List<AgentTask>> byActivity = new LinkedHashMap<>();
    tasks.forEach(
        task ->
            byActivity.computeIfAbsent(activityKey(task), ignored -> new ArrayList<>()).add(task));
    List<IndependentBusinessProcessActivityResponse> response = new ArrayList<>();
    List<BusinessProcessActivityDefinition> definitions =
        activityRepository.findAllByProcessDefinitionIdOrderByIdAsc(
            execution.getProcessDefinition().getId());
    for (BusinessProcessActivityDefinition definition : definitions) {
      List<AgentTask> attempts = byActivity.remove(definition.getActivityId());
      if (attempts == null) attempts = List.of();
      response.add(
          new IndependentBusinessProcessActivityResponse(
              definition.getActivityId(),
              definition.getName(),
              aggregateTaskStatus(attempts),
              attempts.stream().map(this::taskResponse).toList()));
    }
    byActivity.forEach(
        (activityId, attempts) ->
            response.add(
                new IndependentBusinessProcessActivityResponse(
                    activityId,
                    attempts.get(attempts.size() - 1).getProcessActivityName(),
                    aggregateTaskStatus(attempts),
                    attempts.stream().map(this::taskResponse).toList())));
    return List.copyOf(response);
  }

  /** Converte uma tentativa persistida sem esconder resultado, evidência ou falha técnica. */
  private IndependentBusinessProcessTaskResponse taskResponse(AgentTask task) {
    return new IndependentBusinessProcessTaskResponse(
        task.getId(),
        task.getProcessDefinition().getId(),
        task.getProcessDefinition().getVersionNumber(),
        task.getSourceReference(),
        task.getStatus(),
        task.getAssignedAgent().getAgentKey(),
        task.getAssignedAgent().getNickname(),
        task.getTitle(),
        readOptional(task.getResultJson(), "resultado", task.getId()),
        readOptional(task.getEvidenceJson(), "evidência", task.getId()),
        task.getExecutionError(),
        task.getInputTokens(),
        task.getCachedInputTokens(),
        task.getOutputTokens(),
        task.getEstimatedCostUsd(),
        task.getCostEstimationStatus(),
        task.getExecutionModelCode(),
        task.getExecutionMode(),
        task.getExecutionReasoningEffort(),
        null,
        task.getExecutionPrompt(),
        task.getExecutionAgentPrompt(),
        task.getExecutionActivityPrompt(),
        task.getCreatedAt(),
        task.getReceivedAt(),
        finishedAt(task));
  }

  /** Usa a entrega ou a atualização terminal sem fabricar término para tarefa ainda ativa. */
  private Instant finishedAt(AgentTask task) {
    if (task.getDeliveredAt() != null) return task.getDeliveredAt();
    return List.of("BLOCKED", "CANCELLED").contains(task.getStatus()) ? task.getUpdatedAt() : null;
  }

  /** Carrega somente tarefas pertencentes à referência materializada desta execução. */
  private List<AgentTask> tasks(IndependentBusinessProcessExecution execution) {
    if (execution.getSourceReference() == null) return List.of();
    return taskRepository.findBySourceReferenceOrderByCreatedAtAscIdAsc(
        execution.getSourceReference());
  }

  /** Usa a identidade da atividade e mantém fallback explícito para registros históricos. */
  private String activityKey(AgentTask task) {
    return task.getProcessActivityId() == null ? "unmapped" : task.getProcessActivityId();
  }

  /** Consolida as tentativas de uma atividade pela tentativa mais recente de cada agente. */
  private String aggregateTaskStatus(List<AgentTask> attempts) {
    if (attempts.isEmpty()) return "NOT_STARTED";
    Map<String, AgentTask> latestByAgent = new LinkedHashMap<>();
    attempts.forEach(task -> latestByAgent.put(task.getAssignedAgent().getAgentKey(), task));
    List<String> statuses = latestByAgent.values().stream().map(AgentTask::getStatus).toList();
    if (statuses.contains("IN_PROGRESS")) return "IN_PROGRESS";
    if (statuses.contains("BLOCKED")) return "BLOCKED";
    if (statuses.contains("PENDING")) return "PENDING";
    if (statuses.stream().allMatch("COMPLETED"::equals)) return "COMPLETED";
    if (statuses.stream().allMatch("CANCELLED"::equals)) return "CANCELLED";
    return "IN_PROGRESS";
  }

  /**
   * Consolida o processo sem permitir que uma atividade antiga concluída esconda bloqueio atual.
   */
  private String aggregateExecutionStatus(
      List<IndependentBusinessProcessActivityResponse> activities) {
    List<String> statuses =
        activities.stream().map(IndependentBusinessProcessActivityResponse::status).toList();
    if (statuses.contains("BLOCKED")) return "BLOCKED";
    if (statuses.contains("IN_PROGRESS")) return "IN_PROGRESS";
    if (statuses.contains("PENDING")) return "PENDING";
    if (!statuses.isEmpty() && statuses.stream().allMatch("COMPLETED"::equals)) {
      return "COMPLETED";
    }
    if (!statuses.isEmpty() && statuses.stream().allMatch("CANCELLED"::equals)) {
      return "CANCELLED";
    }
    if (statuses.contains("COMPLETED")) return "IN_PROGRESS";
    return "NOT_STARTED";
  }

  /** Soma um contador apenas quando ao menos uma tarefa informou essa métrica. */
  private Long sumLong(List<AgentTask> tasks, Function<AgentTask, Long> mapper) {
    List<Long> values = tasks.stream().map(mapper).filter(Objects::nonNull).toList();
    return values.isEmpty() ? null : values.stream().mapToLong(Long::longValue).sum();
  }

  /** Retorna a falha mais recente para orientar a ação sem depender de logs técnicos. */
  private String latestError(List<AgentTask> tasks) {
    return tasks.stream()
        .filter(task -> task.getExecutionError() != null && !task.getExecutionError().isBlank())
        .max(
            Comparator.comparing(
                    AgentTask::getUpdatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(AgentTask::getId))
        .map(AgentTask::getExecutionError)
        .orElse(null);
  }

  /** Reconhece os estados que encerram a tentativa atual da execução. */
  private boolean isTerminal(String status) {
    return Set.of("COMPLETED", "BLOCKED", "CANCELLED").contains(status);
  }

  /** Usa o estado funcional especializado quando o processo possuir uma cadeia posterior. */
  private String businessStatus(
      IndependentBusinessProcessExecution execution, String technicalStatus) {
    IndependentBusinessProcessExecutionReportProvider provider =
        reportProviders.get(execution.getProcessDefinition().getProcessCode());
    if (provider == null || execution.getSourceReference() == null) return technicalStatus;
    String reportStatus = provider.report(execution.getSourceReference()).status();
    return reportStatus == null ? technicalStatus : reportStatus;
  }

  /** Exige uma execução registrada para manter respostas 404 consistentes. */
  private IndependentBusinessProcessExecution requiredExecution(Long executionId) {
    return executionRepository
        .findById(executionId)
        .orElseThrow(() -> notFound("Execução independente não encontrada."));
  }

  /** Serializa a entrada normalizada e registra qualquer falha inesperada com contexto. */
  private String write(JsonNode value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      LOGGER.error("Falha ao serializar entrada de processo independente.", ex);
      throw new IllegalStateException("Não foi possível persistir a entrada da execução.", ex);
    }
  }

  /** Interpreta JSON obrigatório persistido e preserva contexto diante de corrupção histórica. */
  private JsonNode read(String value, String field, Long executionId) {
    try {
      return objectMapper.readTree(value);
    } catch (JsonProcessingException ex) {
      LOGGER.error(
          "Falha ao interpretar JSON de execução independente. executionId={} campo={}",
          executionId,
          field,
          ex);
      throw new IllegalStateException("Dados persistidos da execução estão inválidos.", ex);
    }
  }

  /**
   * Interpreta resultado opcional e mantém o payload bruto legível se for legado não estruturado.
   */
  private JsonNode readOptional(String value, String field, Long taskId) {
    if (value == null || value.isBlank()) return null;
    try {
      return objectMapper.readTree(value);
    } catch (JsonProcessingException ex) {
      LOGGER.warn(
          "Payload legado não estruturado em tarefa de processo independente. taskId={} campo={}",
          taskId,
          field,
          ex);
      return objectMapper.getNodeFactory().textNode(value);
    }
  }

  /** Valida que o adaptador devolveu uma referência persistível. */
  private String requiredStartedText(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("O adaptador não informou " + label + " da execução.");
    }
    return value.trim();
  }

  /** Limita o título operacional à capacidade da coluna sem perder o fallback do processo. */
  private String limitDisplayName(String value, String fallback) {
    String selected = value == null || value.isBlank() ? fallback : value.trim();
    return selected.length() <= 160 ? selected : selected.substring(0, 160);
  }

  /** Produz erro 400 para entrada que viola o contrato declarado pelo backend. */
  private ResponseStatusException invalid(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  /** Produz erro 404 para identidade inexistente. */
  private ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }

  /** Produz erro 409 quando o estado ou escopo não permite iniciar a execução. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
