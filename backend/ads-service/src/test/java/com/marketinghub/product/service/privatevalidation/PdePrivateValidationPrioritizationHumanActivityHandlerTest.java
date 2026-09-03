package com.marketinghub.product.service.privatevalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.opportunitydossier.OpportunityDossier;
import com.marketinghub.product.Product;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryOpportunity;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityDossierRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryOpportunityRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar o avanço seguro após duas leituras e dois pareceres aprovados. */
class PdePrivateValidationPrioritizationHumanActivityHandlerTest {
  private final ObjectMapper json = new ObjectMapper();
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final OpportunityDossierRepository dossiers = mock(OpportunityDossierRepository.class);
  private final ProductDiscoveryOpportunityRepository opportunities =
      mock(ProductDiscoveryOpportunityRepository.class);
  private final PdePrivateValidationPrioritizationHumanActivityHandler handler =
      new PdePrivateValidationPrioritizationHumanActivityHandler(
          instances, tasks, products, dossiers, opportunities, json);

  /** Libera a priorização apenas com duas leituras integrais e dois pareceres explícitos. */
  @Test
  void exposesFinalPrioritizationAfterAllPrivateGates() throws Exception {
    Instant observedAt = Instant.now().minusSeconds(600);
    givenReadingsAndReviews(observedAt);

    var readiness =
        handler.readiness(process(), activity(), product(), "product:9@private-validation-v1");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.requirements()).allMatch(requirement -> requirement.satisfied());
    assertThat(readiness.auditEvidenceReference())
        .isEqualTo("activity-instance:81;activity-instance:82;agent-task:91;agent-task:92");
  }

  /** Mantém o gate fechado quando o worker conclui, mas não aprova a experiência. */
  @Test
  void blocksFinalPrioritizationWhenPsiqueRequestsAdjustment() throws Exception {
    Instant observedAt = Instant.now().minusSeconds(600);
    givenReadings(observedAt);
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            66L, "product:9@private-validation-v1"))
        .thenReturn(
            List.of(
                review(91L, "humanExperienceReview", "customer-agent", "ADJUST"),
                review(92L, "commercialIntegrityReview", "meta-ad-approver", "APPROVED")));

    var readiness =
        handler.readiness(process(), activity(), product(), "product:9@private-validation-v1");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.requirements())
        .filteredOn(requirement -> "PSIQUE_APPROVED".equals(requirement.code()))
        .singleElement()
        .satisfies(requirement -> assertThat(requirement.satisfied()).isFalse());
  }

  /** Bloqueia decisão textual aprovada quando um fato estruturado de Psique está reprovado. */
  @Test
  void blocksFinalPrioritizationWhenPsiqueStructuredCheckFails() throws Exception {
    Instant observedAt = Instant.now().minusSeconds(600);
    givenReadings(observedAt);
    AgentTask psique = review(91L, "humanExperienceReview", "customer-agent", "APPROVED");
    var result =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(psique.getResultJson());
    ((com.fasterxml.jackson.databind.node.ObjectNode) result.path("privateExperienceChecks"))
        .put("lowEffortReadyResult", false);
    psique.setResultJson(json.writeValueAsString(result));
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            66L, "product:9@private-validation-v1"))
        .thenReturn(
            List.of(
                psique, review(92L, "commercialIntegrityReview", "meta-ad-approver", "APPROVED")));

    var readiness =
        handler.readiness(process(), activity(), product(), "product:9@private-validation-v1");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.requirements())
        .filteredOn(requirement -> "PSIQUE_APPROVED".equals(requirement.code()))
        .singleElement()
        .satisfies(requirement -> assertThat(requirement.satisfied()).isFalse());
  }

  /** Bloqueia a priorização quando o plano omite um dos cinco sinais predeclarados. */
  @Test
  void blocksFinalPrioritizationWhenPrivateSignalContractIsIncomplete() throws Exception {
    Instant observedAt = Instant.now().minusSeconds(600);
    givenReadingsAndReviews(observedAt);
    Product product = product();
    var definition =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.readTree(product.getValidationDefinitionJson());
    definition
        .with("privateValidationPlan")
        .putArray("requiredSignals")
        .add("EXPERIENCE_STARTED")
        .add("VALUE_MOMENT")
        .add("READY_RESULT_USED")
        .add("PREFERRED_OVER_FREE");
    product.setValidationDefinitionJson(json.writeValueAsString(definition));

    var readiness =
        handler.readiness(process(), activity(), product, "product:9@private-validation-v1");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.requirements())
        .filteredOn(requirement -> "PRIVATE_PROTOTYPE_ACCEPTED".equals(requirement.code()))
        .singleElement()
        .satisfies(requirement -> assertThat(requirement.satisfied()).isFalse());
  }

  /** Bloqueia parecer produzido antes de a segunda leitura estar disponível. */
  @Test
  void blocksFinalPrioritizationWhenReviewPredatesReadings() throws Exception {
    Instant observedAt = Instant.now().minusSeconds(600);
    givenReadings(observedAt);
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            66L, "product:9@private-validation-v1"))
        .thenReturn(
            List.of(
                review(
                    91L,
                    "humanExperienceReview",
                    "customer-agent",
                    "APPROVED",
                    observedAt.minusSeconds(60)),
                review(
                    92L,
                    "commercialIntegrityReview",
                    "meta-ad-approver",
                    "APPROVED",
                    observedAt.plusSeconds(120))));

    var readiness =
        handler.readiness(process(), activity(), product(), "product:9@private-validation-v1");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.requirements())
        .filteredOn(requirement -> "REVIEWS_AFTER_PRIVATE_READINGS".equals(requirement.code()))
        .singleElement()
        .satisfies(requirement -> assertThat(requirement.satisfied()).isFalse());
  }

  /** Atualiza produto e gate factual sem criar experimento, cobrança, publicação ou gasto. */
  @Test
  void advancesValidatedProductToCommunicationInStop() throws Exception {
    Instant observedAt = Instant.now().minusSeconds(600);
    givenReadingsAndReviews(observedAt);
    Product product = product();
    ProductDiscoveryOpportunity opportunity = new ProductDiscoveryOpportunity();
    opportunity.setName("Presença pessoal antes de sair");
    opportunity.setEvidenceJson("{\"offers\":[{\"title\":\"Referência preservada\"}]}");
    OpportunityDossier dossier =
        OpportunityDossier.builder()
            .id(21L)
            .createdProduct(product)
            .productDiscoveryOpportunity(opportunity)
            .build();
    when(dossiers.findByCreatedProductId(9L)).thenReturn(Optional.of(dossier));

    handler.approve(
        process(),
        activity(),
        product,
        "product:9@private-validation-v1",
        new ProductProcessActivityExecutionRequest(
            "APPROVE",
            "Operador local",
            "As leituras e os pareceres sustentam a próxima etapa.",
            "private-validation:local-2026-09-02",
            "CONFIRM:pde-construction-approval:finalPrioritization"));

    assertThat(product.getCommercialStatus()).isEqualTo("COMUNICACAO_E_JORNADA");
    assertThat(product.getAutomaticExecutionEnabled()).isFalse();
    assertThat(product.getValidationDefinitionVersion())
        .isEqualTo("PDE_PRIVATE_VALIDATION_V1_COMPLETED");
    var validation = json.readTree(product.getValidationDefinitionJson());
    assertThat(validation.path("purchaseMomentStatus").asText()).isEqualTo("PASS");
    assertThat(validation.path("privateReadings")).hasSize(2);
    var experience = json.readTree(product.getPdeExperienceJson());
    assertThat(experience.path("privateValidation").path("readings")).hasSize(2);
    assertThat(
            experience
                .path("privateValidation")
                .path("independentReviews")
                .path("psique")
                .path("taskId")
                .asLong())
        .isEqualTo(91L);
    var evidence = json.readTree(opportunity.getEvidenceJson());
    assertThat(evidence.path("offers")).hasSize(1);
    assertThat(evidence.path("purchaseMomentGate").path("status").asText()).isEqualTo("PASS");
    assertThat(evidence.path("purchaseMomentGate").path("candidates").path(0).path("readings"))
        .hasSize(2);
    assertThat(
            evidence
                .path("purchaseMomentGate")
                .path("candidates")
                .path(0)
                .path("prototype")
                .path("paymentEnabled")
                .asBoolean())
        .isFalse();
    verify(products).save(product);
    verify(opportunities).save(opportunity);
  }

  /** Configura leituras e pareceres aprovados da mesma execução privada. */
  private void givenReadingsAndReviews(Instant observedAt) throws Exception {
    givenReadings(observedAt);
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(
            66L, "product:9@private-validation-v1"))
        .thenReturn(
            List.of(
                review(91L, "humanExperienceReview", "customer-agent", "APPROVED"),
                review(92L, "commercialIntegrityReview", "meta-ad-approver", "APPROVED")));
  }

  /** Configura duas leituras completas, distintas e posteriores aos critérios. */
  private void givenReadings(Instant observedAt) throws Exception {
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                66L, "product:9@private-validation-v1"))
        .thenReturn(
            List.of(
                reading(81L, "privateReading1", "PV-A1B2C3D4E5F6", observedAt),
                reading(82L, "privateReading2", "PV-1A2B3C4D5E6F", observedAt.plusSeconds(60))));
  }

  /** Monta uma leitura persistida com contagens e taxas recalculadas pelo backend. */
  private BusinessProcessActivityInstance reading(
      Long id, String activityId, String participant, Instant observedAt) throws Exception {
    BusinessProcessActivityDefinition definition = new BusinessProcessActivityDefinition();
    definition.setActivityId(activityId);
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setId(id);
    instance.setActivityDefinition(definition);
    instance.setSourceReference("product:9@private-validation-v1");
    instance.setStatus("COMPLETED");
    instance.setObjectiveAchieved(true);
    instance.setObjectiveEvidenceJson(
        json.writeValueAsString(
            Map.of(
                "evidenceReference",
                "private-session:" + id,
                "structuredEvidence",
                Map.ofEntries(
                    Map.entry("participantReference", participant),
                    Map.entry("consentConfirmed", true),
                    Map.entry("firstPartyEvidenceConfirmed", true),
                    Map.entry("criteriaPassed", true),
                    Map.entry("eligibleParticipants", 1),
                    Map.entry("experienceStarted", 1),
                    Map.entry("valueMoments", 1),
                    Map.entry("readyResultsUsedWithoutAssembly", 1),
                    Map.entry("prototypePreferredOverFree", 1),
                    Map.entry("checkoutStarted", 1),
                    Map.entry("observedAt", observedAt.toString()),
                    Map.entry("eventSource", "FIRST_PARTY_EVENTS"),
                    Map.entry("testMarker", "PRIVATE_PROTOTYPE"),
                    Map.entry("prototypeVersion", "private-v1"),
                    Map.entry(
                        "signals",
                        Map.of(
                            "EXPERIENCE_STARTED",
                            true,
                            "VALUE_MOMENT",
                            true,
                            "READY_RESULT_USED",
                            true,
                            "PREFERRED_OVER_FREE",
                            true,
                            "CHECKOUT_STARTED",
                            true))))));
    return instance;
  }

  /** Monta o parecer persistido de Psique ou Têmis. */
  private AgentTask review(Long id, String activityId, String agentKey, String decision)
      throws Exception {
    return review(id, activityId, agentKey, decision, Instant.now().minusSeconds(120));
  }

  /** Monta um parecer com instante controlado para provar a ordem temporal do gate. */
  private AgentTask review(
      Long id, String activityId, String agentKey, String decision, Instant deliveredAt)
      throws Exception {
    Agent agent = new Agent();
    agent.setAgentKey(agentKey);
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(agent);
    task.setProcessActivityId(activityId);
    task.setStatus("COMPLETED");
    task.setDeliveredAt(deliveredAt);
    Map<String, Boolean> checks =
        "humanExperienceReview".equals(activityId)
            ? Map.of(
                "sameProductAndVersion", true,
                "twoDistinctParticipants", true,
                "fiveSignalsPassedTwice", true,
                "firstPartyEvents", true,
                "lowEffortReadyResult", true,
                "desktopAndMobileUsable", true,
                "consentAndPrivacyPreserved", true,
                "noMaterialHarm", true)
            : Map.ofEntries(
                Map.entry("sameProductAndVersion", true),
                Map.entry("criteriaPredeclared", true),
                Map.entry("twoDistinctParticipants", true),
                Map.entry("fiveSignalsPassedTwice", true),
                Map.entry("firstPartyEvents", true),
                Map.entry("privateAndUnpublished", true),
                Map.entry("paymentDisabled", true),
                Map.entry("zeroMediaSpend", true),
                Map.entry("privacyPreserved", true));
    String checkField =
        "humanExperienceReview".equals(activityId)
            ? "privateExperienceChecks"
            : "privateValidationChecks";
    task.setResultJson(
        json.writeValueAsString(
            Map.of(
                "decision",
                decision,
                "evidence",
                List.of("Evidência persistida e vinculada ao produto privado"),
                checkField,
                checks)));
    return task;
  }

  /** Monta o produto privado com critérios, cena, entrega pronta e versão aceita. */
  private Product product() {
    Instant now = Instant.now();
    String validation =
        """
        {
          "purchaseMomentStatus":"WAITING_PRIVATE_READINGS",
          "privateValidationPlan":{
            "criteriaDeclaredAt":"%s",
            "sourceMaxAgeDays":30,
            "minimumIndependentReadings":2,
            "minimumEligibleParticipantsPerReading":1,
            "requiredSignals":[
              "EXPERIENCE_STARTED",
              "VALUE_MOMENT",
              "READY_RESULT_USED",
              "PREFERRED_OVER_FREE",
              "CHECKOUT_STARTED"
            ],
            "minimumExperienceStartRate":1,
            "minimumValueMomentRate":1,
            "minimumReadyResultUseRate":1,
            "minimumPrototypePreferenceRate":1,
            "minimumCheckoutStartRate":1,
            "strongestFreeAlternative":"Uso manual de IA genérica",
            "prototypeAdvantage":"Resultado pessoal pronto sem montagem",
            "purchaseScene":{
              "trigger":"Compromisso confirmado",
              "deadline":"Antes de sair",
              "costOfError":"Perder tempo e segurança",
              "budgetEvidence":"Compara orientação paga",
              "failedAttempt":"Tentou montar manualmente",
              "currentPaidBehavior":"Compra orientação especializada"
            },
            "humanValueDelivery":{
              "territories":["RECOGNITION","EFFORT_RELIEF"],
              "evidenceSourceIds":["source-1","source-2"],
              "evidencePathways":["LANGUAGE","PAID_BEHAVIOR"],
              "desiredTransformation":"Sentir segurança com menos esforço",
              "readyMadeOutcome":"Recomendação pronta",
              "minimumCustomerInput":"Contexto em linguagem comum",
              "automationBoundary":"A pessoa revisa o resultado",
              "requiresPromptEngineering":false,
              "requiresManualAssembly":false,
              "usableWithoutAiKnowledge":true,
              "customerStepsToValue":3,
              "timeToUsableResultMinutes":8
            }
          },
          "privatePrototypeAcceptance":{
            "status":"READY",
            "sourceQualityPassed":true,
            "sourceQualityEvaluatedAt":"%s",
            "acceptedAt":"%s",
            "privateAccessUrl":"https://private.local/prototype",
            "prototypeVersion":"private-v1",
            "instrumentationReference":"events:local-01",
            "sourceEvidenceReference":"source-snapshot:local-01",
            "privateAccessConfirmed":true,
            "desktopValidated":true,
            "mobileValidated":true,
            "paymentEnabled":false,
            "published":false,
            "mediaSpendBrl":0,
            "eventSource":"FIRST_PARTY_EVENTS",
            "testMarker":"PRIVATE_PROTOTYPE"
          }
        }
        """
            .formatted(now.minusSeconds(3600), now.minusSeconds(1800), now.minusSeconds(1200));
    return Product.builder()
        .id(9L)
        .commercialStatus("PLANNED")
        .automaticExecutionEnabled(true)
        .validationDefinitionVersion("PDE_PRIVATE_VALIDATION_V1")
        .validationDefinitionJson(validation)
        .pdeExperienceJson(
            "{\"experienceVersion\":\"private-validation-v1\",\"status\":\"PLANNED\"}")
        .build();
  }

  /** Monta a versão publicada do processo privado. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(66L);
    process.setProcessCode("pde-construction-approval");
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Monta a atividade humana de priorização final. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(609L);
    activity.setActivityId("finalPrioritization");
    activity.setOwnerName("Operador humano");
    return activity;
  }
}
