package com.marketinghub.agenttask;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: materializar no BPM atividades automáticas comprovadas por execuções técnicas.
 */
@Service
public class AutomaticBusinessProcessActivityService {
  private static final Logger log =
      LoggerFactory.getLogger(AutomaticBusinessProcessActivityService.class);
  private static final String COMPLETED = "COMPLETED";
  private final AgentTaskRepository taskRepository;
  private final BusinessProcessActivityDefinitionRepository activityDefinitionRepository;
  private final BusinessProcessActivityInstanceRepository activityInstanceRepository;
  private final ObjectMapper objectMapper;

  /** Inicializa o serviço com a tarefa de correlação e os repositórios canônicos do BPM. */
  public AutomaticBusinessProcessActivityService(
      AgentTaskRepository taskRepository,
      BusinessProcessActivityDefinitionRepository activityDefinitionRepository,
      BusinessProcessActivityInstanceRepository activityInstanceRepository,
      ObjectMapper objectMapper) {
    this.taskRepository = taskRepository;
    this.activityDefinitionRepository = activityDefinitionRepository;
    this.activityInstanceRepository = activityInstanceRepository;
    this.objectMapper = objectMapper;
  }

  /** Conclui uma atividade automática com horários, custo e evidência da execução técnica real. */
  @Transactional
  public void completeFromExecution(
      Long anchorTaskId,
      String activityId,
      String sourceExecutionReference,
      Instant enteredAt,
      Instant exitedAt,
      BigDecimal knownCostUsd,
      String resultEvidenceJson) {
    validateCompletion(
        anchorTaskId,
        activityId,
        sourceExecutionReference,
        enteredAt,
        exitedAt,
        resultEvidenceJson);
    AgentTask anchorTask =
        taskRepository
            .findById(anchorTaskId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Tarefa de correlação da atividade automática não encontrada."));
    if (anchorTask.getProcessDefinition() == null
        || anchorTask.getSourceReference() == null
        || anchorTask.getSourceReference().isBlank()) {
      throw new IllegalStateException(
          "Tarefa de correlação sem processo ou referência operacional.");
    }
    BusinessProcessActivityDefinition activity =
        activityDefinitionRepository
            .findByProcessDefinitionIdAndActivityId(
                anchorTask.getProcessDefinition().getId(), activityId.trim())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Atividade automática não encontrada na versão do processo."));
    Optional<BusinessProcessActivityInstance> latest =
        activityInstanceRepository
            .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
                activity.getId(), anchorTask.getSourceReference());
    if (latest.isPresent()
        && COMPLETED.equals(latest.get().getStatus())
        && representsExecution(latest.get(), sourceExecutionReference)) {
      return;
    }
    BusinessProcessActivityInstance instance =
        latest
            .filter(value -> !COMPLETED.equals(value.getStatus()))
            .orElseGet(
                () ->
                    newInstance(
                        activity,
                        anchorTask.getSourceReference(),
                        latest.map(value -> value.getOccurrenceNumber() + 1).orElse(1),
                        enteredAt));
    instance.setStatus(COMPLETED);
    instance.setEnteredAt(enteredAt);
    instance.setExitedAt(exitedAt);
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(
        evidence(sourceExecutionReference, resultEvidenceJson).toString());
    instance.setBlockedReason(null);
    instance.setKnownCostUsd(knownCostUsd);
    instance.setCostCoverage(knownCostUsd == null ? "NOT_REPORTED" : "COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setUpdatedAt(exitedAt);
    activityInstanceRepository.save(instance);
  }

  /** Valida os dados mínimos e a ordem temporal antes de modificar o estado do processo. */
  private void validateCompletion(
      Long anchorTaskId,
      String activityId,
      String sourceExecutionReference,
      Instant enteredAt,
      Instant exitedAt,
      String resultEvidenceJson) {
    if (anchorTaskId == null
        || activityId == null
        || activityId.isBlank()
        || sourceExecutionReference == null
        || sourceExecutionReference.isBlank()
        || enteredAt == null
        || exitedAt == null
        || resultEvidenceJson == null
        || resultEvidenceJson.isBlank()) {
      throw new IllegalArgumentException(
          "Conclusão automática exige tarefa, atividade, execução, datas e evidência.");
    }
    if (exitedAt.isBefore(enteredAt)) {
      throw new IllegalArgumentException(
          "Conclusão automática não pode terminar antes de iniciar.");
    }
  }

  /** Cria uma nova ocorrência ainda não persistida para a execução automática informada. */
  private BusinessProcessActivityInstance newInstance(
      BusinessProcessActivityDefinition activity,
      String sourceReference,
      int occurrenceNumber,
      Instant enteredAt) {
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(sourceReference);
    instance.setOccurrenceNumber(occurrenceNumber);
    instance.setCreatedAt(enteredAt);
    return instance;
  }

  /** Monta evidência estruturada sem serializar um documento JSON dentro de outro JSON. */
  private ObjectNode evidence(String sourceExecutionReference, String resultEvidenceJson) {
    try {
      JsonNode result = objectMapper.readTree(resultEvidenceJson);
      if (result == null || !result.isObject()) {
        throw new IllegalArgumentException(
            "Evidência da atividade automática deve ser um objeto JSON.");
      }
      ObjectNode evidence = objectMapper.createObjectNode();
      evidence.put("sourceExecutionReference", sourceExecutionReference.trim());
      evidence.set("result", result);
      return evidence;
    } catch (IllegalArgumentException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error(
          "Falha ao estruturar evidência de atividade automática. sourceExecutionReference={} evidenceLength={}",
          sourceExecutionReference,
          resultEvidenceJson.length(),
          ex);
      throw new IllegalArgumentException("Evidência da atividade automática inválida.", ex);
    }
  }

  /** Reconhece o replay da mesma execução para manter a ocorrência idempotente. */
  private boolean representsExecution(
      BusinessProcessActivityInstance instance, String sourceExecutionReference) {
    try {
      String persisted =
          objectMapper
              .readTree(instance.getObjectiveEvidenceJson())
              .path("sourceExecutionReference")
              .asText();
      return sourceExecutionReference.trim().equals(persisted);
    } catch (Exception ex) {
      log.warn(
          "Falha ao comparar evidência de atividade automática já persistida. activityInstanceId={} sourceExecutionReference={}",
          instance.getId(),
          sourceExecutionReference,
          ex);
      return false;
    }
  }
}
