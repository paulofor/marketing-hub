package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Responsabilidade: comprovar correção, reavaliação com prova nova e preservação das revisões
 * válidas.
 */
class PdeAgentValidationReworkReadinessProviderTest {
  private static final String SOURCE = "product:10@agent-validation-v1";
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final PdeAgentValidationReworkReadinessProvider provider =
      new PdeAgentValidationReworkReadinessProvider(predecessors, tasks, new ObjectMapper());
  private final List<AgentTask> history = new ArrayList<>();
  private BusinessProcessDefinition process;
  private Product product;

  /** Monta a versão publicada e a identidade aceita da nova versão de Mira. */
  @BeforeEach
  void setUp() {
    process = process(80L, 8);
    product =
        Product.builder()
            .id(10L)
            .validationDefinitionJson(
                "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"mira-private-v2\"}}")
            .build();
    when(tasks.findPdeValidationTaskSnapshots(SOURCE, "pde-construction-approval"))
        .thenAnswer(call -> history.stream().map(this::snapshot).toList());
  }

  /** Preserva a rota causal de correção em revisões que alteram somente os objetivos. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {8, 9, 42})
  void exposesCorrectionAfterFunctionalRejectionFromPreviousProcessVersion(int version)
      throws Exception {
    process.setVersionNumber(version);
    for (var source :
        new ObjectMapper()
            .readTree(
                java.nio.file.Path.of("../../infra/testing/pde-commercial-principles/sources.json")
                    .toFile()))
      if (process.getProcessCode().equals(source.path("processCode").asText()))
        process.setDiagramJson(source.path("diagram").toString());
    AgentTask rejection = rejection(350L);
    history.add(rejection);

    var readiness = provider.readiness(process, activity("prototypeCorrection"), product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason())
        .contains("#350")
        .contains("Psique · cenário aderente")
        .contains("rotina útil desaparece")
        .containsIgnoringCase("manter a rotina visível")
        .contains("homologação técnica");
  }

  /** Oferece tarefa causal para #377 mesmo sem aceitação privada ou rejeição funcional. */
  @Test
  void exposesCorrectionForTechnicalHomologationFailureWithoutAcceptance() {
    product.setValidationDefinitionJson("{}");
    AgentTask failure = task(377L, process, "technicalHomologation", "BLOCKED");
    failure.setBlockerCategory("TECHNICAL_FAILURE");
    failure.setExecutionError("A URL do PDE é inválida ou contém parâmetros não permitidos.");
    failure.setBlockerAction("Implemente o protótipo da versão e seus testes próprios.");
    history.add(failure);

    var readiness = provider.readiness(process, activity("prototypeCorrection"), product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason())
        .contains("#377", "não conseguiu homologar", "URL", "testes próprios");
    assertThat(readiness.reason()).doesNotContain("rejeitou a versão");
    assertThat(
            provider.requiresFreshExecution(
                process, activity("prototypeCorrection"), product, SOURCE))
        .isTrue();
  }

