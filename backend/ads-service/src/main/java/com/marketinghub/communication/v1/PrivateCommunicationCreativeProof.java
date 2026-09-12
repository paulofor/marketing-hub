package com.marketinghub.communication.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Responsabilidade: confirmar que a peça atual conserva revisões independentes e decisão de uso.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrivateCommunicationCreativeProof {
  private final AgentTaskRepository tasks;
  private final BusinessProcessActivityInstanceRepository instances;
  private final ObjectMapper json;

  /** Confere a última peça e seus pareceres na definição chamada pelo próprio processo pai. */
  public ObjectNode resolve(
      BusinessProcessActivityInstance call, String reference, String version) {
    try {
      var callProof = json.readTree(call.getObjectiveEvidenceJson());
      long childId = callProof.path("childProcessDefinitionId").asLong();
      require(
          "SUBPROCESS_OBJECTIVE_ACHIEVED_V1".equals(callProof.path("evidenceType").asText())
              && reference.equals(callProof.path("sourceReference").asText())
              && childId > 0,
          "A aprovação dos criativos não identifica o subprocesso deste ciclo.");
      var history = tasks.findFunctionalSnapshotsByProcessSince(childId, reference, null);
      var producer = latest(history, "nonAudiovisual");
      require(
          "communication-director".equals(producer.agentKey()),
          "A peça não foi produzida por Íris.");
      var produced = json.readTree(producer.resultJson());
      var renders = produced.path("functionalOutput").path("renderedAssets");
      require(
          "IRIS_COMMUNICATION_V1".equals(produced.path("contractVersion").asText())
              && "NON_AUDIOVISUAL_PACKAGE".equals(produced.path("outputType").asText())
              && "COMPLETED".equals(produced.path("executionStatus").asText())
              && reference.equals(produced.path("sourceReference").asText())
              && renders.isArray()
              && !renders.isEmpty(),
          "A imagem final do próprio ciclo está ausente.");
      var result =
          json.createObjectNode()
              .put("processDefinitionId", childId)
              .put("producerTaskId", producer.id());
      var approvals = result.putArray("reviews");
      for (String code : List.of("customer", "commercial")) {
        var reviewer = latest(history, code);
        require(
            reviewer.id() > producer.id()
                && ("customer".equals(code) ? "customer-agent" : "meta-ad-approver")
                    .equals(reviewer.agentKey()),
            "A peça atual ainda precisa de nova revisão independente.");
        var review = json.readTree(reviewer.resultJson());
        require(
            "APPROVED".equals(review.path("decision").asText())
                && review.path("requiredChanges").isArray()
                && review.path("requiredChanges").isEmpty(),
            "O último parecer ainda exige ajustes no criativo.");
        for (var render : renders) {
          boolean reviewed = false;
          for (var audit : review.path("renderedAssetAudit"))
            if (render.path("artifactId").asLong() > 0
                && render.path("artifactId").asLong() == audit.path("artifactId").asLong()
                && render.path("sha256").asText().matches("[0-9a-f]{64}")
                && render.path("sha256").asText().equals(audit.path("sha256").asText()))
              reviewed = true;
          require(
              reviewed
                  && version.equals(render.path("prototypeVersion").asText())
                  && render.path("privateValidation").asBoolean(),
              "O parecer não examinou os mesmos pixels e a versão do criativo atual.");
        }
        approvals
            .addObject()
            .put("taskId", reviewer.id())
            .put("activityId", code)
            .put("resultSha256", hash(reviewer.resultJson()));
      }
      var human =
          instances
              .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                  childId, reference)
              .stream()
              .filter(i -> "human".equals(i.getActivityDefinition().getActivityId()))
              .max(Comparator.comparing(BusinessProcessActivityInstance::getOccurrenceNumber))
              .orElseThrow();
      require(
          "COMPLETED".equals(human.getStatus())
              && human.isObjectiveAchieved()
              && "APPROVE"
                  .equals(json.readTree(human.getObjectiveEvidenceJson()).path("decision").asText())
              && human.getExitedAt() != null
              && history.stream()
                  .filter(
                      t ->
                          List.of("nonAudiovisual", "customer", "commercial")
                              .contains(t.processActivityId()))
                  .allMatch(
                      t -> t.createdAt() != null && !t.createdAt().isAfter(human.getExitedAt())),
          "Confirme o uso da peça atual após seus pareceres independentes.");
      result.put("humanDecisionInstanceId", human.getId());
      result.put("humanDecisionSha256", hash(human.getObjectiveEvidenceJson()));
      result.set("renderedAssets", renders);
      return result;
    } catch (Exception ex) {
      log.warn(
          "Prova criativa da jornada privada indisponível. instanceId={} sourceReference={} version={}",
          call.getId(),
          reference,
          version,
          ex);
      throw new IllegalStateException(
          "Os criativos do ciclo precisam de aprovação vigente: " + ex.getMessage(), ex);
    }
  }

  /** Exige a última tentativa concluída, sem recuperar silenciosamente um sucesso anterior. */
  private AgentTaskFunctionalSnapshot latest(
      List<AgentTaskFunctionalSnapshot> history, String code) {
    var task =
        history.stream()
            .filter(t -> code.equals(t.processActivityId()))
            .max(Comparator.comparing(AgentTaskFunctionalSnapshot::id))
            .orElseThrow();
    require(
        "COMPLETED".equals(task.status()),
        "A última tentativa de " + code + " não está concluída.");
    return task;
  }

  /** Preserva o motivo funcional que impede o uso de uma aprovação antiga. */
  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  /** Identifica os bytes dos pareceres e da decisão persistida. */
  private static String hash(String value) throws java.security.NoSuchAlgorithmException {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
  }
}
