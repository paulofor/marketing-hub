package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Responsabilidade: impedir comandos de recuperação inventados, históricos ou disponíveis
 * indevidamente.
 */
class ProductProcessActivityRecoveryResolverTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Expõe somente o comando da atividade publicada declarada para remediar o card bloqueado. */
  @ParameterizedTest
  @CsvSource({"NOT_STARTED,true", "PENDING,false", "IN_PROGRESS,false", "BLOCKED,true"})
  void preservesRecoveryAvailabilityAndHistory(String recoveryState, boolean available)
      throws Exception {
    var blocked = group("technicalHomologation", "BLOCKED", false, true);
    var recovery = group("prototypeCorrection", recoveryState, available, true);
    var result =
        ProductProcessActivityRecoveryResolver.resolve(
            List.of(blocked, recovery), definitions(), json, 70L, "experiment:92");
    var action = result.getFirst().recoveryAction();
    assertThat(action.activityId()).isEqualTo("prototypeCorrection");
    assertThat(action.ownerName()).isEqualTo("Dédalo");
    assertThat(action.sequenceNumber()).isEqualTo(6);
    assertThat(action.actionAvailable()).isEqualTo(available);
    assertThat(action.operationalState()).isEqualTo(recoveryState);
    assertThat(action.objectiveAchieved()).isEqualTo("COMPLETED".equals(recoveryState));
    assertThat(result.getFirst().executionRequestAvailable()).isFalse();
    assertThat(result.getFirst().operationalState()).isEqualTo("BLOCKED");
    assertThat(result.getFirst().tasks()).isSameAs(blocked.tasks());
  }

  /** Não apresenta correção concluída como pendência nem remove sua tarefa do histórico. */
  @Test
  void completedRecoveryIsNotADependencyEvenWithHistoricalTasks() throws Exception {
    var recovery = recoveryWithTask("COMPLETED");
    var result =
        ProductProcessActivityRecoveryResolver.resolve(
            List.of(
                group("technicalHomologation", "BLOCKED", false, true),
                group("psiqueRecovery", "NOT_STARTED", false, true),
                recovery),
            definitions(),
            json,
            70L,
            "experiment:92");
    assertThat(result).allSatisfy(activity -> assertThat(activity.recoveryAction()).isNull());
    assertThat(result.getLast().tasks()).isEqualTo(recovery.tasks());
  }

  /** Mantém autoria, custo e estado próprios quando vários cards dependem da mesma correção. */
  @ParameterizedTest
  @CsvSource({"PENDING", "IN_PROGRESS", "BLOCKED"})
  void dependentActivitiesDoNotAcquireTheRecoveryTask(String state) throws Exception {
    var recovery = recoveryWithTask(state);
    var definition = definitions().get("prototypeCorrection");
    definition.setDefinitionJson(
        "{\"remediatesActivities\":[\"psiqueAdherent\",\"psiqueRecovery\",\"psiqueSafety\",\"commercialIntegrityReview\"]}");
    var originals =
        List.of(
            group("psiqueAdherent", "BLOCKED", false, true),
            group("psiqueRecovery", "NOT_STARTED", false, true),
            group("psiqueSafety", "NOT_STARTED", false, true),
            group("commercialIntegrityReview", "NOT_STARTED", false, true));
    var groups = new java.util.ArrayList<>(originals);
    groups.add(recovery);
    var result =
        ProductProcessActivityRecoveryResolver.resolve(
            groups, Map.of("prototypeCorrection", definition), json, 70L, "experiment:92");
    for (int index = 0; index < originals.size(); index++) {
      var dependent = result.get(index);
      assertThat(dependent.tasks()).isSameAs(originals.get(index).tasks());
      assertThat(dependent.taskCount()).isZero();
      assertThat(dependent.operationalState()).isEqualTo(originals.get(index).operationalState());
      assertThat(dependent.recoveryAction().activityId()).isEqualTo("prototypeCorrection");
      assertThat(dependent.recoveryAction().latestTask().taskId()).isEqualTo(385L);
    }
    assertThat(result.getLast().recoveryAction()).isNull();
    assertThat(result.getLast().tasks()).hasSize(1);
  }

  /** Mostra a tentativa atual sem vazar tarefa de outro ciclo, processo ou auditoria extensa. */
  @Test
  void tracksOnlyTheLatestTaskInTheExactContext() throws Exception {
    var node = json.valueToTree(group("prototypeCorrection", "BLOCKED", true, true));
    ((com.fasterxml.jackson.databind.node.ObjectNode) node)
        .set(
            "tasks",
            json.readTree(
                """
        [{"taskId":378,"processDefinitionId":70,"sourceReference":"experiment:92","status":"BLOCKED",
          "assignedAgentNickname":"Dédalo","executionError":"URL executável ausente",
          "promptSent":"AUDITORIA_QUE_NAO_DEVE_SER_DUPLICADA"},
         {"taskId":400,"processDefinitionId":70,"sourceReference":"experiment:91","status":"COMPLETED"},
         {"taskId":401,"processDefinitionId":69,"sourceReference":"experiment:92","status":"COMPLETED"}]
        """));
    var result =
        ProductProcessActivityRecoveryResolver.resolve(
            List.of(
                group("technicalHomologation", "BLOCKED", false, true),
                json.treeToValue(node, ProductProcessActivityExecutionGroupResponse.class)),
            definitions(),
            json,
            70L,
            "experiment:92");
    var task = result.getFirst().recoveryAction().latestTask();
    assertThat(task.taskId()).isEqualTo(378L);
    assertThat(task.status()).isEqualTo("BLOCKED");
    assertThat(task.executionError()).isEqualTo("URL executável ausente");
    assertThat(json.writeValueAsString(task))
        .doesNotContain("promptSent", "AUDITORIA_QUE_NAO_DEVE_SER_DUPLICADA");
  }

  /** Não atribui recuperação a atividade concluída, histórica ou já liberada pelo backend. */
  @ParameterizedTest
  @CsvSource({
    "COMPLETED,false,true",
    "BLOCKED,true,true",
    "BLOCKED,false,false",
    "NOT_STARTED,false,true"
  })
  void doesNotInventRecovery(String state, boolean available, boolean selected) throws Exception {
    var result =
        ProductProcessActivityRecoveryResolver.resolve(
            List.of(
                group("technicalHomologation", state, available, selected),
                group("prototypeCorrection", "NOT_STARTED", true, true)),
            definitions(),
            json,
            70L,
            "experiment:92");
    assertThat(result.getFirst().recoveryAction()).isNull();
  }

  /** Uma definição de recuperação inativa não pode oferecer comando no card de origem. */
  @Test
  void ignoresInactiveRecovery() throws Exception {
    var result =
        ProductProcessActivityRecoveryResolver.resolve(
            List.of(
                group("technicalHomologation", "BLOCKED", false, true),
                group("prototypeCorrection", "NOT_STARTED", false, false)),
            definitions(),
            json,
            70L,
            "experiment:92");
    assertThat(result.getFirst().recoveryAction()).isNull();
  }

  /** Usa metadado mínimo do grafo sem acoplar a projeção ao produto ou a números de tarefas. */
  private Map<String, BusinessProcessActivityDefinition> definitions() {
    var definition = new BusinessProcessActivityDefinition();
    definition.setId(706L);
    definition.setDefinitionJson("{\"remediatesActivities\":[\"technicalHomologation\"]}");
    return Map.of("prototypeCorrection", definition);
  }

  /** Cria tentativa sintética de correção no mesmo contrato HTTP, sem persistência produtiva. */
  private ProductProcessActivityExecutionGroupResponse recoveryWithTask(String state)
      throws Exception {
    var node =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.valueToTree(group("prototypeCorrection", state, "BLOCKED".equals(state), true));
    node.set(
        "tasks",
        json.readTree(
            """
        [{"taskId":385,"processDefinitionId":70,"sourceReference":"experiment:92",
          "status":"%s","assignedAgentNickname":"Dédalo","estimatedCostUsd":0.25}]
        """
                .formatted(state)));
    node.put("taskCount", 1);
    return json.treeToValue(node, ProductProcessActivityExecutionGroupResponse.class);
  }

  /** Monta a projeção pública com a mesma serialização usada pelo endpoint de atividades. */
  private ProductProcessActivityExecutionGroupResponse group(
      String activityId, String state, boolean available, boolean selected) throws Exception {
    return json.readValue(
        """
        {"activityId":"%s","activityName":"Atividade de teste","activityOwnerName":"Dédalo","sequenceNumber":6,
         "operationalState":"%s","objectiveAchieved":%s,"selectedVersionActivity":%s,
         "tasks":[],"executionRequestAvailable":%s,
         "executionControl":{"executorType":"AGENT","interactionType":"COMMAND",
           "actionLabel":"Criar tarefa de correção","actionAvailable":%s,
           "availabilityReason":"Disponibilidade fornecida pelo backend"}}
        """
            .formatted(
                activityId, state, "COMPLETED".equals(state), selected, available, available),
        ProductProcessActivityExecutionGroupResponse.class);
  }
}
