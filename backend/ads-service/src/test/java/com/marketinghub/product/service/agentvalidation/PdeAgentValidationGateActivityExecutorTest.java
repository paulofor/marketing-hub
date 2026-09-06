package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: comprovar o gate v7 sem fabricar leitura, venda ou autorização de mídia. */
class PdeAgentValidationGateActivityExecutorTest {
  private static final Instant NOW = Instant.parse("2026-09-06T12:00:00Z");
  private static final String SOURCE = "product:10@agent-validation-v1";
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProductProcessPeriodService periods = mock(ProductProcessPeriodService.class);
  private final ObjectMapper json = new ObjectMapper();
  private PdeAgentValidationGateActivityExecutor executor;
  private BusinessProcessDefinition process;
  private BusinessProcessActivityDefinition gate;
  private Product product;
  private List<AgentTask> completedTasks;

  /** Monta uma ocorrência completa e cronologicamente válida da mesma versão de Mira. */
  @BeforeEach
  void setUp() {
    executor =
        new PdeAgentValidationGateActivityExecutor(
            tasks, instances, products, periods, json, Clock.fixed(NOW, ZoneOffset.UTC));
    process = new BusinessProcessDefinition();
    process.setId(70L);
    process.setProcessCode("pde-construction-approval");
    process.setVersionNumber(7);
    process.setStatus("PUBLISHED");
    gate = new BusinessProcessActivityDefinition();
    gate.setId(710L);
    gate.setProcessDefinition(process);
    gate.setActivityId("agentValidationGate");
    product =
        Product.builder()
            .id(10L)
            .slug("orientacao-digital-rotina-pele-madura")
            .internalName("Mira")
            .commercialStatus("PLANNED")
            .automaticExecutionEnabled(true)
            .validationDefinitionVersion("PDE_AGENT_VALIDATION_V1")
            .validationDefinitionJson(validationContract())
            .pdeExperienceJson(
                "{\"experienceVersion\":\"private-validation-v1\",\"status\":\"AGENT_VALIDATION_READY\"}")
            .build();
    completedTasks = new ArrayList<>();
    completedTasks.add(
        task(
            100L,
            "technicalHomologation",
            "customer-agent",
            "DETERMINISTIC",
            "pde-agent-validation-harness-v1",
            technicalResult(),
            NOW.minusSeconds(500)));
    completedTasks.add(
        task(
            101L,
            "psiqueAdherent",
            "customer-agent",
            "MODEL",
            "gpt-5.6-sol",
            psiqueResult("ADHERENT"),
            NOW.minusSeconds(400)));
    completedTasks.add(
        task(
            102L,
            "psiqueRecovery",
            "customer-agent",
            "MODEL",
            "gpt-5.6-sol",
            psiqueResult("RECOVERY"),
            NOW.minusSeconds(350)));
    completedTasks.add(
        task(
            103L,
            "psiqueSafety",
            "customer-agent",
            "MODEL",
            "gpt-5.6-sol",
            psiqueResult("SAFETY"),
            NOW.minusSeconds(300)));
    completedTasks.add(
        task(
            104L,
            "commercialIntegrityReview",
            "meta-ad-approver",
            "MODEL",
            "gpt-5.6-sol",
            temisResult(),
            NOW.minusSeconds(100)));
    when(tasks.findByProcessDefinitionIdAndSourceReferenceOrderByCreatedAtAscIdAsc(70L, SOURCE))
        .thenReturn(completedTasks);
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            710L, SOURCE))
        .thenReturn(Optional.empty());
    when(instances.saveAndFlush(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Libera somente comunicação em STOP e preserva mercado, pagamento e mídia como pendentes. */
  @Test
  void approvesCompleteAgentValidationWithoutCommercialSideEffects() throws Exception {
    var readiness = executor.readiness(process, gate, product, SOURCE);
    var result = executor.execute(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.requirements()).allMatch(requirement -> requirement.satisfied());
    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    assertThat(product.getCommercialStatus()).isEqualTo("COMUNICACAO_E_JORNADA");
    assertThat(product.getAutomaticExecutionEnabled()).isFalse();
    assertThat(product.getValidationDefinitionVersion()).isEqualTo("PDE_AGENT_VALIDATED_V1");
    var validation = json.readTree(product.getValidationDefinitionJson());
    assertThat(validation.path("purchaseMomentStatus").asText())
        .isEqualTo("WAITING_MARKET_VALIDATION");
    assertThat(validation.path("finalCommercialPrioritizationEligible").asBoolean()).isFalse();
    assertThat(validation.path("communicationPreparationEligible").asBoolean()).isTrue();
    assertThat(validation.path("agentValidation").path("humanEvidenceClaimed").asBoolean())
        .isFalse();
    verify(products).save(product);
    verify(periods).recordTransition(product, "PLANNED");
    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getObjectiveEvidenceJson())
        .contains("PDE_AGENT_VALIDATION_GATE_V1")
        .contains("\"publicationAuthorized\":false")
        .contains("\"campaignAuthorized\":false")
        .contains("\"mediaSpendAuthorizedBrl\":0")
        .contains("\"humanEvidenceClaimed\":false");
  }

  /** Mantém o gate fechado quando falta um dos três cenários independentes. */
  @Test
  void blocksWhenOnePsiqueScenarioIsMissing() {
    completedTasks.removeIf(task -> "psiqueRecovery".equals(task.getProcessActivityId()));

    var readiness = executor.readiness(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("RECOVERY");
    assertThat(readiness.requirements())
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("PSIQUE_SCENARIOS");
              assertThat(requirement.satisfied()).isFalse();
            });
    verify(products, never()).save(any());
  }

  /** Rejeita parecer que tenta converter uma simulação em evidência humana. */
  @Test
  void blocksForgedHumanEvidenceClaim() throws Exception {
    AgentTask psique =
        completedTasks.stream()
            .filter(task -> "psiqueAdherent".equals(task.getProcessActivityId()))
            .findFirst()
            .orElseThrow();
    var forged =
        (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(psique.getResultJson());
    forged.put("humanEvidenceClaimed", true);
    psique.setResultJson(forged.toString());

    var readiness = executor.readiness(process, gate, product, SOURCE);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("ADHERENT");
    verify(products, never()).save(any());
  }

  /** Impede que uma versão futura reutilize silenciosamente o executor e o contrato do v7. */
  @Test
  void supportsOnlyPublishedVersionSevenContract() {
    assertThat(executor.supports(process, gate)).isTrue();

    process.setVersionNumber(8);

    assertThat(executor.supports(process, gate)).isFalse();
  }

  /** Monta a predeclaração completa e a aceitação histórica imutável do protótipo. */
  private String validationContract() {
    return """
        {
          "purchaseMomentStatus":"WAITING_MARKET_VALIDATION",
          "privatePrototypeAcceptance":{
            "status":"READY",
            "privateAccessUrl":"https://v7.clubemusa.com.br/mira-private",
            "prototypeVersion":"mira-private-v1"
          },
          "agentValidationPlan":{
            "contractVersion":"PDE_AGENT_VALIDATION_V1",
            "sourceReference":"product:10@agent-validation-v1",
            "trafficClass":"AGENT_VALIDATION",
            "internalMarker":"mh_internal_test",
            "requiredScenarios":["ADHERENT","RECOVERY","SAFETY"],
            "requiredDevices":["DESKTOP_1440","IPHONE_15_PRO","PIXEL_7"],
            "maxReadyResultSeconds":600,
            "humanEvidenceClaimed":false,
            "commercialEvidenceClaimed":false,
            "paymentEnabled":false,
            "publicationAuthorized":false,
            "campaignAuthorized":false,
            "mediaSpendAuthorizedBrl":0,
            "status":"READY"
          }
        }
        """;
  }

  /** Produz o resultado integral do harness nos três dispositivos e cenários. */
  private String technicalResult() {
    return """
        {
          "contractVersion":"PDE_AGENT_TECHNICAL_HOMOLOGATION_V1",
          "mode":"TECHNICAL",
          "decision":"APPROVED",
          "sourceReference":"product:10@agent-validation-v1",
          "productId":10,
          "productSlug":"orientacao-digital-rotina-pele-madura",
          "publicUrl":"https://v7.clubemusa.com.br/mira-private",
          "prototypeVersion":"mira-private-v1",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "humanEvidenceClaimed":false,
          "commercialEvidenceClaimed":false,
          "checks":{
            "sameVersion":true,"desktopAndMobile":true,"happyResultWithinTenMinutes":true,
            "recoveryPreserved":true,"safetyBlocked":true,"accessibilityBasic":true,
            "responsiveLayout":true,"privacyPreserved":true,"internalTrafficSegregated":true,
            "paymentDisabled":true,"publicationDisabled":true,"campaignDisabled":true,
            "zeroMediaSpend":true
          },
          "devices":[
            {"deviceProfile":"DESKTOP_1440","status":"PASS"},
            {"deviceProfile":"IPHONE_15_PRO","status":"PASS"},
            {"deviceProfile":"PIXEL_7","status":"PASS"}
          ],
          "scenarios":[
            {"scenarioCode":"ADHERENT","status":"PASS","resultReadySeconds":35},
            {"scenarioCode":"ADHERENT","status":"PASS","resultReadySeconds":40},
            {"scenarioCode":"ADHERENT","status":"PASS","resultReadySeconds":45},
            {"scenarioCode":"RECOVERY","status":"PASS","resultReadySeconds":70},
            {"scenarioCode":"SAFETY","status":"PASS","resultReadySeconds":0}
          ],
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0}
        }
        """;
  }

  /** Produz um parecer sintético completo para o cenário informado. */
  private String psiqueResult(String scenario) {
    return """
        {
          "contractVersion":"PDE_PSIQUE_AGENT_SCENARIO_V1",
          "decision":"APPROVED",
          "scenarioCode":"%s",
          "sourceReference":"product:10@agent-validation-v1",
          "productId":10,
          "productSlug":"orientacao-digital-rotina-pele-madura",
          "prototypeVersion":"mira-private-v1",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "syntheticEvaluation":true,
          "humanEvidenceClaimed":false,
          "commercialEvidenceClaimed":false,
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0},
          "experienceAssessment":{"evidenceBoundary":"Simulação explícita limitada ao harness."},
          "checks":{
            "sameProductAndVersion":true,"isolatedFreshSession":true,
            "functionalOutcomeMatchesScenario":true,"lowEffortNoPrompting":true,
            "accessibilityAndResponsive":true,"privacyPreserved":true,
            "internalTrafficSegregated":true,"safeLimits":true,"noExternalSideEffects":true
          },
          "visualAudit":{"evidenceIds":[901]},
          "evidence":["Evidência sintética persistida"],
          "requiredChanges":[],
          "rootCause":"O mecanismo observado sustenta o resultado do cenário."
        }
        """
        .formatted(scenario);
  }

  /** Produz a auditoria independente final com todos os gates verdadeiros. */
  private String temisResult() {
    return """
        {
          "contractVersion":"PDE_TEMIS_AGENT_VALIDATION_V1",
          "decision":"APPROVED",
          "commercialRationale":"A validação está segregada e não afirma resposta humana.",
          "rootCause":"Os contratos preservam verdade, privacidade e efeitos externos nulos.",
          "sourceReference":"product:10@agent-validation-v1",
          "productId":10,
          "productSlug":"orientacao-digital-rotina-pele-madura",
          "prototypeVersion":"mira-private-v1",
          "trafficClass":"AGENT_VALIDATION",
          "internalMarker":"mh_internal_test",
          "humanEvidenceClaimed":false,
          "commercialEvidenceClaimed":false,
          "sideEffects":{"paymentEnabled":false,"published":false,"campaignCreated":false,"mediaSpendBrl":0},
          "agentValidationChecks":{
            "sameProductAndVersion":true,"criteriaPredeclared":true,"technicalHarnessPassed":true,
            "threeScenarioReviewsApproved":true,"syntheticEvidenceLabeled":true,
            "internalTrafficSegregated":true,"privacyPreserved":true,"paymentDisabled":true,
            "publicationDisabled":true,"campaignDisabled":true,"zeroMediaSpend":true,
            "noHumanOrCommercialClaim":true,"strategyFidelity":true
          },
          "evidence":["harness","aderente","recuperação","segurança"],
          "requiredChanges":[]
        }
        """;
  }

  /** Monta uma tarefa concluída com auditoria e custo persistidos. */
  private AgentTask task(
      long id,
      String activityId,
      String agentKey,
      String executionMode,
      String model,
      String result,
      Instant deliveredAt) {
    Agent agent = new Agent();
    agent.setAgentKey(agentKey);
    AgentTask task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(agent);
    task.setProcessDefinition(process);
    task.setProcessActivityId(activityId);
    task.setSourceReference(SOURCE);
    task.setStatus("COMPLETED");
    task.setExecutionMode(executionMode);
    task.setExecutionModelCode(model);
    task.setResultJson(result);
    task.setEstimatedCostUsd(BigDecimal.ZERO.setScale(8));
    task.setCreatedAt(deliveredAt.minusSeconds(20));
    task.setDeliveredAt(deliveredAt);
    return task;
  }
}
