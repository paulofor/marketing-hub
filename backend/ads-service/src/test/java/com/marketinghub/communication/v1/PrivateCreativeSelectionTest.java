package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.agenttask.CommunicationMaterializationContextProvider;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: validar seleção privada pelos pareceres reais sem criar contrato comercial. */
class PrivateCreativeSelectionTest {
  private final ObjectMapper json = new ObjectMapper();
  private final CommunicationMaterializationContextProvider contexts =
      mock(CommunicationMaterializationContextProvider.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final PrivateCreativeSelection selection =
      new PrivateCreativeSelection(
          contexts, new PrivateCommunicationCreativeProof(tasks, instances, json), json);
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final Product product = Product.builder().id(98111L).build();
  private final ObjectNode context = json.createObjectNode();
  private final List<AgentTaskFunctionalSnapshot> history = new ArrayList<>();
  private String reference = "experiment:98102";
  private String version = "private-pde-test-v3";

  /** Prepara identidades sintéticas e exatamente os mesmos pixels examinados pelos dois agentes. */
  @BeforeEach
  void fixture() {
    process.setId(98121L);
    process.setProcessCode("creative-production-approval");
    configureContext();
    configureHistory();
    when(contexts.resolve(anyString()))
        .thenAnswer(i -> Optional.of(json.convertValue(context, Map.class)));
    when(tasks.findFunctionalSnapshotsByProcessSince(eq(process.getId()), anyString(), isNull()))
        .thenAnswer(i -> history);
  }

  /** Reproduz o bloqueio original sem plano e valida outra referência independente. */
  @ParameterizedTest
  @ValueSource(longs = {0L, 10000L})
  void selectsPrivatePixelsWithAllPreviousCommercialFlagsDisabled(long offset) {
    if (offset > 0) {
      product.setId(product.getId() + offset);
      process.setId(process.getId() + offset);
      reference = "experiment:" + (98102 + offset);
      version = "another-private-version";
      configureContext();
      configureHistory();
      when(tasks.findFunctionalSnapshotsByProcessSince(
              eq(process.getId()), eq(reference), isNull()))
          .thenAnswer(i -> history);
    }
    var readiness = selection.readiness(process, product, reference).orElseThrow();
    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reviewAndAccept()).isTrue();
    assertThat(readiness.workspaceReferenceId()).isNull();
    assertThat(readiness.auditEvidenceReference())
        .contains(reference, "agent-task:98154", "a".repeat(64));
    var completed =
        selection
            .completeApproval(process, product, reference, readiness.confirmationToken())
            .orElseThrow();
    assertThat(completed.objectiveAchieved()).isTrue();
    assertThat(completed.structuredEvidence())
        .containsEntry("scope", "PRIVATE_PREPARATION")
        .containsEntry("sourceReference", reference)
        .containsEntry("prototypeVersion", version)
        .containsEntry("published", false)
        .containsEntry("paymentEnabled", false)
        .containsEntry("publicationAuthorized", false)
        .containsEntry("externalMediaSpendAuthorized", false)
        .doesNotContainKeys("commercialPlanId", "creativePackageId", "visualAssets");
    assertThat(
            json.valueToTree(completed.structuredEvidence()).path("creativeProof").path("reviews"))
        .hasSize(2);
    verify(tasks, never()).save(any());
    verifyNoInteractions(instances);
  }

  /** Não transforma nenhum dos modos privados documentados em uma importação comercial. */
  @ParameterizedTest
  @ValueSource(
      strings = {"LEARNING_CYCLE_PRIVATE", "PRODUCT_PRIVATE", "INITIAL_EXPERIMENT_PRIVATE"})
  void recognizesCanonicalPrivateModes(String mode) {
    context.put("mode", mode);
    assertThat(selection.readiness(process, product, reference).orElseThrow().ready()).isTrue();
  }

