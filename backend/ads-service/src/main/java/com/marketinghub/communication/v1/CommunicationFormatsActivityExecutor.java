package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.backendactivity.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: resolver os formatos previstos a partir do contrato de comunicação vigente. */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationFormatsActivityExecutor implements BackendProductProcessActivityExecutor {
  private final AgentTaskRepository tasks;
  private final BusinessProcessActivityInstanceRepository instances;
  private final BusinessProcessActivityDefinitionRepository definitions;
  private final ExperimentRepository experiments;
  private final IrisCommunicationMaterializationContextProvider context;
  private final ObjectMapper json;

  /** Reconhece somente a resolução determinística de formatos do subprocesso criativo. */
  @Override
  public boolean supports(
      BusinessProcessDefinition process, BusinessProcessActivityDefinition activity) {
    return "creative-production-approval".equals(process.getProcessCode())
        && "route".equals(activity.getActivityId());
  }

  /** Exige contrato concluído da mesma referência antes de permitir qualquer produção. */
  @Override
  @Transactional(readOnly = true)
  public BackendProductProcessActivityReadiness readiness(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    try {
      source(product, reference);
      return new BackendProductProcessActivityReadiness(
          true,
          "Contrato de comunicação concluído; os formatos serão registrados sem publicar ou gastar.");
    } catch (RuntimeException ex) {
      log.warn(
          "Formatos aguardam contrato. productId={} processDefinitionId={} sourceReference={}",
          product.getId(),
          process.getId(),
          reference,
          ex);
      return new BackendProductProcessActivityReadiness(false, ex.getMessage());
    }
  }

  /** Registra a decisão e a dispensa explícita de audiovisual quando nenhum briefing o exige. */
  @Override
  @Transactional
  public BackendProductProcessActivityExecutionResult execute(
      BusinessProcessDefinition process,
      BusinessProcessActivityDefinition activity,
      Product product,
      String reference) {
    var source = source(product, reference);
    JsonNode result = result(source);
    JsonNode brief = result.path("functionalOutput").path("audiovisualBrief");
    boolean audiovisual = brief.isTextual() && !brief.asText().isBlank();
    var evidence = json.createObjectNode();
    evidence.put("evidenceType", "COMMUNICATION_FORMATS_V1");
    evidence.put("communicationTaskId", source.id());
    evidence.put("sourceReference", reference);
    evidence.put("nonAudiovisualRequired", true);
    evidence.put("audiovisualRequired", audiovisual);
    evidence.put("publicationAuthorized", false);
    evidence.put("spendAuthorized", false);
    record(activity, reference, "COMPLETED", true, evidence.toString(), null);
    if (!audiovisual) {
      var optional =
          definitions
              .findByProcessDefinitionIdAndActivityId(process.getId(), "audiovisual")
              .orElseThrow();
      var omitted = evidence.deepCopy();
      omitted.put("evidenceType", "OPTIONAL_ACTIVITY_NOT_REQUIRED_V1");
      omitted.put("activityId", "audiovisual");
      record(
          optional,
          reference,
          "NOT_APPLICABLE",
          false,
          omitted.toString(),
          "O contrato de comunicação não prevê briefing audiovisual; nenhuma mídia foi produzida ou aprovada.");
    }
    return new BackendProductProcessActivityExecutionResult(
        reference,
        "COMPLETED",
        true,
        audiovisual
            ? "Formatos registrados, incluindo audiovisual previsto no briefing."
            : "Peças não audiovisuais previstas; audiovisual dispensado com motivo registrado.");
  }

  /**
   * Confere o produto e a última prova funcional da comunicação sem carregar prompts ou auditoria.
   */
  private AgentTaskFunctionalSnapshot source(Product product, String reference) {
    Long experimentId =
        reference != null && reference.matches("experiment:[1-9][0-9]*")
            ? Long.parseLong(reference.substring(11))
            : context
                .experimentId(reference)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Informe o experimento ou plano comercial oficial deste produto para resolver formatos."));
    var experiment = experiments.findById(experimentId).orElseThrow();
    if (experiment.getProduct() == null
        || !Objects.equals(product.getId(), experiment.getProduct().getId()))
      throw new IllegalStateException("O contrato de comunicação pertence a outro produto.");
    var task =
        tasks
            .findFunctionalSnapshots(
                reference, java.util.Set.of("pde-communication-sales-journey"), null)
            .stream()
            .filter(
                t ->
                    t.processDefinitionId() != null
                        && "pde-communication-sales-journey".equals(t.processCode())
                        && "communicationContract".equals(t.processActivityId())
                        && t.agentKey() != null
                        && "communication-director".equals(t.agentKey()))
            .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Conclua primeiro o contrato de comunicação com Íris."));
    if (!"COMPLETED".equals(task.status()))
      throw new IllegalStateException(
          "A última tentativa do contrato de comunicação ainda não está concluída.");
    var result = result(task);
    if (!"IRIS_COMMUNICATION_V1".equals(result.path("contractVersion").asText())
        || !"COMPLETED".equals(result.path("executionStatus").asText())
        || !"COMMUNICATION_PACKAGE".equals(result.path("outputType").asText())
        || !reference.equals(result.path("sourceReference").asText())
        || result.path("functionalOutput").path("messageStrategy").asText().isBlank()
        || result.path("functionalOutput").path("channelBriefings").isEmpty())
      throw new IllegalStateException(
          "O contrato de comunicação não contém mensagem e briefings íntegros.");
    return task;
  }

  /** Lê o resultado persistido preservando contexto e stack trace de qualquer falha. */
  private JsonNode result(AgentTaskFunctionalSnapshot task) {
    try {
      var result = json.readTree(task.resultJson());
      if (result == null || !result.isObject())
        throw new IllegalStateException("Resultado de comunicação ausente.");
      return result;
    } catch (Exception ex) {
      log.error(
          "Contrato de comunicação inválido. taskId={} processDefinitionId={}",
          task.id(),
          task.processDefinitionId(),
          ex);
      throw new IllegalStateException("O resultado de Íris está inválido.", ex);
    }
  }

  /** Persiste uma nova ocorrência sem apagar tentativas, custos ou decisões anteriores. */
  private void record(
      BusinessProcessActivityDefinition activity,
      String reference,
      String status,
      boolean achieved,
      String evidence,
      String reason) {
    var latest =
        instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            activity.getId(), reference);
    if (latest
        .filter(i -> status.equals(i.getStatus()) && evidence.equals(i.getObjectiveEvidenceJson()))
        .isPresent()) return;
    if ("NOT_APPLICABLE".equals(status)
        && latest
            .filter(i -> "COMPLETED".equals(i.getStatus()) && i.isObjectiveAchieved())
            .isPresent()) return;
    if (tasks.existsByProcessDefinitionIdAndSourceReferenceAndProcessActivityIdAndStatusIn(
        activity.getProcessDefinition().getId(),
        reference,
        activity.getActivityId(),
        java.util.Set.of("PENDING", "IN_PROGRESS")))
      throw new IllegalStateException(
          "A atividade possui tarefa em andamento; aguarde sua conclusão.");
    if (latest
        .filter(i -> java.util.Set.of("PENDING", "IN_PROGRESS").contains(i.getStatus()))
        .isPresent())
      throw new IllegalStateException(
          "A atividade possui trabalho em andamento; aguarde antes de resolver formatos.");
    var instance = new BusinessProcessActivityInstance();
    Instant now = Instant.now();
    instance.setActivityDefinition(activity);
    instance.setSourceReference(reference);
    instance.setOccurrenceNumber(latest.map(i -> i.getOccurrenceNumber() + 1).orElse(1));
    instance.setStatus(status);
    instance.setObjectiveAchieved(achieved);
    instance.setObjectiveEvidenceJson(evidence);
    instance.setBlockedReason(reason);
    instance.setKnownCostUsd(BigDecimal.ZERO);
    instance.setCostCoverage("COMPLETE");
    instance.setEvidenceQuality("DIRECT");
    instance.setCreatedAt(now);
    instance.setUpdatedAt(now);
    instance.setEnteredAt(now);
    instance.setExitedAt(now);
    instances.saveAndFlush(instance);
  }
}