  /** Repete a mesma homologação quando o executor falha sem atribuir defeito ao protótipo. */
  @ParameterizedTest
  @CsvSource({
    "EXECUTOR_FAILURE,Falha no catálogo instalado do executor.",
    "TECHNICAL_FAILURE,O harness instalado não possui cenários próprios para este produto. Implemente-os antes da homologação; não reutilize outro PDE."
  })
  void retriesTechnicalHomologationWithoutOpeningProductCorrection(
      String category, String executionError) {
    AgentTask failure = task(581L, process, "technicalHomologation", "BLOCKED");
    failure.setBlockerCategory(category);
    failure.setExecutionError(executionError);
    history.add(failure);
    BusinessProcessActivityDefinition technical = activity("technicalHomologation");
    when(predecessors.readiness(process, technical, SOURCE))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                true, "A atividade predecessora possui conclusão comprovada."));

    var correction = provider.readiness(process, activity("prototypeCorrection"), product, SOURCE);
    var retry = provider.readiness(process, technical, product, SOURCE);

    assertThat(correction.ready()).isFalse();
    assertThat(correction.reason()).contains("Nenhuma rejeição funcional");
    assertThat(retry.ready()).isTrue();
    assertThat(provider.requiresFreshExecution(process, technical, product, SOURCE)).isTrue();
  }

  /** Não transforma falhas técnicas de cenários ou do próprio corretor em rejeições funcionais. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"psiqueSafety", "prototypeCorrection", "access"})
  void ignoresTechnicalFailureOutsideHomologation(String activityId) {
    AgentTask failure = task(377L, process, activityId, "BLOCKED");
    failure.setBlockerCategory("TECHNICAL_FAILURE");
    history.add(failure);
    assertThat(
            provider.readiness(process, activity("prototypeCorrection"), product, SOURCE).ready())
        .isFalse();
  }

  /** Uma homologação posterior concluída supera a falha técnica sem inventar correção funcional. */
  @Test
  void retiresTechnicalFailureAfterSuccessfulRetry() {
    AgentTask failure = task(377L, process, "technicalHomologation", "BLOCKED");
    failure.setBlockerCategory("TECHNICAL_FAILURE");
    history.add(failure);
    history.add(task(379L, process, "technicalHomologation", "COMPLETED"));
    assertThat(
            provider.readiness(process, activity("prototypeCorrection"), product, SOURCE).ready())
        .isFalse();
  }

  /** Impede reexecutar o harness antes de existir correção válida e versionada. */
  @Test
  void blocksTechnicalHomologationUntilCorrectionIsReady() {
    history.add(rejection(350L));

    var readiness = provider.readiness(process, activity("technicalHomologation"), product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("publique uma nova versão");
  }

  /** Libera nova homologação quando Dédalo corrige a causa em versão distinta da rejeitada. */
  @Test
  void releasesTechnicalHomologationAfterVersionedCorrection() {
    history.add(rejection(350L));
    history.add(correction(351L));
    BusinessProcessActivityDefinition technical = activity("technicalHomologation");
    when(predecessors.readiness(process, technical, SOURCE))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                true, "A atividade predecessora possui conclusão comprovada."));

    var readiness = provider.readiness(process, technical, product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason()).contains("nova versão");
  }

  /** Preserva o checkpoint concluído quando uma tarefa condicional indevida foi cancelada. */
  @Test
  void cancelledCorrectionDoesNotReplaceValidCheckpoint() {
    history.add(rejection(350L));
    history.add(correction(351L));
    history.add(task(352L, process, "prototypeCorrection", "CANCELLED"));
    BusinessProcessActivityDefinition technical = activity("technicalHomologation");
    when(predecessors.readiness(process, technical, SOURCE))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                true, "A atividade predecessora possui conclusão comprovada."));

    var readiness = provider.readiness(process, technical, product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.reason()).contains("nova versão");
  }

  /** Não aceita a homologação da versão aposentada como autorização para Psique na v8. */
  @Test
  void requiresCurrentProcessTechnicalApprovalForPsique() {
    AgentTask oldTechnical = task(349L, process(69L, 7), "technicalHomologation", "COMPLETED");
    oldTechnical.setResultJson(
        "{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v1\"}");
    history.add(oldTechnical);
    BusinessProcessActivityDefinition psique = activity("psiqueAdherent");
    when(predecessors.readiness(process, psique, SOURCE))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                true, "A atividade predecessora possui conclusão comprovada."));

    var readiness = provider.readiness(process, psique, product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("mira-private-v2");
  }

  /**
   * Preserva a prova antiga e exige revisão ligada ao pacote corrigido em identidades distintas.
   */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {71L, 845L})
  void correctedKitPackageRequiresFreshMatrixAndSubsequentIndependentReview(long productId)
      throws Exception {
    String source = "experiment:" + (9000 + productId);
    product.setId(productId);
    product.setValidationDefinitionJson(
        "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"kit-fixture-v1\"}}");
    when(tasks.findPdeValidationTaskSnapshots(source, "pde-construction-approval"))
        .thenAnswer(call -> history.stream().map(this::snapshot).toList());
    var technical = activity("technicalHomologation");
    var psique = activity("psiqueAdherent");
    when(predecessors.readiness(process, psique, source))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Histórico preservado."));
    var old = task(610L, process, "technicalHomologation", "COMPLETED");
    var result = new ObjectMapper().createObjectNode();
    result.put("decision", "APPROVED");
    result.put("prototypeVersion", "kit-fixture-v1");
    result.put("fixtureContract", "PDE_PRIVATE_KIT_FIXTURES_V1");
    result
        .putArray("scenarios")
        .addObject()
        .put("scenarioCode", "ADHERENT")
        .put("packageFileCount", 24)
        .put("zipSha256", "a".repeat(64));
    old.setResultJson(result.toString());
    history.add(old);
    assertThat(provider.requiresFreshExecution(process, technical, product, source)).isTrue();
    assertThat(provider.readiness(process, psique, product, source).ready()).isFalse();
    var oldReview = task(611L, process, "psiqueAdherent", "COMPLETED");
    oldReview.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"kit-fixture-v1\"}");
    history.add(oldReview);
    result.put("packageContractVersion", "PDE_PRIVATE_KIT_PACKAGE_V2");
    ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("scenarios").get(0))
        .put("packageFileCount", 36);
    var corrected = task(612L, process, "technicalHomologation", "COMPLETED");
    corrected.setResultJson(result.toString());
    history.add(corrected);
    assertThat(provider.requiresFreshExecution(process, technical, product, source)).isFalse();
    assertThat(provider.readiness(process, psique, product, source).ready()).isTrue();
    assertThat(provider.requiresFreshExecution(process, psique, product, source)).isTrue();
    var newReview = task(613L, process, "psiqueAdherent", "COMPLETED");
    newReview.setResultJson(oldReview.getResultJson());
    history.add(newReview);
    assertThat(provider.requiresFreshExecution(process, psique, product, source)).isFalse();
    assertThat(old.getStatus()).isEqualTo("COMPLETED");
    assertThat(old.getResultJson()).doesNotContain("PDE_PRIVATE_KIT_PACKAGE_V2");
  }

  /** Uma segunda rejeição exige nova correção mesmo quando a versão já teve homologação técnica. */
  @Test
  void blocksReviewerAfterNewFunctionalRejection() {
    history.add(correction(353L));
    AgentTask approved = task(354L, process, "technicalHomologation", "COMPLETED");
    approved.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
    history.add(approved);
    AgentTask newRejection = rejection(355L);
    newRejection.setProcessDefinition(process);
    history.add(newRejection);

    var readiness = provider.readiness(process, activity("psiqueAdherent"), product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("#355", "publique uma nova versão");
    assertThat(
            provider.requiresFreshExecution(
                process, activity("prototypeCorrection"), product, SOURCE))
        .isTrue();
  }

  /** Mantém bloqueios atuais visíveis e retira do estado corrente somente os já corrigidos. */
  @ParameterizedTest
  @CsvSource({
    "technicalHomologation,352,true",
    "technicalHomologation,355,false",
    "psiqueAdherent,352,true",
    "psiqueAdherent,355,false",
    "prototypeCorrection,356,false"
  })
  void keepsCurrentBlockAndRetiresCorrectedBlock(
      String activityId, long taskId, boolean expectedFresh) {
    history.add(rejection(350L));
    history.add(correction(353L));
    if ("prototypeCorrection".equals(activityId)) {
      AgentTask newRejection = rejection(355L);
      newRejection.setProcessDefinition(process);
      history.add(newRejection);
    }
    history.add(task(taskId, process, activityId, "BLOCKED"));
    assertThat(provider.requiresFreshExecution(process, activity(activityId), product, SOURCE))
        .isEqualTo(expectedFresh);
  }

  /** Uma falha técnica posterior impede reutilizar a aprovação anterior como prova vigente. */
  @Test
  void newerTechnicalFailureInvalidatesPreviousApproval() {
    history.add(correction(353L));
    AgentTask approved = task(354L, process, "technicalHomologation", "COMPLETED");
    approved.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
    history.add(approved);
    history.add(task(356L, process, "technicalHomologation", "BLOCKED"));
    var psique = activity("psiqueAdherent");
    when(predecessors.readiness(process, psique, SOURCE))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Histórico concluído."));
    assertThat(provider.readiness(process, psique, product, SOURCE).ready()).isFalse();
  }

  /** Limita a regra à versão que declara a rota de retrabalho. */
  @Test
  void preservesVersionEightAndRejectsPreviousContracts() {
    assertThat(provider.supports(process, activity("prototypeCorrection"))).isTrue();
    assertThat(provider.supports(process, activity("psiqueSafety"))).isTrue();

    process.setVersionNumber(7);

    assertThat(provider.supports(process, activity("prototypeCorrection"))).isFalse();
  }

  /** Reabre apenas Têmis com prova nova, em identidades distintas, sem repetir matriz e Psique. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {10, 110})
  void newControlEvidenceReopensOnlyIntegrityReview(long productId) throws Exception {
    String source = prepareIntegrityEvidence(productId, true);
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isTrue();
    assertThat(
            provider.requiresFreshExecution(
                process, activity("commercialIntegrityReview"), product, source))
        .isTrue();
    assertThat(
            provider.readiness(process, activity("prototypeCorrection"), product, source).ready())
        .isFalse();
    for (String previous :
        List.of("technicalHomologation", "psiqueAdherent", "psiqueRecovery", "psiqueSafety"))
      assertThat(provider.requiresFreshExecution(process, activity(previous), product, source))
          .isFalse();
  }

  /** Reabre apenas SAFETY e retém Têmis até parecer independente sobre os controles corrigidos. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {4, 97004})
  void scopedControlsReopenSafetyAndRequireIndependentCitation(long productId) throws Exception {
    var report = prepareScopedSafety(productId);
    String source = "product:" + productId + "@agent-validation-v1";
    assertThat(provider.requiresFreshExecution(process, activity("psiqueSafety"), product, source))
        .isTrue();
    assertThat(provider.readiness(process, activity("psiqueSafety"), product, source).ready())
        .isTrue();
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isFalse();
    for (String preserved : List.of("technicalHomologation", "psiqueAdherent", "psiqueRecovery"))
      assertThat(provider.requiresFreshExecution(process, activity(preserved), product, source))
          .isFalse();
    assertThat(
            provider.readiness(process, activity("prototypeCorrection"), product, source).ready())
        .isFalse();
    var next = task(505L, process, "psiqueSafety", "COMPLETED");
    next.setSourceReference(source);
    next.setUpdatedAt(Instant.now());
    var result = new ObjectMapper().createObjectNode();
    result.put("decision", "APPROVED").put("prototypeVersion", "mira-private-v2");
    next.setResultJson(result.toString());
    history.add(next);
    when(tasks.findById(505L)).thenReturn(java.util.Optional.of(next));
    assertThat(provider.requiresFreshExecution(process, activity("psiqueSafety"), product, source))
        .isFalse();
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isFalse();
    result.putArray("evidence").add("Controles revisados: " + report.path("reportSha256").asText());
    next.setResultJson(result.toString());
    assertThat(provider.requiresFreshExecution(process, activity("psiqueSafety"), product, source))
        .isFalse();
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isTrue();
    var finalReview = task(506L, process, "commercialIntegrityReview", "COMPLETED");
    finalReview.setSourceReference(source);
    finalReview.setResultJson(result.toString());
    history.add(finalReview);
    assertThat(provider.requiresFreshExecution(process, activity("psiqueSafety"), product, source))
        .isFalse();
    assertThat(
            provider.requiresFreshExecution(
                process, activity("commercialIntegrityReview"), product, source))
        .isFalse();
    assertThat(history.get(4).getStatus()).isEqualTo("BLOCKED");
  }

  /** Exercita o comando real do BPM: reabre SAFETY concluído e impede Têmis antes do parecer. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {4, 97004})
  void actualCommandReopensOnlyScopedReview(long productId) throws Exception {
    prepareScopedSafety(productId);
    String source = "product:" + productId + "@agent-validation-v1";
    product.setAutomaticExecutionEnabled(true);
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATION_V1");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"psiqueSafety\",\"type\":\"TASK\",\"label\":\"Segurança\",\"responsibleAgentKeys\":[\"customer-agent\"]},{\"id\":\"commercialIntegrityReview\",\"type\":\"TASK\",\"responsibleAgentKeys\":[\"meta-ad-approver\"]}]}");
    var safety = activity("psiqueSafety");
    safety.setDefinitionJson("{\"responsibleAgentKeys\":[\"customer-agent\"]}");
    var integrity = activity("commercialIntegrityReview");
    integrity.setDefinitionJson("{\"responsibleAgentKeys\":[\"meta-ad-approver\"]}");
    var processes =
        mock(
            com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository
                .class);
    var definitions =
        mock(
            com.marketinghub.repository.jpa.businessprocess
                .BusinessProcessActivityDefinitionRepository.class);
    var products = mock(com.marketinghub.repository.jpa.product.ProductRepository.class);
    var experiments = mock(com.marketinghub.repository.jpa.experiment.ExperimentRepository.class);
    var instances =
        mock(
            com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository
                .class);
    var commands = mock(com.marketinghub.agenttask.AgentTaskService.class);
    var completed = new com.marketinghub.agenttask.BusinessProcessActivityInstance();
    completed.setId(901L);
    completed.setActivityDefinition(safety);
    completed.setSourceReference(source);
    completed.setStatus("COMPLETED");
    completed.setObjectiveAchieved(true);
    completed.setOccurrenceNumber(1);
    completed.setCreatedAt(Instant.now().minusSeconds(600));
    completed.setUpdatedAt(completed.getCreatedAt());
    when(processes.findById(process.getId())).thenReturn(java.util.Optional.of(process));
    when(products.findById(productId)).thenReturn(java.util.Optional.of(product));
    when(definitions.findByProcessDefinitionIdAndActivityId(process.getId(), "psiqueSafety"))
        .thenReturn(java.util.Optional.of(safety));
    when(definitions.findByProcessDefinitionIdAndActivityId(
            process.getId(), "commercialIntegrityReview"))
        .thenReturn(java.util.Optional.of(integrity));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
                process.getProcessCode(), "product:" + productId + "@"))
        .thenReturn(List.of(completed));
    when(commands.retryBlockedByHumanOrRefreshPending(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true)))
        .thenReturn(mock(com.marketinghub.agenttask.AgentTaskResponse.class));
    var service =
        new com.marketinghub.businessprocess.execution.service
            .BusinessProcessActivityExecutionService(
            processes,
            definitions,
            tasks,
            mock(
                com.marketinghub.repository.jpa.agenttask.AgentTaskActivityCoverageRepository
                    .class),
            instances,
            mock(com.marketinghub.repository.jpa.planning.CommercialPlanRepository.class),
            null,
            products,
            experiments,
            commands,
            new ObjectMapper(),
            List.of(),
            List.of(),
            List.of(provider));
    var response =
        service.requestProductActivityExecution(
            process.getId(), productId, "psiqueSafety", null, null, source);
    assertThat(response.tasks()).hasSize(1);
    var captured =
        org.mockito.ArgumentCaptor.forClass(
            com.marketinghub.agenttask.CreateAgentTaskRequest.class);
    org.mockito.Mockito.verify(commands)
        .retryBlockedByHumanOrRefreshPending(
            captured.capture(), org.mockito.ArgumentMatchers.eq(true));
    assertThat(captured.getValue().assignedAgentKey()).isEqualTo("customer-agent");
    assertThat(captured.getValue().sourceReference()).isEqualTo(source);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.requestProductActivityExecution(
                    process.getId(), productId, "commercialIntegrityReview", null, null, source))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
        .hasMessageContaining("Psique · limite e segurança");
    org.mockito.Mockito.verifyNoMoreInteractions(commands);
    org.mockito.Mockito.verify(tasks, org.mockito.Mockito.never())
        .findById(org.mockito.ArgumentMatchers.anyLong());
  }

  /** Não transporta a solicitação de revisão de um contexto para outro já validado. */
  @Test
  void doesNotReopenAnotherContextUsingTheSameVersion() throws Exception {
    var report = prepareScopedSafety(97004);
    ((com.fasterxml.jackson.databind.node.ObjectNode) report.path("scopedRevalidation"))
        .put("productId", 4)
        .put("sourceReference", "experiment:111");
    history.get(4).setStatus("COMPLETED");
    history
        .get(4)
        .setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
    String source = "product:97004@agent-validation-v1";
    assertThat(provider.requiresFreshExecution(process, activity("psiqueSafety"), product, source))
        .isFalse();
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isTrue();
  }

  /** Não repete novo parecer recusado usando o pedido ligado ao parecer antigo. */
  @Test
  void scopedSafetyRejectionRequiresNewDiagnosis() throws Exception {
    prepareScopedSafety(4);
    String source = "product:4@agent-validation-v1";
    var rejected = task(505L, process, "psiqueSafety", "BLOCKED");
    rejected.setSourceReference(source);
    rejected.setUpdatedAt(Instant.now());
    rejected.setBlockerCategory("FUNCTIONAL_ADJUSTMENT");
    rejected.setResultJson("{\"decision\":\"BLOCKED\",\"prototypeVersion\":\"mira-private-v2\"}");
    history.add(rejected);
    when(tasks.findById(505L)).thenReturn(java.util.Optional.of(rejected));
    assertThat(provider.requiresFreshExecution(process, activity("psiqueSafety"), product, source))
        .isFalse();
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isFalse();
  }

  /** Rejeita mudanças funcionais, provas incompletas ou identidades trocadas sem liberar Têmis. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "productId",
        "sourceReference",
        "blockedTaskId",
        "previousSafetyTaskId",
        "blockedResultSha256",
        "previousSafetyResultSha256",
        "customerExperienceChanged",
        "backendRevision",
        "sourceSha256",
        "testReceiptSha256",
        "evidenceReference",
        "retainedActivities",
        "generatedAt",
        "missingScope"
      })
  void invalidScopedBindingNeverPromotesOldSafetyApproval(String defect) throws Exception {
    var report = prepareScopedSafety(4);
    var scope = (com.fasterxml.jackson.databind.node.ObjectNode) report.path("scopedRevalidation");
    var acceptance =
        (com.fasterxml.jackson.databind.node.ObjectNode) scope.path("technicalAcceptance");
    switch (defect) {
      case "productId", "blockedTaskId", "previousSafetyTaskId" -> scope.put(defect, 999999);
      case "sourceReference", "blockedResultSha256", "previousSafetyResultSha256" ->
          scope.put(defect, "wrong");
      case "customerExperienceChanged" -> scope.put(defect, true);
      case "backendRevision", "sourceSha256", "testReceiptSha256", "evidenceReference" ->
          acceptance.remove(defect);
      case "retainedActivities" -> scope.putArray(defect).add("technicalHomologation");
      case "generatedAt" -> report.put(defect, "2020-01-01T00:00:00Z");
      case "missingScope" -> report.remove("scopedRevalidation");
      default -> throw new IllegalArgumentException(defect);
    }
    String source = "product:4@agent-validation-v1";
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isFalse();
  }

  /** Monta suplemento íntegro de uma correção técnica sem mudar a experiência válida. */
  private com.fasterxml.jackson.databind.node.ObjectNode prepareScopedSafety(long productId)
      throws Exception {
    String source = prepareIntegrityEvidence(productId, true);
    var mapper = new ObjectMapper();
    var report = mapper.createObjectNode();
    report.put("generatedAt", Instant.now().minusSeconds(120).toString());
    report.put("prototypeVersion", "mira-private-v2");
    report.put("operationalControlsContract", "PDE_CONTEXTUAL_EXECUTION_CONTROLS_V1");
    report.put("agentApprovalClaimed", false);
    report.put("reportSha256", "a".repeat(64));
    report.put("testReceiptSha256", "b".repeat(64));
    report
        .putObject("implementationChange")
        .put("closedContextProtectionChanged", true)
        .put("validContextExperienceChanged", false);
    report
        .putArray("sourceFiles")
        .addObject()
        .put("path", "src/Control.java")
        .put("sha256", "c".repeat(64));
    var scope = report.putObject("scopedRevalidation");
    scope.put("contractVersion", "PDE_SCOPED_SAFETY_REVALIDATION_V1");
    scope.put("scope", "PRIVATE_CONTEXT_CONTROLS");
    scope.put("productId", productId).put("sourceReference", source);
    scope.put("customerExperienceChanged", false).put("independentReviewRequired", true);
    scope.put("blockedTaskId", 504).put("previousSafetyTaskId", 503);
    scope.put("affectedActivityId", "psiqueSafety");
    scope.putArray("retainedActivities").add("psiqueAdherent").add("psiqueRecovery");
    for (var task : history)
      when(tasks.findById(task.getId())).thenReturn(java.util.Optional.of(task));
    scope.put("blockedResultSha256", resultDigest(history.get(4).getResultJson()));
    scope.put("previousSafetyResultSha256", resultDigest(history.get(3).getResultJson()));
    var acceptance = scope.putObject("technicalAcceptance");
    acceptance.put("origin", "OPERATOR_VALIDATION").put("decision", "ACCEPTED_CONTROLS");
    acceptance
        .put("backendRevision", "d".repeat(40))
        .put("evidenceReference", "Local deterministic controls receipt");
    acceptance.put("sourcePath", "src/Control.java").put("sourceSha256", "c".repeat(64));
    acceptance.put("testReceiptSha256", "b".repeat(64));
    var evidence = mock(PdeOperationalControlEvidence.class);
    when(evidence.resolve(product.getSlug(), "mira-private-v2"))
        .thenReturn(java.util.Optional.of(report));
    org.springframework.test.util.ReflectionTestUtils.setField(
        provider, "operationalEvidence", evidence);
    return report;
  }

  /** Calcula a identidade dos bytes originais do parecer usado na regressão. */
  private String resultDigest(String value) throws Exception {
    return java.util.HexFormat.of()
        .formatHex(
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  /** Uma prova anterior ao bloqueio não autoriza repetir o parecer com entradas iguais. */
  @Test
  void unchangedEvidencePreservesBlock() throws Exception {
    String source = prepareIntegrityEvidence(10, false);
    assertThat(
            provider
                .readiness(process, activity("commercialIntegrityReview"), product, source)
                .ready())
        .isFalse();
    assertThat(
            provider.requiresFreshExecution(
                process, activity("commercialIntegrityReview"), product, source))
        .isFalse();
    assertThat(
            provider.readiness(process, activity("prototypeCorrection"), product, source).ready())
        .isTrue();
  }

  /** O novo parecer aprovado resolve o bloqueio antigo sem fabricar correção de produto. */
  @Test
  void actualLaterApprovalResolvesHistoricalRejection() throws Exception {
    String source = prepareIntegrityEvidence(10, true);
    var approved = task(506L, process, "commercialIntegrityReview", "COMPLETED");
    approved.setSourceReference(source);
    approved.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
    history.add(approved);
    assertThat(
            provider.readiness(process, activity("prototypeCorrection"), product, source).ready())
        .isFalse();
    assertThat(
            provider.requiresFreshExecution(
                process, activity("commercialIntegrityReview"), product, source))
        .isFalse();
  }

  /** Monta aprovações, rejeição e suplemento temporal sem alterar contratos ou dados publicados. */
  private String prepareIntegrityEvidence(long productId, boolean fresh) throws Exception {
    String source = "product:" + productId + "@agent-validation-v1";
    product.setId(productId);
    product.setSlug("synthetic-" + productId);
    when(tasks.findPdeValidationTaskSnapshots(source, "pde-construction-approval"))
        .thenAnswer(call -> history.stream().map(this::snapshot).toList());
    long id = 500;
    for (String activity :
        List.of("technicalHomologation", "psiqueAdherent", "psiqueRecovery", "psiqueSafety")) {
      var approved = task(id++, process, activity, "COMPLETED");
      approved.setSourceReference(source);
      approved.setResultJson(
          "{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
      history.add(approved);
    }
    var rejected = task(504, process, "commercialIntegrityReview", "BLOCKED");
    rejected.setSourceReference(source);
    rejected.setBlockerCategory("FUNCTIONAL_ADJUSTMENT");
    rejected.setDeliveredAt(null);
    rejected.setUpdatedAt(Instant.now().minusSeconds(fresh ? 300 : 30));
    rejected.setResultJson(
        "{\"decision\":\"BLOCKED\",\"prototypeVersion\":\"mira-private-v2\",\"rootCause\":\"Faltam provas dos controles.\"}");
    history.add(rejected);
    when(tasks.findById(504L)).thenReturn(java.util.Optional.of(rejected));
    var evidence = mock(PdeOperationalControlEvidence.class);
    var report = new ObjectMapper().createObjectNode();
    report.put("generatedAt", Instant.now().minusSeconds(120).toString());
    when(evidence.resolve(product.getSlug(), "mira-private-v2"))
        .thenReturn(java.util.Optional.of(report));
    org.springframework.test.util.ReflectionTestUtils.setField(
        provider, "operationalEvidence", evidence);
    when(predecessors.readiness(
            org.mockito.ArgumentMatchers.eq(process),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq(source)))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Aprovado"));
    return source;
  }

  /**
   * Reavalia somente a prova incompleta, preservando matriz, aprovações e a decisão independente.
   */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {4, 97004})
  void supplementReopensOnlyAffectedScenario(long productId) throws Exception {
    var report = prepareScenarioSupplement(productId);
    String source = "product:" + productId + "@agent-validation-v1";
    assertThat(provider.readiness(process, activity("psiqueRecovery"), product, source).ready())
        .isTrue();
    assertThat(
            provider.requiresFreshExecution(process, activity("psiqueRecovery"), product, source))
        .isTrue();
    for (String previous : List.of("technicalHomologation", "psiqueAdherent"))
      assertThat(provider.requiresFreshExecution(process, activity(previous), product, source))
          .isFalse();
    assertThat(provider.readiness(process, activity("psiqueSafety"), product, source).ready())
        .isFalse();
    assertThat(
            provider.readiness(process, activity("prototypeCorrection"), product, source).ready())
        .isFalse();
    assertThat(history.getLast().getStatus()).isEqualTo("BLOCKED");
    assertThat(report.path("agentApprovalClaimed").asBoolean()).isFalse();

    var approved = task(505, process, "psiqueRecovery", "COMPLETED");
    approved.setResultJson("{\"decision\":\"APPROVED\",\"prototypeVersion\":\"mira-private-v2\"}");
    history.add(approved);
    assertThat(
            provider.requiresFreshExecution(process, activity("psiqueRecovery"), product, source))
        .isFalse();
    assertThat(provider.readiness(process, activity("psiqueSafety"), product, source).ready())
        .isTrue();
  }

  /** Prova velha, funcional, de outro contexto ou reaproveitada após nova recusa não abre gasto. */
  @ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "old",
        "product",
        "reference",
        "activity",
        "task",
        "hash",
        "implementation",
        "approval",
        "independent",
        "reason",
        "missing",
        "newRejection"
      })
  void unrelatedOrUnchangedSupplementDoesNotReopenScenario(String mutation) throws Exception {
    var report = prepareScenarioSupplement(4);
    var supplement =
        (com.fasterxml.jackson.databind.node.ObjectNode) report.path("reviewReevaluation");
    switch (mutation) {
      case "old" -> report.put("generatedAt", Instant.now().minusSeconds(600).toString());
      case "product" -> supplement.put("productId", 99);
      case "reference" -> supplement.put("sourceReference", "experiment:999");
      case "activity" -> supplement.put("activityId", "psiqueSafety");
      case "task" -> supplement.put("blockedTaskId", 999);
      case "hash" -> supplement.put("blockedResultSha256", "a".repeat(64));
      case "implementation" -> supplement.put("implementationChanged", true);
      case "approval" -> report.put("agentApprovalClaimed", true);
      case "independent" -> supplement.put("independentReviewRequired", false);
      case "reason" -> supplement.put("reasonCode", "PRODUCT_DEFECT");
      case "missing" -> report.remove("reviewReevaluation");
      case "newRejection" -> {
        var next = task(505, process, "psiqueRecovery", "BLOCKED");
        next.setSourceReference("product:4@agent-validation-v1");
        next.setBlockerCategory("FUNCTIONAL_ADJUSTMENT");
        next.setResultJson(history.getLast().getResultJson());
        history.add(next);
        when(tasks.findById(505L)).thenReturn(java.util.Optional.of(next));
      }
      default -> throw new IllegalArgumentException("Mutação desconhecida: " + mutation);
    }
    String source = "product:4@agent-validation-v1";
    assertThat(provider.readiness(process, activity("psiqueRecovery"), product, source).ready())
        .isFalse();
    assertThat(
            provider.requiresFreshExecution(process, activity("psiqueRecovery"), product, source))
        .isFalse();
  }

  /** Monta o caso original com identidades intercambiáveis e hash dos bytes reais do parecer. */
  private com.fasterxml.jackson.databind.node.ObjectNode prepareScenarioSupplement(long productId)
      throws Exception {
    String source = prepareIntegrityEvidence(productId, true);
    history.removeIf(
        task -> List.of("psiqueRecovery", "psiqueSafety").contains(task.getProcessActivityId()));
    var rejected = history.getLast();
    rejected.setProcessActivityId("psiqueRecovery");
    var report = new ObjectMapper().createObjectNode();
    report.put("generatedAt", Instant.now().minusSeconds(120).toString());
    report.put("reportSha256", "b".repeat(64));
    report.put("agentApprovalClaimed", false);
    report
        .putObject("reviewReevaluation")
        .put("contractVersion", "PDE_REVIEW_EVIDENCE_SUPPLEMENT_V1")
        .put("reasonCode", "MISSING_EXECUTION_EVIDENCE")
        .put("implementationChanged", false)
        .put("independentReviewRequired", true)
        .put("productId", productId)
        .put("sourceReference", source)
        .put("activityId", "psiqueRecovery")
        .put("blockedTaskId", rejected.getId())
        .put(
            "blockedResultSha256",
            java.util.HexFormat.of()
                .formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                        .digest(
                            rejected
                                .getResultJson()
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    var evidence = mock(PdeOperationalControlEvidence.class);
    when(evidence.resolve(product.getSlug(), "mira-private-v2"))
        .thenReturn(java.util.Optional.of(report));
    org.springframework.test.util.ReflectionTestUtils.setField(
        provider, "operationalEvidence", evidence);
    return report;
  }

  /** Cria uma rejeição funcional histórica com orientação operacional preservada. */
  private AgentTask rejection(long id) {
    AgentTask task = task(id, process(69L, 7), "psiqueAdherent", "BLOCKED");
    task.setBlockerCategory("FUNCTIONAL_ADJUSTMENT");
    task.setBlockerAction("Manter a rotina visível depois da conclusão.");
    task.setExecutionError("Psique rejeitou a continuidade da experiência.");
    task.setResultJson(
        "{\"rootCause\":\"A rotina útil desaparece quando o cenário é concluído.\"}");
    return task;
  }

  /** Cria a conclusão de Dédalo que liga a rejeição à versão corrigida. */
  private AgentTask correction(long id) {
    AgentTask task = task(id, process, "prototypeCorrection", "COMPLETED");
    task.setResultJson(
        """
        {
          "decision":"READY",
          "correctionPlan":{
            "previousPrototypeVersion":"mira-private-v1",
            "correctedPrototypeVersion":"mira-private-v2",
            "nextActivityId":"technicalHomologation",
            "verification":{
              "technicalRevalidationRequired":true,
              "noExternalSideEffects":true
            }
          }
        }
        """);
    return task;
  }

  /** Monta uma tarefa ordenável do mesmo produto e processo funcional. */
  private AgentTask task(
      long id, BusinessProcessDefinition taskProcess, String activityId, String status) {
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setProcessDefinition(taskProcess);
    task.setSourceReference(SOURCE);
    task.setProcessActivityId(activityId);
    task.setStatus(status);
    task.setCreatedAt(Instant.parse("2026-09-07T10:00:00Z").plusSeconds(id));
    task.setUpdatedAt(task.getCreatedAt());
    return task;
  }

  /** Cria a identidade mínima de uma versão do macroprocesso. */
  private BusinessProcessDefinition process(long id, int version) {
    BusinessProcessDefinition definition = new BusinessProcessDefinition();
    definition.setId(id);
    definition.setProcessCode("pde-construction-approval");
    definition.setVersionNumber(version);
    definition.setStatus(version == 8 ? "PUBLISHED" : "RETIRED");
    return definition;
  }

  /** Cria a definição relacional da atividade consultada. */
  private BusinessProcessActivityDefinition activity(String activityId) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId((long) activityId.hashCode());
    activity.setProcessDefinition(process);
    activity.setActivityId(activityId);
    return activity;
  }

  /** Projeta os mesmos registros do cenário sem fornecer os prompts à decisão de retrabalho. */
  private PdeValidationTaskSnapshot snapshot(AgentTask task) {
    return new PdeValidationTaskSnapshot(
        task.getId(),
        task.getProcessDefinition().getId(),
        task.getProcessActivityId(),
        task.getStatus(),
        task.getBlockerCategory(),
        task.getBlockerAction(),
        task.getResultJson(),
        task.getExecutionError(),
        task.getUpdatedAt());
  }
}
