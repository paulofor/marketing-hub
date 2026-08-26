package com.marketinghub.businessprocess.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.document.service.recentDocuments.BusinessProcessActivityDocumentResponse;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: consultar documentos auditáveis produzidos por cada atividade BPM. */
@Service
public class BusinessProcessActivityDocumentService {
  private static final int RECENT_DOCUMENT_LIMIT = 10;
  private static final Pattern COMMERCIAL_PLAN_REFERENCE =
      Pattern.compile("^commercial-plan:(\\d+)@.*$");
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BusinessProcessActivityDocumentService.class);

  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskRepository taskRepository;
  private final CommercialPlanRepository commercialPlanRepository;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas do processo, das tarefas e do diagrama. */
  @Autowired
  public BusinessProcessActivityDocumentService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      CommercialPlanRepository commercialPlanRepository,
      ObjectMapper objectMapper) {
    this.processRepository = processRepository;
    this.taskRepository = taskRepository;
    this.commercialPlanRepository = commercialPlanRepository;
    this.objectMapper = objectMapper;
  }

  /** Mantém os testes unitários focados na leitura documental sem carregar o catálogo comercial. */
  BusinessProcessActivityDocumentService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this(processRepository, taskRepository, null, objectMapper);
  }

  /** Lista as atividades que já possuem documento concluído para ativar os links no BPM. */
  @Transactional(readOnly = true)
  public List<String> documentActivityIds(Long processDefinitionId) {
    requiredProcess(processDefinitionId);
    return taskRepository.findDocumentActivityIds(processDefinitionId);
  }

  /** Retorna no máximo os dez documentos mais recentes da atividade informada. */
  @Transactional(readOnly = true)
  public List<BusinessProcessActivityDocumentResponse> recentDocuments(
      Long processDefinitionId, String activityId) {
    BusinessProcessDefinition process = requiredProcess(processDefinitionId);
    String normalizedActivityId = requireTaskActivity(process, activityId);
    return taskRepository
        .findRecentActivityDocuments(
            processDefinitionId, normalizedActivityId, PageRequest.of(0, RECENT_DOCUMENT_LIMIT))
        .stream()
        .limit(RECENT_DOCUMENT_LIMIT)
        .map(this::response)
        .toList();
  }

  /** Retorna no máximo os dez documentos mais recentes produzidos pelo processo inteiro. */
  @Transactional(readOnly = true)
  public List<BusinessProcessActivityDocumentResponse> recentProcessDocuments(
      Long processDefinitionId) {
    requiredProcess(processDefinitionId);
    return taskRepository
        .findRecentProcessDocuments(processDefinitionId, PageRequest.of(0, RECENT_DOCUMENT_LIMIT))
        .stream()
        .limit(RECENT_DOCUMENT_LIMIT)
        .map(this::response)
        .toList();
  }

  /** Exige uma definição existente antes de consultar seu histórico operacional. */
  private BusinessProcessDefinition requiredProcess(Long processDefinitionId) {
    return processRepository
        .findById(processDefinitionId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processo não encontrado."));
  }

  /** Confirma que o identificador pertence a uma atividade TASK da própria definição. */
  private String requireTaskActivity(BusinessProcessDefinition process, String activityId) {
    String normalized = activityId == null ? "" : activityId.trim();
    if (normalized.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Atividade não informada.");
    }
    try {
      JsonNode nodes = objectMapper.readTree(process.getDiagramJson()).path("nodes");
      boolean taskExists =
          nodes.isArray()
              && java.util.stream.StreamSupport.stream(nodes.spliterator(), false)
                  .anyMatch(
                      node ->
                          normalized.equals(node.path("id").asText())
                              && "TASK".equals(node.path("type").asText()));
      if (!taskExists) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Atividade não encontrada neste processo.");
      }
      return normalized;
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(
          "Falha ao ler diagrama para consultar documentos BPM. processDefinitionId={} activityId={}",
          process.getId(),
          normalized,
          ex);
      throw new IllegalStateException("Não foi possível ler o diagrama do processo.", ex);
    }
  }

  /** Converte a tarefa concluída em documento sem perder origem, custo ou evidências. */
  private BusinessProcessActivityDocumentResponse response(AgentTask task) {
    return new BusinessProcessActivityDocumentResponse(
        task.getId(),
        task.getTitle(),
        task.getSourceReference(),
        task.getAssignedAgent().getAgentKey(),
        task.getAssignedAgent().getNickname(),
        task.getResultJson(),
        task.getEvidenceJson(),
        task.getInputTokens(),
        task.getCachedInputTokens(),
        task.getOutputTokens(),
        task.getEstimatedCostUsd(),
        task.getCostEstimationStatus(),
        task.getReceivedAt(),
        task.getDeliveredAt() == null ? task.getUpdatedAt() : task.getDeliveredAt(),
        executionModelCode(task),
        executionReasoningEffort(task),
        productInternalName(task),
        task.getExecutionPrompt());
  }

  /** Recupera a identidade de modelo já registrada em evidências legadas, sem inventar um valor. */
  private String executionModelCode(AgentTask task) {
    return firstPresent(task.getExecutionModelCode(), evidenceText(task, "modelCode", "model"));
  }

  /** Recupera o esforço já registrado em evidências legadas, sem inferi-lo pelo modelo. */
  private String executionReasoningEffort(AgentTask task) {
    return firstPresent(
        task.getExecutionReasoningEffort(),
        evidenceText(task, "reasoningEffort", "modelReasoningEffort"));
  }

  /** Lê somente atributos textuais conhecidos do JSON de evidência já persistido. */
  private String evidenceText(AgentTask task, String... fieldNames) {
    if (task.getEvidenceJson() == null || task.getEvidenceJson().isBlank()) return null;
    try {
      JsonNode evidence = objectMapper.readTree(task.getEvidenceJson());
      for (String fieldName : fieldNames) {
        JsonNode value = evidence.path(fieldName);
        if (value.isTextual() && !value.asText().isBlank()) return value.asText();
      }
    } catch (Exception ex) {
      LOGGER.debug(
          "Evidência legada não contém JSON para auditoria da tarefa {}.", task.getId(), ex);
    }
    return null;
  }

  /** Devolve o primeiro texto preenchido, mantendo ausente o dado que não foi registrado. */
  private String firstPresent(String first, String second) {
    if (first != null && !first.isBlank()) return first;
    return second != null && !second.isBlank() ? second : null;
  }

  /** Resolve o nome interno somente quando a referência possui vínculo comercial canônico. */
  private String productInternalName(AgentTask task) {
    if (commercialPlanRepository == null || task.getSourceReference() == null) return null;
    Matcher matcher = COMMERCIAL_PLAN_REFERENCE.matcher(task.getSourceReference());
    if (!matcher.matches()) return null;
    try {
      return commercialPlanRepository
          .findById(Long.valueOf(matcher.group(1)))
          .flatMap(this::internalProductName)
          .orElse(null);
    } catch (NumberFormatException ex) {
      LOGGER.warn(
          "Referência comercial inválida ao consultar produto da tarefa {}.", task.getId(), ex);
      return null;
    }
  }

  /** Lê a identidade interna pelo vínculo direto do plano, sem deduzir produto por texto livre. */
  private Optional<String> internalProductName(CommercialPlan plan) {
    if (plan.getHypothesis() != null && plan.getHypothesis().getProduct() != null) {
      return Optional.ofNullable(plan.getHypothesis().getProduct().getInternalName());
    }
    if (plan.getExperiment() != null && plan.getExperiment().getProduct() != null) {
      return Optional.ofNullable(plan.getExperiment().getProduct().getInternalName());
    }
    return plan.getExperiments().stream()
        .map(experiment -> experiment.getProduct())
        .filter(java.util.Objects::nonNull)
        .map(product -> product.getInternalName())
        .filter(value -> value != null && !value.isBlank())
        .findFirst();
  }
}
