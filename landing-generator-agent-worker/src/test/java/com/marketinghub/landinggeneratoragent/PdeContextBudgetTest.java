package com.marketinghub.landinggeneratoragent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: verificar projeção sem perda, segregação e bloqueio anterior ao modelo. */
class PdeContextBudgetTest {
  /** Conserva aceites vazios e deduplica provas idênticas sem alterar o snapshot original. */
  @Test
  void preservesContinuityAndDeduplicates() throws Exception {
    var json = new ObjectMapper();
    String proof = "prova😀\\\"\n".repeat(4000);
    String context =
        json.writeValueAsString(
            Map.of(
                "completedActivities",
                java.util.List.of(),
                "historicalContinuityArtifacts",
                java.util.List.of(Map.of("result", proof), Map.of("evidence", proof)),
                "versionContinuity",
                Map.of("pending", 11),
                "productBigIdea",
                "ideia integral"));
    var task = Map.<String, Object>of("taskId", 9001, "processContextJson", context);
    var budget = new PdeContextBudget(json);
    var projected = budget.project(task);
    assertThat(task.get("processContextJson")).isEqualTo(context);
    var view = json.readTree((String) projected.get("processContextJson"));
    assertThat(view.path("completedActivities").size()).isZero();
    assertThat(view.path("versionContinuity").path("pending").asInt()).isEqualTo(11);
    assertThat(view.path("productBigIdea").asText()).isEqualTo("ideia integral");
    var file = Files.createTempFile("pde-synthetic-", ".json");
    try {
      budget.save(file, 9001);
      var refs = json.readTree(Files.readString(file));
      assertThat(refs.path("references").size()).isEqualTo(1);
      assertThat(json.readTree(refs.path("references").elements().next().asText()).asText())
          .isEqualTo(proof);
    } finally {
      Files.delete(file);
    }
  }

  /** Recusa contrato irredutível e contabiliza escape e UTF-8 conservadoramente. */
  @Test
  void blocksOversizeWithoutTruncation() throws Exception {
    var budget = new PdeContextBudget(new ObjectMapper());
    budget.preflight("a".repeat(1000), "{}");
    assertThatThrownBy(() -> budget.preflight("a".repeat(1048577), "{}"))
        .hasMessageContaining("PDE_CONTEXT_BUDGET_V1");
    assertThatThrownBy(() -> budget.preflight("😀\\\"".repeat(160000), "{}"))
        .hasMessageContaining("limit=900000");
  }

  /** Aceita exatamente o orçamento serializado e bloqueia o primeiro byte excedente. */
  @Test
  void checksExactBoundary() throws Exception {
    var json = new ObjectMapper();
    var budget = new PdeContextBudget(json);
    int overhead =
        json.writeValueAsBytes(Map.of("input", "", "outputSchema", json.readTree("{}"))).length;
    budget.preflight("a".repeat(PdeContextBudget.LIMIT - overhead), "{}");
    assertThatThrownBy(
            () -> budget.preflight("a".repeat(PdeContextBudget.LIMIT - overhead + 1), "{}"))
        .hasMessageContaining("PDE_CONTEXT_BUDGET_V1");
  }

  /** Impede até a inicialização do processo externo quando o mínimo não cabe. */
  @Test
  void refusesBeforeStartingExecutor() throws Exception {
    var properties = new LandingGeneratorAgentProperties();
    properties.setCodexCommand("/nonexistent-must-never-start");
    var consumer =
        new PdeConstructionBpmTaskConsumer(
            properties,
            new ObjectMapper(),
            org.mockito.Mockito.mock(AutomaticExecutionControl.class),
            org.mockito.Mockito.mock(CodexTelemetryReporter.class));
    var task =
        Map.<String, Object>of(
            "taskId",
            9902,
            "processCode",
            "pde-construction-approval",
            "activityId",
            "journey",
            "description",
            "x".repeat(1100000));
    assertThatThrownBy(() -> consumer.execute(task)).hasMessageContaining("PDE_CONTEXT_BUDGET_V1");
  }

  /** Reduz volume acima do limite também na correção, preservando conteúdo aninhado integral. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"journey", "prototypeCorrection"})
  void largeNestedContextRemainsRecoverable(String activity) throws Exception {
    var json = new ObjectMapper();
    String original =
        json.writeValueAsString(Map.of("requirements", "q".repeat(1200000), "revision", 3));
    var context =
        Map.of(
            "historicalContinuityArtifacts",
            java.util.List.of(Map.of("result", original)),
            "versionContinuity",
            Map.of("accepted", java.util.List.of(), "pending", 11),
            "correctionAttempts",
            java.util.List.of(Map.of("evidence", original)));
    var task =
        Map.<String, Object>of(
            "taskId",
            8101,
            "activityId",
            activity,
            "processContextJson",
            json.writeValueAsString(context));
    var budget = new PdeContextBudget(json);
    assertThatThrownBy(() -> budget.preflight(json.writeValueAsString(task), "{}"))
        .hasMessageContaining("PDE_CONTEXT_BUDGET_V1");
    var projection = budget.project(task);
    budget.preflight(json.writeValueAsString(projection), "{}");
    assertThat(projection.toString()).contains("contextReference", "pending", "accepted");
  }
}
