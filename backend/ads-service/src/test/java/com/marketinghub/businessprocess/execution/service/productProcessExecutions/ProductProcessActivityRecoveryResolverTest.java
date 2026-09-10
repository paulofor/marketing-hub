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
            List.of(blocked, recovery), definitions(), json);
    var action = result.getFirst().recoveryAction();
    assertThat(action.activityId()).isEqualTo("prototypeCorrection");
    assertThat(action.ownerName()).isEqualTo("Dédalo");
    assertThat(action.actionAvailable()).isEqualTo(available);
    assertThat(result.getFirst().executionRequestAvailable()).isFalse();
    assertThat(result.getFirst().operationalState()).isEqualTo("BLOCKED");
    assertThat(result.getFirst().tasks()).isSameAs(blocked.tasks());
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
            json);
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
            json);
    assertThat(result.getFirst().recoveryAction()).isNull();
  }

  /** Usa metadado mínimo do grafo sem acoplar a projeção ao produto ou a números de tarefas. */
  private Map<String, BusinessProcessActivityDefinition> definitions() {
    var definition = new BusinessProcessActivityDefinition();
    definition.setId(706L);
    definition.setDefinitionJson("{\"remediatesActivities\":[\"technicalHomologation\"]}");
    return Map.of("prototypeCorrection", definition);
  }

  /** Monta a projeção pública com a mesma serialização usada pelo endpoint de atividades. */
  private ProductProcessActivityExecutionGroupResponse group(
      String activityId, String state, boolean available, boolean selected) throws Exception {
    return json.readValue(
        """
        {"activityId":"%s","activityName":"Atividade de teste","activityOwnerName":"Dédalo",
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
