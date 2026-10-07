package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: proteger a projeção de retornos condicionais encerrados ou ainda exigidos. */
class ProductProcessConditionalActivityResolverTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Preserva cancelamento e bloqueio sem contá-los como pendência após todos os aceites. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"CANCELLED", "BLOCKED"})
  void recordsInactiveRecoveryWhenEveryRemediatedObjectiveIsResolved(String recoveryState) {
    var correction = group("prototypeCorrection", recoveryState, false, true, false);
    var technical = group("technicalHomologation", "COMPLETED", true, true, false);
    var safety = group("psiqueSafety", "NOT_APPLICABLE", false, true, false);

    var result =
        ProductProcessConditionalActivityResolver.resolve(
            List.of(correction, technical, safety),
            Map.of(
                "prototypeCorrection",
                conditionalDefinition("technicalHomologation", "psiqueSafety")),
            json);

    assertThat(result.getFirst())
        .satisfies(
            recorded -> {
              assertThat(recorded.operationalState()).isEqualTo("RECORDED");
              assertThat(recorded.objectiveAchieved()).isFalse();
              assertThat(recorded.selectedVersionActivity()).isTrue();
              assertThat(recorded.executionRequestAvailable()).isFalse();
              assertThat(recorded.stateEvidence()).isEqualTo("RECORDED_CONDITIONAL_RECOVERY");
              assertThat(recorded.stateReason()).contains("preservada no histórico");
              assertThat(recorded.activityInstanceId()).isEqualTo(9202L);
              assertThat(recorded.tasks()).isSameAs(correction.tasks());
            });
  }

  /** Mantém a recuperação histórica pendente quando qualquer destino ainda precisa de aceite. */
  @ParameterizedTest
  @CsvSource({
    "CANCELLED,BLOCKED,false", "CANCELLED,PENDING,false", "CANCELLED,CANCELLED,false",
    "BLOCKED,BLOCKED,false", "BLOCKED,PENDING,false", "BLOCKED,CANCELLED,false"
  })
  void keepsInactiveRecoveryWhenTargetIsUnresolved(
      String recoveryState, String targetState, boolean targetObjective) {
    var correction = group("prototypeCorrection", recoveryState, false, true, false);
    var target = group("technicalHomologation", targetState, targetObjective, true, false);

    var result =
        ProductProcessConditionalActivityResolver.resolve(
            List.of(correction, target),
            Map.of("prototypeCorrection", conditionalDefinition("technicalHomologation")),
            json);

    assertThat(result.getFirst()).isSameAs(correction);
    assertThat(result.getFirst().operationalState()).isEqualTo(recoveryState);
  }

  /** Não altera cancelamentos obrigatórios nem recuperações ainda disponíveis para execução. */
  @Test
  void doesNotRecordOrdinaryOrActionableCancellation() {
    var ordinary = group("journey", "CANCELLED", false, true, false);
    var actionable = group("prototypeCorrection", "CANCELLED", false, true, true);
    var target = group("technicalHomologation", "COMPLETED", true, true, false);

    var result =
        ProductProcessConditionalActivityResolver.resolve(
            List.of(ordinary, actionable, target),
            Map.of(
                "journey", ordinaryDefinition(),
                "prototypeCorrection", conditionalDefinition("technicalHomologation")),
            json);

    assertThat(result.get(0)).isSameAs(ordinary);
    assertThat(result.get(1)).isSameAs(actionable);
  }

  /** Falha fechada quando o metadado da atividade condicional não pode ser interpretado. */
  @Test
  void keepsCancellationWhenDefinitionMetadataIsInvalid() {
    var correction = group("prototypeCorrection", "CANCELLED", false, true, false);
    var definition = new BusinessProcessActivityDefinition();
    definition.setId(9206L);
    definition.setDefinitionJson("{");

    var result =
        ProductProcessConditionalActivityResolver.resolve(
            List.of(correction, group("technicalHomologation", "COMPLETED", true, true, false)),
            Map.of("prototypeCorrection", definition),
            json);

    assertThat(result.getFirst()).isSameAs(correction);
  }

  /** Conserva a pendência quando a definição aponta para um destino que não pode ser comprovado. */
  @Test
  void keepsCancellationWhenRemediatedTargetIsMissing() {
    var correction = group("prototypeCorrection", "CANCELLED", false, true, false);

    var result =
        ProductProcessConditionalActivityResolver.resolve(
            List.of(correction),
            Map.of("prototypeCorrection", conditionalDefinition("missingTarget")),
            json);

    assertThat(result.getFirst()).isSameAs(correction);
  }

  /** Cria o metadado mínimo de uma recuperação funcional com destinos explícitos. */
  private BusinessProcessActivityDefinition conditionalDefinition(String... targets) {
    var definition = new BusinessProcessActivityDefinition();
    definition.setId(9205L);
    definition.setDefinitionJson(
        "{\"activationMode\":\"ON_FUNCTIONAL_REJECTION\",\"remediatesActivities\":"
            + json.valueToTree(targets)
            + "}");
    return definition;
  }

  /** Cria uma atividade obrigatória sem metadado condicional. */
  private BusinessProcessActivityDefinition ordinaryDefinition() {
    var definition = new BusinessProcessActivityDefinition();
    definition.setId(9204L);
    definition.setDefinitionJson("{}");
    return definition;
  }

  /** Monta a projeção mínima preservando identidade de instância e coleção de auditoria. */
  private ProductProcessActivityExecutionGroupResponse group(
      String id, String state, boolean objective, boolean selected, boolean available) {
    return new ProductProcessActivityExecutionGroupResponse(
        9200L,
        id,
        id,
        "Objetivo " + id,
        "Agente",
        1,
        selected,
        state,
        "Situação original",
        objective,
        "DIRECT",
        9202L,
        2,
        0,
        List.of(),
        available,
        "Contrato original",
        null);
  }
}