  /** Mantém o modo privado bloqueado sem retornar vazio e sem recuperar uma aprovação antiga. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "gate",
        "product",
        "reference",
        "version",
        "preparation",
        "publication",
        "payment",
        "media",
        "pixels",
        "review",
        "pending",
        "failed",
        "producer"
      })
  void rejectsBrokenPrivateProofBeforeDecision(String cause) throws Exception {
    switch (cause) {
      case "gate" -> {
        context.put("availability", "MISSING");
        context.put("reason", "Gate privado revogado.");
      }
      case "product" -> ((ObjectNode) context.path("product")).put("id", 98777L);
      case "reference" -> context.put("sourceReference", "experiment:98777");
      case "version" -> context.put("prototypeVersion", "another-version");
      case "preparation" -> context.remove(PrivateCreativePreparationContext.FIELD);
      case "publication" -> context.put("publicationAuthorized", true);
      case "payment" -> context.put("paymentEnabled", true);
      case "media" -> context.put("externalMediaSpendAuthorized", true);
      case "pixels" -> {
        var review = (ObjectNode) json.readTree(history.getLast().resultJson());
        ((ObjectNode) review.path("renderedAssetAudit").get(0)).put("sha256", "b".repeat(64));
        history.set(2, task(98156L, "commercial", "meta-ad-approver", "COMPLETED", review));
      }
      case "review" -> {
        var review = (ObjectNode) json.readTree(history.getLast().resultJson());
        review.put("decision", "ADJUST");
        review.withArray("requiredChanges").add("Corrija a prova.");
        history.set(2, task(98156L, "commercial", "meta-ad-approver", "COMPLETED", review));
      }
      case "pending" ->
          history.add(
              task(98157L, "customer", "customer-agent", "IN_PROGRESS", json.createObjectNode()));
      case "failed" ->
          history.add(
              task(98157L, "customer", "customer-agent", "BLOCKED", json.createObjectNode()));
      case "producer" ->
          history.add(
              task(
                  98157L,
                  "nonAudiovisual",
                  "communication-director",
                  "IN_PROGRESS",
                  json.createObjectNode()));
      default -> throw new IllegalArgumentException(cause);
    }
    assertThat(selection.readiness(process, product, reference))
        .isPresent()
        .get()
        .satisfies(r -> assertThat(r.ready()).isFalse());
    assertThatThrownBy(() -> selection.completeApproval(process, product, reference, "unapproved"))
        .isInstanceOf(IllegalStateException.class);
    verify(tasks, never()).save(any());
    verifyNoInteractions(instances);
  }

  /**
   * Uma seleção mostrada antes de um novo parecer não autoriza silenciosamente o resultado novo.
   */
  @Test
  void bindsConfirmationToReviewedPackageAndRejectsLaterApprovedReview() throws Exception {
    String first =
        selection.readiness(process, product, reference).orElseThrow().confirmationToken();
    var updated = (ObjectNode) json.readTree(history.getLast().resultJson());
    updated.put("summary", "Parecer novo com os mesmos pixels.");
    history.add(task(98157L, "commercial", "meta-ad-approver", "COMPLETED", updated));
    String next =
        selection.readiness(process, product, reference).orElseThrow().confirmationToken();
    assertThat(next).isNotEqualTo(first);
    assertThatThrownBy(() -> selection.completeApproval(process, product, reference, first))
        .hasMessageContaining("mudaram");
    assertThat(
            selection
                .completeApproval(process, product, reference, next)
                .orElseThrow()
                .objectiveAchieved())
        .isTrue();
  }

  /** Deixa o contrato comercial anterior para o efeito comercial existente. */
  @Test
  void preservesCommercialSelection() {
    context.put("mode", "COMMERCIAL_PLAN");
    assertThat(selection.readiness(process, product, reference)).isEmpty();
    assertThat(selection.completeApproval(process, product, reference, "commercial-token"))
        .isEmpty();
    verifyNoInteractions(tasks, instances);
  }

  /** Monta o contexto canônico sem fatura, plano ou autorização comercial artificiais. */
  private void configureContext() {
    context.removeAll();
    context
        .put("availability", "AVAILABLE")
        .put("inputReadiness", "READY")
        .put("mode", "LEARNING_CYCLE_PRIVATE")
        .put("sourceReference", reference)
        .put("prototypeVersion", version)
        .put("publicationAuthorized", false)
        .put("paymentEnabled", false)
        .put("externalMediaSpendAuthorized", false);
    context.putObject("product").put("id", product.getId());
    context
        .putObject(PrivateCreativePreparationContext.FIELD)
        .put("contractVersion", PrivateCreativePreparationContext.VERSION)
        .put("scope", "PRIVATE_PREPARATION")
        .put("sourceReference", reference)
        .put("prototypeVersion", version)
        .put("nonAudiovisualEvidenceRequired", true)
        .put("publicationAuthorized", false)
        .put("spendAuthorized", false)
        .put("commercialEvidenceClaimed", false);
  }

  /** Monta produção e pareceres independentes da mesma definição e versão. */
  private void configureHistory() {
    history.clear();
    var produced =
        json.createObjectNode()
            .put("contractVersion", "IRIS_COMMUNICATION_V1")
            .put("outputType", "NON_AUDIOVISUAL_PACKAGE")
            .put("executionStatus", "COMPLETED")
            .put("sourceReference", reference);
    produced
        .putObject("functionalOutput")
        .putArray("renderedAssets")
        .addObject()
        .put("artifactId", 98569L)
        .put("sha256", "a".repeat(64))
        .put("prototypeVersion", version)
        .put("privateValidation", true);
    var review = json.createObjectNode().put("decision", "APPROVED");
    review.putArray("requiredChanges");
    review
        .putArray("renderedAssetAudit")
        .addObject()
        .put("artifactId", 98569L)
        .put("sha256", "a".repeat(64));
    history.add(task(98154L, "nonAudiovisual", "communication-director", "COMPLETED", produced));
    history.add(task(98155L, "customer", "customer-agent", "COMPLETED", review));
    history.add(task(98156L, "commercial", "meta-ad-approver", "COMPLETED", review));
  }

  /** Mantém identidades, horários e status no mesmo formato da consulta funcional resumida. */
  private AgentTaskFunctionalSnapshot task(
      long id, String code, String agent, String status, ObjectNode result) {
    return new AgentTaskFunctionalSnapshot(
        id,
        process.getId(),
        process.getProcessCode(),
        code,
        agent,
        status,
        Instant.parse("2026-10-01T00:00:00Z"),
        Instant.parse("2026-10-01T00:01:00Z"),
        result.toString());
  }
}
