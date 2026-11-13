package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Comprova que somente uma falha do executor superada resolve correção sem versão nova. */
class PdeAgentValidationCorrectionPolicyTest {
  private final ObjectMapper json = new ObjectMapper();
  private final PdeAgentValidationCorrectionPolicy policy =
      new PdeAgentValidationCorrectionPolicy(json);
  private static final String SOURCE = "experiment:112";
  private static final String VERSION = "capella-beleza-v3";
  private static final Instant NOW = Instant.parse("2026-11-04T08:00:00Z");

  /**
   * Preserva o contrato explícito e os diagnósticos históricos, exigindo nova prova da mesma
   * identidade.
   */
  @ParameterizedTest
  @ValueSource(strings = {"explicit", "timeout", "catalog", "legacyIdentityCatalog"})
  void acceptsKnownExecutorFailureWithFreshProof(String kind) throws Exception {
    var failed =
        failed(
            kind.equals("explicit") ? "EXECUTOR_FAILURE" : "TECHNICAL_FAILURE",
            kind.equals("legacyIdentityCatalog")
                ? "A referência da homologação não corresponde ao produto alvo."
                : kind.equals("catalog")
                    ? "O harness instalado não possui cenários próprios para este produto."
                    : "Timeout ao homologar o PDE nos dispositivos suportados.");
    var result =
        policy
            .executorCorrectionResolution(
                List.of(failed), correction(draft()), technical(approval()), 7L, SOURCE, VERSION)
            .orElseThrow();
    assertThat(result.path("contractVersion").asText())
        .isEqualTo("PDE_EXECUTOR_CORRECTION_RESOLUTION_V1");
    assertThat(result.path("technicalTaskId").asLong()).isEqualTo(896);
    assertThat(result.path("failedExecutionErrorSha256").asText()).hasSize(64);
    assertThat(failed.status()).isEqualTo("BLOCKED");
  }

