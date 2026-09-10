package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: reproduzir sucessor sobre produto legado sem perder contratos ou isolamento.
 */
class LearningCycleConstructionContextTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final LearningCycleConstructionContext resolver =
      new LearningCycleConstructionContext(cycles, chains, tasks, mapper);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Product product =
      Product.builder()
          .id(4L)
          .slug("vega-test")
          .validationDefinitionVersion("v1")
          .pdeExperienceJson("{\"experienceVersion\":\"historical-v7\"}")
          .publicUrl("https://historical.invalid")
          .build();
  private final Experiment experiment = Experiment.builder().id(92L).product(product).build();
  private List<AgentTask> approved;

  /**
   * Prepara aprovações sintéticas e uma referência histórica que jamais deve substituir o sucessor.
   */
  @BeforeEach
  void setup() {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    cycle.setPreviousCycleId(1L);
    cycle.setProductVersion("successor-v8");
    cycle.setStatus("OPEN");
    cycle.setCreatedAt(Instant.parse("2026-09-09T00:00:00Z"));
    cycle.setInheritedLearningJson(
        "{\"cycleId\":1,\"experimentId\":91,\"limitation\":\"Amostra pequena; sem causa comprovada\"}");
    cycle.setBriefJson("{\"hypothesis\":\"Melhorar primeiro resultado útil\"}");
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));
    var process = new BusinessProcessDefinition();
    process.setId(67L);
    process.setProcessCode("pde-commercial-plan-offer");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(new ArrayList<>(List.of(item)));
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    approved =
        new ArrayList<>(
            List.of(
                task(
                    362,
                    "productArchitecture",
                    "landing-generator",
                    "{\"decision\":\"APPROVE\",\"productArchitecture\":{\"privatePrototype\":{\"simpleInput\":\"Ocasião e peça\"}}}"),
                task(
                    361,
                    "economics",
                    "financial-agent",
                    "{\"contractVersion\":\"PDE_PRIVATE_ECONOMICS_V1\",\"decision\":\"APPROVE\",\"economics\":{\"commercialSpendAuthorized\":false},\"metrics\":{\"primary\":\"Uso\"}}"),
                task(
                    359,
                    "marketStrategy",
                    "experiment-strategist",
                    "{\"decision\":\"APPROVE\",\"marketStrategicContract\":{\"contractVersion\":\"MARKET_STRATEGY_V3\",\"status\":\"READY_FOR_PRIVATE_VALIDATION\",\"privateValidationPlan\":{}}}")));
    when(tasks
            .findByProcessDefinitionIdAndSourceReferenceAndCreatedAtGreaterThanEqualOrderByCreatedAtDescIdDesc(
                67L, "experiment:92", cycle.getCreatedAt()))
        .thenReturn(approved);
  }

  /** Cria uma entrega pertencente ao especialista e com ordem temporal verificável. */
  private AgentTask task(long id, String activity, String agent, String result) {
    var task = new AgentTask();
    task.setId(id);
    task.setStatus("COMPLETED");
    task.setProcessActivityId(activity);
    task.setAssignedAgent(Agent.builder().agentKey(agent).build());
    task.setResultJson(result);
    task.setDeliveredAt(cycle.getCreatedAt().plusSeconds(id));
    return task;
  }

  /**
   * Confirma que a versão e as provas vêm do ciclo sem modificar produto, checkout ou histórico.
   */
  @Test
  void buildsSuccessorWithExactApprovalsAndInheritedLearning() {
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.experienceVersion()).isEqualTo("successor-v8");
    assertThat(result.publicUrl()).isNull();
    assertThat(result.commercialCheckoutUrl()).isNull();
    assertThat(result.pdeContext().path("lineage").path("architectureTaskId").asLong())
        .isEqualTo(362);
    assertThat(result.pdeContext().path("inheritedLearning").path("experimentId").asLong())
        .isEqualTo(91);
    assertThat(result.pdeContext().path("cycleBrief").path("hypothesis").asText())
        .contains("primeiro resultado");
    assertThat(product.getPdeExperienceJson()).contains("historical-v7");
    verify(cycles, never()).save(any());
  }

  /**
   * Exporta o contrato produzido pelo backend para a validação real do consumidor na matriz local.
   */
  @Test
  void exportsCompleteContractForWorkerValidation() throws Exception {
    var fixture =
        mapper.readTree(getClass().getResourceAsStream("/learningcycle/successor-planning.json"));
    approved.get(0).setResultJson(fixture.path("architecture").toString());
    approved.get(1).setResultJson(fixture.path("economics").toString());
    approved.get(2).setResultJson(fixture.path("strategy").toString());
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.pdeContext()).isNotNull();
    assertThat(
            result
                .pdeContext()
                .path("harness")
                .path("privatePrototype")
                .path("maxValueTimeMinutes")
                .asInt())
        .isEqualTo(5);
    String output = System.getProperty("vega.context.output");
    if (output != null)
      java.nio.file.Files.writeString(
          java.nio.file.Path.of(output), mapper.writeValueAsString(Map.of("taskTarget", result)));
  }

  /** A ausência de uma aprovação não autoriza usar silenciosamente a versão comercial anterior. */
  @Test
  void missingApprovalKeepsTargetVersionButBlocksContext() {
    approved.remove(0);
    var result =
        resolver.resolve("experiment:92", experiment, "pde-construction-approval").orElseThrow();
    assertThat(result.experienceVersion()).isEqualTo("successor-v8");
    assertThat(result.pdeContext()).isNull();
  }

  /** Não aceita parecer de outro agente nem aprovação substituída por rejeição. */
  @Test
  void rejectsWrongAgentAndLatestRejectedContract() {
    approved.getFirst().getAssignedAgent().setAgentKey("other");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
    approved.getFirst().getAssignedAgent().setAgentKey("landing-generator");
    approved.getFirst().setResultJson("{\"decision\":\"ADJUST\"}");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
  }

  /**
   * Mudança posterior na economia exige arquitetura compatível, sem herdar autorização comercial.
   */
  @Test
  void rejectsStaleArchitectureAndCommercialSpend() {
    approved.get(1).setDeliveredAt(approved.getFirst().getDeliveredAt().plusSeconds(1));
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
    setup();
    approved
        .get(1)
        .setResultJson(
            "{\"contractVersion\":\"PDE_PRIVATE_ECONOMICS_V1\",\"decision\":\"APPROVE\",\"economics\":{\"commercialSpendAuthorized\":true}}");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
  }

  /**
   * Uma nova tentativa bloqueada ou em execução invalida o reaproveitamento da aprovação anterior.
   */
  @Test
  void latestAttemptMustBeCompletedEvenWhenOlderApprovalExists() {
    var previous = approved.getFirst();
    var replacement =
        task(400, "productArchitecture", "landing-generator", previous.getResultJson());
    approved.addFirst(replacement);
    for (var state : List.of("BLOCKED", "IN_PROGRESS", "PENDING", "CANCELLED")) {
      replacement.setStatus(state);
      assertThat(
              resolver
                  .resolve("experiment:92", experiment, "pde-construction-approval")
                  .orElseThrow()
                  .pdeContext())
          .as(state)
          .isNull();
    }
    replacement.setStatus("COMPLETED");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext()
                .path("lineage")
                .path("architectureTaskId")
                .asLong())
        .isEqualTo(400);
  }

  /** Não expõe dados de outro produto ou da construção em processos comerciais diferentes. */
  @Test
  void isolatesProductProcessAndClosedCycle() {
    assertThat(resolver.resolve("experiment:92", experiment, "landing-page-generation")).isEmpty();
    cycle.setStatus("ADJUSTED");
    assertThat(
            resolver
                .resolve("experiment:92", experiment, "pde-construction-approval")
                .orElseThrow()
                .pdeContext())
        .isNull();
    cycle.setProductId(10L);
    assertThatThrownBy(
            () -> resolver.resolve("experiment:92", experiment, "pde-construction-approval"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("produtos diferentes");
  }
}
