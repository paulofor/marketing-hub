package com.marketinghub.businessprocess.document.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.document.service.recentDocuments.BusinessProcessActivityDocumentResponse;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: consultar documentos auditáveis produzidos por cada atividade BPM. */
@Service
public class BusinessProcessActivityDocumentService {
  private static final int RECENT_DOCUMENT_LIMIT = 10;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(BusinessProcessActivityDocumentService.class);

  private final BusinessProcessDefinitionRepository processRepository;
  private final AgentTaskRepository taskRepository;
  private final ObjectMapper objectMapper;

  /** Configura as fontes canônicas do processo, das tarefas e do diagrama. */
  public BusinessProcessActivityDocumentService(
      BusinessProcessDefinitionRepository processRepository,
      AgentTaskRepository taskRepository,
      ObjectMapper objectMapper) {
    this.processRepository = processRepository;
    this.taskRepository = taskRepository;
    this.objectMapper = objectMapper;
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
        task.getDeliveredAt() == null ? task.getUpdatedAt() : task.getDeliveredAt());
  }
}
