package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agent.Agent;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir recuperar rejeição funcional como especificação técnica pronta. */
class IrisRenderPlanRecoveryTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Preserva parecer e autorizações enquanto transfere apenas a renderização determinística. */
  @ParameterizedTest
  @ValueSource(
      strings = {"experiment:102", "experiment:91093", "product:91004@agent-validation-v1"})
  void recoversOnlyTechnicalDependencyInDifferentContexts(String reference) throws Exception {
    var fixture = fixture();
    var current = (ObjectNode) fixture.path("communicationInput");
    var result = (ObjectNode) fixture.path("result");
    current.put("sourceReference", reference);
    ((ObjectNode) current.path("privateCreativePreparation")).put("sourceReference", reference);
    result.put("sourceReference", reference);
    var task = task(result, current, reference);

    var ready = IrisRenderPlanRecovery.pendingPlan(task, current, json);

    assertThat(ready.path("executionStatus").asText()).isEqualTo("READY_FOR_RENDER");
    assertThat(ready.path("evidenceGaps")).isEmpty();
    assertThat(ready.path("functionalOutput")).isEqualTo(result.path("functionalOutput"));
    assertThat(json.readTree(task.getResultJson()).path("executionStatus").asText())
        .isEqualTo("BLOCKED");
    assertThat(current.path("publicationAuthorized").asBoolean()).isFalse();
  }

  /** Lacunas de produto, versão, estratégia, fonte e autoridade continuam bloqueando o fluxo. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "mixedGap",
        "functionalGap",
        "prototype",
        "strategy",
        "publication",
        "payment",
        "spend",
        "paidVideo",
        "missingFlag",
        "rendered",
        "invalidCrop",
        "wrongAgent",
        "wrongActivity",
        "boundedRetry"
      })
  void refusesIncompleteOrChangedContracts(String defect) throws Exception {
    var fixture = fixture();
    var current = (ObjectNode) fixture.path("communicationInput");
    var result = (ObjectNode) fixture.path("result");
    var task = task(result, current, current.path("sourceReference").asText());
    switch (defect) {
      case "mixedGap" ->
          ((com.fasterxml.jackson.databind.node.ArrayNode) result.path("evidenceGaps"))
              .add("Estratégia não aprovada.");
      case "functionalGap" ->
          result.putArray("evidenceGaps").add("Não há prova aprovada do produto.");
      case "prototype" -> current.put("prototypeVersion", "changed-version");
      case "strategy" ->
          ((ObjectNode) current.path("marketStrategicContract")).put("contentHash", "a".repeat(64));
      case "publication" -> current.put("publicationAuthorized", true);
      case "payment" -> current.put("paymentEnabled", true);
      case "spend" -> current.put("externalMediaSpendAuthorized", true);
      case "paidVideo" ->
          ((ObjectNode) current.path("privateCreativePreparation"))
              .put("videoProductionRequestId", 42);
      case "missingFlag" -> current.remove("paymentEnabled");
      case "rendered" ->
          ((ObjectNode) result.path("functionalOutput"))
              .putArray("renderedAssets")
              .addObject()
              .put("artifactId", 999);
      case "invalidCrop" ->
          ((ObjectNode)
                  result
                      .path("functionalOutput")
                      .path("staticAssets")
                      .get(0)
                      .path("renderSpec")
                      .path("crop"))
              .put("width", 0);
      case "wrongAgent" -> task.getAssignedAgent().setAgentKey("customer-agent");
      case "wrongActivity" -> task.setProcessActivityId("human");
      case "boundedRetry" ->
          task.setExecutionError("AUTO_RETRY_MATERIALIZATION_ONCE|503 Storage unavailable");
      default -> throw new IllegalArgumentException(defect);
    }
    task.setResultJson(result.toString());
    assertThat(IrisRenderPlanRecovery.pendingPlan(task, current, json)).isNull();
  }

  /** Monta a tarefa preservando somente os campos necessários à política de recuperação. */
  private AgentTask task(JsonNode result, JsonNode input, String reference) {
    var task = new AgentTask();
    var agent = new Agent();
    agent.setAgentKey("communication-director");
    task.setAssignedAgent(agent);
    var process = new BusinessProcessDefinition();
    process.setProcessCode("creative-production-approval");
    task.setProcessDefinition(process);
    task.setProcessActivityId("nonAudiovisual");
    task.setSourceReference(reference);
    task.setStatus("BLOCKED");
    task.setResultJson(result.toString());
    task.setEvidenceJson(
        json.createObjectNode().set("communicationInputReference", input).toString());
    return task;
  }

  /** Reutiliza a especificação real arquivada na matriz existente de provas criativas. */
  private JsonNode fixture() throws Exception {
    for (var directory = Path.of("").toAbsolutePath();
        directory != null;
        directory = directory.getParent()) {
      var path =
          directory.resolve("infra/testing/mira-creative-recovery/namespace-render-plan.json");
      if (Files.exists(path)) return json.readTree(path.toFile());
    }
    throw new IllegalStateException("Fixture de renderização privada ausente.");
  }
}
