package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleImplementedInputContext;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.agentvalidation.PdeImplementedInputPlanningReadinessProvider;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Responsabilidade: comprovar a retomada pelo serviço real da tela antes de repetir Dédalo. */
class PdeImplementedInputPlanningHandoffTest {
  /** Reabre Atena, revalida Plutus e libera Dédalo, preservando todas as tentativas anteriores. */
  @ParameterizedTest
  @CsvSource({
    "91004,metodo-musa-7-dias,musa-pde-entry-v14-primeiro-ajuste-aplicavel",
    "92010,pde-planejado-36,mira-private-candidate-v3"
  })
  void resumesAtOriginAndKeepsDependentReviewsInOrder(Long productId, String slug, String version)
      throws Exception {
    var json = new ObjectMapper();
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var definitions = mock(BusinessProcessActivityDefinitionRepository.class);
    var tasks = mock(AgentTaskRepository.class);
    var products = mock(ProductRepository.class);
    var experiments = mock(ExperimentRepository.class);
    var cycles = mock(LearningSalesCycleRepository.class);
    var agentTasks = mock(AgentTaskService.class);
    var input = new LearningCycleImplementedInputContext(products, json);
    var gate = new PdeImplementedInputPlanningReadinessProvider(cycles, input, tasks, json);
    var service =
        new BusinessProcessActivityExecutionService(
            processes,
            definitions,
            tasks,
            mock(AgentTaskActivityCoverageRepository.class),
            mock(BusinessProcessActivityInstanceRepository.class),
            mock(CommercialPlanRepository.class),
            null,
            products,
            experiments,
            agentTasks,
            json,
            List.of(),
            List.of(gate));
    var product =
        Product.builder()
            .id(productId)
            .slug(slug)
            .internalName("Produto sintético")
            .name("Produto sintético")
            .automaticExecutionEnabled(true)
            .build();
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 100L);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 200L);
    cycle.setProductVersion(version);
    cycle.setStage("PLANNING");
    cycle.setStatus("OPEN");
    cycle.setCreatedAt(Instant.parse("2026-10-10T00:00:00Z"));
    String reference = "experiment:" + cycle.getExperimentId();
    var experiment = new Experiment();
    experiment.setId(cycle.getExperimentId());
    experiment.setProduct(product);
    var process = new BusinessProcessDefinition();
    process.setId(91116L);
    process.setProcessCode("pde-commercial-plan-offer");
    process.setName("Planejamento sintético");
    process.setVersionNumber(12);
    process.setStatus("PUBLISHED");
    process.setDiagramJson(
        """
        {"nodes":[
          {"id":"marketStrategy","type":"TASK","responsibleAgentKeys":["experiment-strategist"]},
          {"id":"economics","type":"TASK","responsibleAgentKeys":["financial-agent"]},
          {"id":"productArchitecture","type":"TASK","responsibleAgentKeys":["landing-generator"]}],
         "flows":[{"from":"marketStrategy","to":"economics"},
                  {"from":"economics","to":"productArchitecture"}]}
        """);
    var activityList = new ArrayList<BusinessProcessActivityDefinition>();
    var history = new ArrayList<AgentTask>();
    String[] codes = {"marketStrategy", "economics", "productArchitecture"};
    String[] owners = {"experiment-strategist", "financial-agent", "landing-generator"};
    for (int i = 0; i < codes.length; i++) {
      var activity = new BusinessProcessActivityDefinition();
      activity.setId(91200L + i);
      activity.setProcessDefinition(process);
      activity.setActivityId(codes[i]);
      activity.setName(codes[i]);
      activity.setDefinitionJson("{\"responsibleAgentKeys\":[\"" + owners[i] + "\"]}");
      activityList.add(activity);
      when(definitions.findByProcessDefinitionIdAndActivityId(process.getId(), codes[i]))
          .thenReturn(Optional.of(activity));
      history.add(
          task(
              97001L + i,
              process,
              codes[i],
              owners[i],
              reference,
              i == 2 ? "BLOCKED" : "COMPLETED",
              i == 0 ? strategy(json, "Quatro escolhas antigas.") : "{\"decision\":\"APPROVE\"}"));
    }
    when(processes.findById(process.getId())).thenReturn(Optional.of(process));
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(productId))
        .thenReturn(List.of(experiment));
    when(cycles.findByExperimentId(cycle.getExperimentId())).thenReturn(Optional.of(cycle));
    when(definitions.findAllByProcessDefinitionIdOrderByIdAsc(process.getId()))
        .thenReturn(activityList);
    when(tasks.findBySourceReferenceAndProcessDefinitionProcessCodeOrderByCreatedAtAscIdAsc(
            reference, process.getProcessCode()))
        .thenAnswer(ignored -> List.copyOf(history));
    when(tasks.findFunctionalSnapshotsByProcessSince(
            process.getId(), reference, cycle.getCreatedAt()))
        .thenAnswer(
            ignored ->
                history.stream()
                    .map(
                        t ->
                            new AgentTaskFunctionalSnapshot(
                                t.getId(),
                                process.getId(),
                                process.getProcessCode(),
                                t.getProcessActivityId(),
                                t.getAssignedAgent().getAgentKey(),
                                t.getStatus(),
                                t.getCreatedAt(),
                                t.getDeliveredAt(),
                                t.getResultJson()))
                    .toList());
    when(agentTasks.retryBlockedByHumanOrRefreshPending(any(), eq(true)))
        .thenReturn(mock(AgentTaskResponse.class));

    var before = service.productProcessExecutions(process.getId(), productId);
    assertThat(before.activities().getFirst().objectiveAchieved()).isFalse();
    assertThat(before.currentActivityId())
        .as(
            "Referência %s; estados %s",
            before.currentExecutionReference(),
            before.activities().stream()
                .map(a -> a.activityId() + ":" + a.operationalState())
                .toList())
        .isEqualTo("marketStrategy");
    assertThat(
            service
                .requestProductActivityExecution(process.getId(), productId, "marketStrategy")
                .tasks())
        .hasSize(1);
    verify(agentTasks)
        .retryBlockedByHumanOrRefreshPending(
            argThat(
                r ->
                    "marketStrategy".equals(r.processActivityId())
                        && reference.equals(r.sourceReference())),
            eq(true));

    history.add(
        task(
            97004L,
            process,
            "marketStrategy",
            owners[0],
            reference,
            "COMPLETED",
            strategy(
                json, input.resolve(cycle).orElseThrow().path("minimumCustomerInput").asText())));
    assertThat(service.productProcessExecutions(process.getId(), productId).currentActivityId())
        .isEqualTo("economics");
    assertThat(
            service
                .requestProductActivityExecution(process.getId(), productId, "economics")
                .tasks())
        .hasSize(1);
    verify(agentTasks)
        .retryBlockedByHumanOrRefreshPending(
            argThat(r -> "economics".equals(r.processActivityId())), eq(true));

    history.add(
        task(
            97005L,
            process,
            "economics",
            owners[1],
            reference,
            "COMPLETED",
            "{\"decision\":\"APPROVE\"}"));
    var ready = service.productProcessExecutions(process.getId(), productId);
    assertThat(ready.currentActivityId()).isEqualTo("productArchitecture");
    assertThat(ready.activities().get(2).executionRequestAvailable()).isTrue();
    assertThat(history.getFirst().getResultJson()).contains("Quatro escolhas antigas");
    assertThat(history.get(2).getStatus()).isEqualTo("BLOCKED");
  }

  /** Cria tentativa local auditável sem acionar modelo, endpoint externo ou cobrança. */
  private AgentTask task(
      Long id,
      BusinessProcessDefinition process,
      String activity,
      String owner,
      String reference,
      String status,
      String result) {
    var value = new AgentTask();
    value.setId(id);
    value.setProcessDefinition(process);
    value.setProcessActivityId(activity);
    value.setProcessActivityName(activity);
    value.setAssignedAgent(Agent.builder().id(id).agentKey(owner).nickname(owner).build());
    value.setTitle(activity);
    value.setSourceReference(reference);
    value.setStatus(status);
    value.setResultJson(result);
    value.setCreatedAt(Instant.parse("2026-10-10T01:00:00Z").plusSeconds(id));
    value.setUpdatedAt(value.getCreatedAt());
    value.setDeliveredAt("COMPLETED".equals(status) ? value.getCreatedAt().plusSeconds(60) : null);
    return value;
  }

  /** Monta somente o contrato mínimo de estratégia necessário à validação determinística. */
  private String strategy(ObjectMapper json, String minimum) {
    var value = json.createObjectNode().put("decision", "APPROVE");
    value
        .putObject("marketStrategicContract")
        .putObject("agentValidationPlan")
        .putObject("customerValueDelivery")
        .put("minimumCustomerInput", minimum);
    return value.toString();
  }
}