  /** Rejeita identidades, provas, ordem ou alterações funcionais incompatíveis com a retomada. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "functional",
        "unknownTechnical",
        "currentIdentityDivergence",
        "activeFailure",
        "otherActivity",
        "missingFailure",
        "otherProcess",
        "activeCorrection",
        "differentVersion",
        "missingVersion",
        "newVersion",
        "sideEffect",
        "wrongNext",
        "wrongOrigin",
        "noRevalidation",
        "readyCorrection",
        "oldTechnical",
        "blockedTechnical",
        "unapprovedTechnical",
        "wrongProduct",
        "wrongSource",
        "wrongVersion",
        "missingTimestamp",
        "oldTimestamp",
        "missingResult",
        "invalidJson"
      })
  void rejectsUnprovedRecovery(String change) throws Exception {
    var failed =
        failed("TECHNICAL_FAILURE", "Timeout ao homologar o PDE nos dispositivos suportados.");
    var draft = draft();
    var approved = approval();
    var plan = (ObjectNode) draft.path("correctionPlan");
    var verification = (ObjectNode) plan.path("verification");
    var correction = correction(draft);
    var technical = technical(approved);
    switch (change) {
      case "functional" -> failed = failed("FUNCTIONAL_ADJUSTMENT", failed.executionError());
      case "unknownTechnical" ->
          failed = failed("TECHNICAL_FAILURE", "Arquivo inválido no produto.");
      case "currentIdentityDivergence" ->
          failed =
              failed("TECHNICAL_FAILURE", "Identidade da tarefa divergente da linhagem canônica.");
      case "activeFailure" ->
          failed =
              snapshot(
                  894,
                  "technicalHomologation",
                  "IN_PROGRESS",
                  "EXECUTOR_FAILURE",
                  null,
                  null,
                  NOW.minusSeconds(100));
      case "otherActivity" ->
          failed =
              snapshot(
                  894,
                  "psiqueSafety",
                  "BLOCKED",
                  "EXECUTOR_FAILURE",
                  null,
                  null,
                  NOW.minusSeconds(100));
      case "otherProcess" ->
          failed =
              new PdeValidationTaskSnapshot(
                  894L,
                  71L,
                  "technicalHomologation",
                  "BLOCKED",
                  "EXECUTOR_FAILURE",
                  null,
                  null,
                  null,
                  NOW.minusSeconds(100));
      case "activeCorrection" ->
          correction =
              snapshot(
                  895,
                  "prototypeCorrection",
                  "IN_PROGRESS",
                  null,
                  draft.toString(),
                  null,
                  NOW.minusSeconds(80));
      case "differentVersion" -> plan.put("previousPrototypeVersion", "other-v1");
      case "missingVersion" -> verification.remove("newVersionPublished");
      case "newVersion" -> verification.put("newVersionPublished", true);
      case "sideEffect" -> verification.put("noExternalSideEffects", false);
      case "wrongNext" -> plan.put("nextActivityId", "publish");
      case "wrongOrigin" -> plan.put("sourceTaskId", 1234);
      case "noRevalidation" -> verification.put("technicalRevalidationRequired", false);
      case "readyCorrection" -> draft.put("decision", "READY");
      case "oldTechnical" ->
          technical =
              snapshot(
                  890, "technicalHomologation", "COMPLETED", null, approved.toString(), null, NOW);
      case "blockedTechnical" ->
          technical =
              snapshot(
                  896, "technicalHomologation", "BLOCKED", null, approved.toString(), null, NOW);
      case "unapprovedTechnical" -> approved.put("decision", "BLOCKED");
      case "wrongProduct" -> approved.put("productId", 8);
      case "wrongSource" -> approved.put("sourceReference", "experiment:999");
      case "wrongVersion" -> approved.put("prototypeVersion", "other-v1");
      case "missingTimestamp" ->
          technical =
              snapshot(
                  896, "technicalHomologation", "COMPLETED", null, approved.toString(), null, null);
      case "oldTimestamp" ->
          technical =
              snapshot(
                  896,
                  "technicalHomologation",
                  "COMPLETED",
                  null,
                  approved.toString(),
                  null,
                  NOW.minusSeconds(90));
      case "missingResult" ->
          technical = snapshot(896, "technicalHomologation", "COMPLETED", null, null, null, NOW);
      case "invalidJson" ->
          correction =
              snapshot(
                  895,
                  "prototypeCorrection",
                  "BLOCKED",
                  null,
                  "invalid",
                  null,
                  NOW.minusSeconds(80));
      case "missingFailure" -> {}
      default -> throw new AssertionError(change);
    }
    if (!List.of("activeCorrection", "invalidJson").contains(change))
      correction = correction(draft);
    if (!List.of(
            "oldTechnical", "blockedTechnical", "missingTimestamp", "oldTimestamp", "missingResult")
        .contains(change)) technical = technical(approved);
    assertThat(
            policy.executorCorrectionResolution(
                change.equals("missingFailure") ? List.of() : List.of(failed),
                correction,
                technical,
                7L,
                SOURCE,
                VERSION))
        .isEmpty();
  }

  /** Produz a falha histórica sem inventar saída funcional ou conclusão. */
  private PdeValidationTaskSnapshot failed(String category, String error) {
    return snapshot(
        894, "technicalHomologation", "BLOCKED", category, null, error, NOW.minusSeconds(100));
  }

  /** Produz a recusa da correção após a falha do executor. */
  private PdeValidationTaskSnapshot correction(ObjectNode draft) {
    return snapshot(
        895,
        "prototypeCorrection",
        "BLOCKED",
        "FUNCTIONAL_ADJUSTMENT",
        draft.toString(),
        null,
        NOW.minusSeconds(80));
  }

  /** Produz uma nova prova técnica da mesma identidade e versão. */
  private PdeValidationTaskSnapshot technical(ObjectNode approval) {
    return snapshot(
        896, "technicalHomologation", "COMPLETED", null, approval.toString(), null, NOW);
  }

  /** Mantém os campos mínimos persistidos utilizados pela política. */
  private PdeValidationTaskSnapshot snapshot(
      long id,
      String activity,
      String status,
      String category,
      String result,
      String error,
      Instant updatedAt) {
    return new PdeValidationTaskSnapshot(
        id, 70L, activity, status, category, null, result, error, updatedAt);
  }

  /** Reproduz os sinais estruturados da correção 895 sem os textos comerciais do produto. */
  private ObjectNode draft() throws Exception {
    return (ObjectNode)
        json.readTree(
            """
        {"decision":"BLOCKED","correctionPlan":{
          "sourceTaskId":894,"rejectedActivityId":"technicalHomologation",
          "previousPrototypeVersion":"capella-beleza-v3","nextActivityId":"technicalHomologation",
          "verification":{"technicalRevalidationRequired":true,
            "newVersionPublished":false,"noExternalSideEffects":true}}}
        """);
  }

  /** Fornece apenas a identidade; o executor do gate testa o conteúdo completo da homologação. */
  private ObjectNode approval() {
    return json.createObjectNode()
        .put("decision", "APPROVED")
        .put("productId", 7)
        .put("sourceReference", SOURCE)
        .put("prototypeVersion", VERSION);
  }
}
