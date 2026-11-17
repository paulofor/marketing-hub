package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: prevenir diagnóstico pago no ciclo rejeitado sem quebrar correção técnica. */
class PdeFunctionalCorrectionCycleReadinessProviderTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final PdeFunctionalCorrectionCycleReadinessProvider provider =
      new PdeFunctionalCorrectionCycleReadinessProvider(tasks, new ObjectMapper());

  /** Exige sucessor no caso original e em outra identidade, sem depender de nome de produto. */
  @ParameterizedTest
  @ValueSource(longs = {7L, 18L})
  void blocksFunctionalCorrectionBeforeAnotherModelCall(long productId) {
    String source = "experiment:" + (9000 + productId);
    when(tasks.findPdeValidationTaskSnapshots(source, "pde-construction-approval"))
        .thenReturn(
            List.of(
                proof(676, "psiqueAdherent", "FUNCTIONAL_ADJUSTMENT"),
                proof(677, "prototypeCorrection", "FUNCTIONAL_ADJUSTMENT")));

    var readiness =
        provider.readiness(
            process(),
            activity("prototypeCorrection"),
            Product.builder().id(productId).build(),
            source);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason())
        .contains("#676", "sucessores vinculados", "nova candidata", "referência atual");
  }

  /** Preserva falha técnica mais recente e não confunde diagnóstico de correção com parecer. */
  @Test
  void preservesCurrentTechnicalFailure() {
    when(tasks.findPdeValidationTaskSnapshots("experiment:9123", "pde-construction-approval"))
        .thenReturn(
            List.of(
                proof(676, "psiqueAdherent", "FUNCTIONAL_ADJUSTMENT"),
                proof(678, "technicalHomologation", "TECHNICAL_FAILURE")));
    assertThat(
            provider
                .readiness(
                    process(),
                    activity("prototypeCorrection"),
                    Product.builder().id(7L).build(),
                    "experiment:9123")
                .ready())
        .isTrue();
  }

  /** Não bloqueia uma nova candidata que ainda não recebeu rejeição própria. */
  @Test
  void permitsSuccessorWithoutItsOwnRejection() {
    when(tasks.findPdeValidationTaskSnapshots("experiment:9124", "pde-construction-approval"))
        .thenReturn(List.of());
    assertThat(
            provider
                .readiness(
                    process(),
                    activity("prototypeCorrection"),
                    Product.builder().id(7L).build(),
                    "experiment:9124")
                .ready())
        .isTrue();
  }

  /** Mantém a correção legada e restringe o gate ao domínio e atividade correspondentes. */
  @Test
  void preservesLegacyAndOtherActivities() {
    var process = process();
    var product = Product.builder().id(7L).build();
    assertThat(
            provider
                .readiness(
                    process,
                    activity("prototypeCorrection"),
                    product,
                    "product:7@agent-validation-v1")
                .ready())
        .isTrue();
    assertThat(provider.supports(process, activity("psiqueAdherent"))).isFalse();
    process.setProcessCode("another-domain");
    assertThat(provider.supports(process, activity("prototypeCorrection"))).isFalse();
    verifyNoInteractions(tasks);
  }

  /** Monta a definição genérica do domínio sem alterar contratos ou processos publicados. */
  private BusinessProcessDefinition process() {
    var process = new BusinessProcessDefinition();
    process.setId(117L);
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(8);
    return process;
  }

  /** Confere o gate na revisão publicada do processo com o contrato canônico completo. */
  @Test
  void supportsCurrentCanonicalRevision() throws Exception {
    var process = process();
    process.setVersionNumber(42);
    for (var source :
        new ObjectMapper()
            .readTree(
                java.nio.file.Path.of("../../infra/testing/pde-commercial-principles/sources.json")
                    .toFile())) {
      if (process.getProcessCode().equals(source.path("processCode").asText())) {
        process.setDiagramJson(source.path("diagram").toString());
      }
    }
    assertThat(provider.supports(process, activity("prototypeCorrection"))).isTrue();
  }

  /** O suplemento exato não cria outro ciclo, produto ou tarefa de construção. */
  @ParameterizedTest
  @ValueSource(longs = {4, 97004})
  void permitsOnlyBoundEvidenceSupplement(long productId) throws Exception {
    prepareSupplement(productId);
    var ready =
        provider.readiness(
            process(),
            activity("prototypeCorrection"),
            Product.builder().id(productId).slug("fixture-product").build(),
            "experiment:" + (9000 + productId));
    assertThat(ready.ready()).isTrue();
    assertThat(ready.reason())
        .contains("prova complementar", "revisão independente", "sem criar sucessor");
  }

  /** Um relatório diferente, anterior ou funcional não elimina a proteção do ciclo. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"hash", "product", "reference", "task", "old", "implementation", "missing"})
  void rejectsUnrelatedSupplement(String mutation) throws Exception {
    var report = prepareSupplement(4);
    var scope = (com.fasterxml.jackson.databind.node.ObjectNode) report.path("reviewReevaluation");
    switch (mutation) {
      case "hash" -> scope.put("blockedResultSha256", "a".repeat(64));
      case "product" -> scope.put("productId", 99);
      case "reference" -> scope.put("sourceReference", "experiment:999");
      case "task" -> scope.put("blockedTaskId", 999);
      case "old" -> report.put("generatedAt", "2026-11-01T00:00:00Z");
      case "implementation" -> scope.put("implementationChanged", true);
      case "missing" -> report.remove("reviewReevaluation");
      default -> throw new IllegalArgumentException("Mutação desconhecida.");
    }
    assertThat(
            provider
                .readiness(
                    process(),
                    activity("prototypeCorrection"),
                    Product.builder().id(4L).slug("fixture-product").build(),
                    "experiment:9004")
                .ready())
        .isFalse();
  }

  /** Um novo parecer aprovado não mantém a rejeição antiga como exigência de sucessor. */
  @Test
  void completedReviewSupersedesOldBlockWithoutRewritingIt() {
    String result =
        "{\"decision\":\"BLOCKED\",\"prototypeVersion\":\"candidate-v1\",\"productId\":4,\"sourceReference\":\"experiment:9004\"}";
    var rejected =
        new PdeValidationTaskSnapshot(
            676L,
            117L,
            "commercialIntegrityReview",
            "BLOCKED",
            "FUNCTIONAL_ADJUSTMENT",
            null,
            result,
            null);
    var approved =
        new PdeValidationTaskSnapshot(
            678L,
            117L,
            "commercialIntegrityReview",
            "COMPLETED",
            null,
            null,
            result.replace("BLOCKED", "APPROVED"),
            null);
    when(tasks.findPdeValidationTaskSnapshots("experiment:9004", "pde-construction-approval"))
        .thenReturn(List.of(rejected, approved));
    assertThat(
            provider
                .readiness(
                    process(),
                    activity("prototypeCorrection"),
                    Product.builder().id(4L).build(),
                    "experiment:9004")
                .ready())
        .isTrue();
    assertThat(rejected.status()).isEqualTo("BLOCKED");
  }

  /**
   * Aprovação de outra etapa, versão ou identidade não libera correção funcional no ciclo antigo.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"activity", "process", "version", "product", "reference", "decision"})
  void unrelatedCompletionDoesNotClearFunctionalRejection(String mutation) {
    String result =
        "{\"decision\":\"BLOCKED\",\"prototypeVersion\":\"candidate-v1\",\"productId\":4,\"sourceReference\":\"experiment:9004\"}";
    String approvedResult = result.replace("BLOCKED", "APPROVED");
    String activityId = "commercialIntegrityReview";
    long processId = 117;
    switch (mutation) {
      case "activity" -> activityId = "technicalHomologation";
      case "process" -> processId = 118;
      case "version" -> approvedResult = approvedResult.replace("candidate-v1", "candidate-v2");
      case "product" ->
          approvedResult = approvedResult.replace("\"productId\":4", "\"productId\":99");
      case "reference" ->
          approvedResult = approvedResult.replace("experiment:9004", "experiment:999");
      case "decision" -> approvedResult = approvedResult.replace("APPROVED", "ADJUST");
      default -> throw new IllegalArgumentException("Mutação desconhecida.");
    }
    var rejected =
        new PdeValidationTaskSnapshot(
            676L,
            117L,
            "commercialIntegrityReview",
            "BLOCKED",
            "FUNCTIONAL_ADJUSTMENT",
            null,
            result,
            null);
    var approved =
        new PdeValidationTaskSnapshot(
            678L, processId, activityId, "COMPLETED", null, null, approvedResult, null);
    when(tasks.findPdeValidationTaskSnapshots("experiment:9004", "pde-construction-approval"))
        .thenReturn(List.of(rejected, approved));
    assertThat(
            provider
                .readiness(
                    process(),
                    activity("prototypeCorrection"),
                    Product.builder().id(4L).build(),
                    "experiment:9004")
                .ready())
        .isFalse();
  }

  /** Vincula bytes e identidade da prova simulada, mantendo o parecer original intacto. */
  private com.fasterxml.jackson.databind.node.ObjectNode prepareSupplement(long productId)
      throws Exception {
    var json = new ObjectMapper();
    String reference = "experiment:" + (9000 + productId);
    String result =
        json.createObjectNode()
            .put("decision", "BLOCKED")
            .put("productId", productId)
            .put("sourceReference", reference)
            .put("prototypeVersion", "private-candidate-v1")
            .toString();
    var rejected =
        new PdeValidationTaskSnapshot(
            676L,
            117L,
            "commercialIntegrityReview",
            "BLOCKED",
            "FUNCTIONAL_ADJUSTMENT",
            null,
            result,
            null,
            java.time.Instant.parse("2026-11-14T00:28:55Z"));
    when(tasks.findPdeValidationTaskSnapshots(reference, "pde-construction-approval"))
        .thenReturn(List.of(rejected));
    var report =
        json.createObjectNode()
            .put("generatedAt", "2026-11-17T12:00:00Z")
            .put("agentApprovalClaimed", false)
            .put("reportSha256", "b".repeat(64));
    report
        .putObject("reviewReevaluation")
        .put("contractVersion", "PDE_REVIEW_EVIDENCE_SUPPLEMENT_V1")
        .put("reasonCode", "MISSING_EXECUTION_EVIDENCE")
        .put("implementationChanged", false)
        .put("independentReviewRequired", true)
        .put("productId", productId)
        .put("sourceReference", reference)
        .put("blockedTaskId", 676)
        .put("activityId", "commercialIntegrityReview")
        .put(
            "blockedResultSha256",
            java.util.HexFormat.of()
                .formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(result.getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    var evidence = mock(PdeOperationalControlEvidence.class);
    when(evidence.resolve("fixture-product", "private-candidate-v1"))
        .thenReturn(java.util.Optional.of(report));
    org.springframework.test.util.ReflectionTestUtils.setField(
        provider, "operationalEvidence", evidence);
    return report;
  }

  /** Monta a atividade consultada pelo mesmo ponto de entrada do motor BPM. */
  private BusinessProcessActivityDefinition activity(String code) {
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId(code);
    return activity;
  }

  /** Projeta a rejeição auditável sem transportar prompts nem conteúdo de outro produto. */
  private PdeValidationTaskSnapshot proof(long id, String activity, String category) {
    return new PdeValidationTaskSnapshot(id, 117L, activity, "BLOCKED", category, null, null, null);
  }
}
