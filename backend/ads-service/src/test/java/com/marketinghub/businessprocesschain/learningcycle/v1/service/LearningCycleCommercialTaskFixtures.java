package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: simular o ledger dos revisores sem chamar modelo ou gerar cobrança. */
@TestConfiguration
@Profile("learning-cycles-fixture")
@Import(LearningCycleCommercialTaskFixtures.Controller.class)
public class LearningCycleCommercialTaskFixtures {
  private static final Map<Long, AgentTask> TASKS = new ConcurrentHashMap<>();

  /** Usa a prontidão humana real; a fixture nunca executa liberação de campanha. */
  @Bean
  com.marketinghub.businessprocess.execution.service.humanactivity
          .HumanProductProcessActivityExecutor
      commercialHumanGate(
          com.marketinghub.repository.jpa.experiment.ExperimentRepository experiments,
          com.marketinghub.repository.jpa.experiment.ExperimentRunRepository runs,
          com.marketinghub.repository.jpa.planning.CommercialPlanRepository plans,
          com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository cycles,
          com.marketinghub.experiment.service.ExperimentReadinessService readiness) {
    var handler =
        new com.marketinghub.experiment.service.PdeCommercialActivationHumanActivityHandler(
            experiments,
            runs,
            plans,
            cycles,
            readiness,
            mock(com.marketinghub.experiment.service.ExperimentService.class),
            mock(com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository.class),
            mock(
                com.marketinghub.facebookads.resumption.service.FacebookCampaignResumptionService
                    .class));
    var gate =
        mock(
            com.marketinghub.businessprocess.execution.service.humanactivity
                .HumanProductProcessActivityExecutor.class);
    when(gate.supports(any(), any()))
        .thenAnswer(call -> handler.supports(call.getArgument(0), call.getArgument(1)));
    when(gate.readiness(any(), any(), any(), anyString()))
        .thenAnswer(
            call ->
                handler.readiness(
                    call.getArgument(0),
                    call.getArgument(1),
                    call.getArgument(2),
                    call.getArgument(3)));
    when(gate.execute(any(), any(), any(), anyString(), any()))
        .thenThrow(
            new IllegalStateException("Liberação comercial externa não pertence à fixture."));
    return gate;
  }

  /** Descarta apenas tarefas sintéticas durante o reset transacional da matriz. */
  static void reset() {
    TASKS.clear();
  }

  /** Filtra tarefas simuladas pelo mesmo contrato de identidade usado pela projeção real. */
  @Bean
  AgentTaskRepository agentTasks() {
    var repository = mock(AgentTaskRepository.class);
    when(repository.findBySourceReferenceAndProcessDefinitionProcessCodeOrderByCreatedAtAscIdAsc(
            anyString(), anyString()))
        .thenAnswer(
            call ->
                TASKS.values().stream()
                    .filter(
                        task ->
                            task.getSourceReference().equals(call.getArgument(0))
                                && task.getProcessDefinition()
                                    .getProcessCode()
                                    .equals(call.getArgument(1)))
                    .sorted(Comparator.comparing(AgentTask::getId))
                    .toList());
    return repository;
  }

  /**
   * Registra a solicitação do backend e devolve recibo pendente sem considerar o objetivo provado.
   */
  @Bean
  AgentTaskService taskService(BusinessProcessDefinitionRepository processes, ObjectMapper json) {
    var service = mock(AgentTaskService.class);
    when(service.retryBlockedByHumanOrRefreshPending(any(CreateAgentTaskRequest.class)))
        .thenAnswer(
            call -> {
              CreateAgentTaskRequest request = call.getArgument(0);
              var process = processes.findById(request.processDefinitionId()).orElseThrow();
              if (!"pde-commercial-homologation-activation".equals(process.getProcessCode())
                  || !Set.of("experiment:91001", "experiment:91002")
                      .contains(request.sourceReference()))
                throw new IllegalArgumentException(
                    "Solicitação fora do revisor comercial simulado.");
              var task = new AgentTask();
              task.setId(95000L + TASKS.size());
              task.setAssignedAgent(
                  Agent.builder()
                      .id(task.getId())
                      .agentKey(request.assignedAgentKey())
                      .nickname("Revisor sintético")
                      .build());
              task.setSourceReference(request.sourceReference());
              task.setProcessDefinition(process);
              task.setProcessActivityId(request.processActivityId());
              task.setTitle("Revisão local segregada");
              task.setStatus("PENDING");
              task.setCreatedAt(Instant.now());
              task.setUpdatedAt(Instant.now());
              task.setCostEstimationStatus("NOT_REPORTED");
              TASKS.put(task.getId(), task);
              return json.convertValue(
                  Map.of(
                      "id",
                      task.getId(),
                      "status",
                      task.getStatus(),
                      "sourceReference",
                      task.getSourceReference()),
                  AgentTaskResponse.class);
            });
    return service;
  }

  /**
   * Responsabilidade: receber somente resultados externos sintéticos para a matriz de homologação.
   */
  @RestController
  @Profile("learning-cycles-fixture")
  static class Controller {
    /** Expõe recibos locais para verificar deduplicação, correlação e estados intermediários. */
    @GetMapping("/fixture/commercial-tasks")
    List<Map<String, Object>> tasks() {
      return TASKS.values().stream()
          .sorted(Comparator.comparing(AgentTask::getId))
          .map(
              task ->
                  Map.<String, Object>of(
                      "id",
                      task.getId(),
                      "status",
                      task.getStatus(),
                      "sourceReference",
                      task.getSourceReference(),
                      "activityId",
                      task.getProcessActivityId(),
                      "simulated",
                      true))
          .toList();
    }

    /** Aplica resultado controlado do revisor; a aprovação final da campanha permanece humana. */
    @PostMapping("/fixture/commercial-tasks/{id}/complete")
    Map<String, Object> complete(@PathVariable long id) {
      var task = Objects.requireNonNull(TASKS.get(id));
      task.setStatus("COMPLETED");
      task.setEstimatedCostUsd(new BigDecimal("0.12"));
      task.setCostEstimationStatus("ESTIMATED");
      task.setResultJson("{\"fixture\":true,\"review\":\"Jornada sintética validada\"}");
      task.setDeliveredAt(Instant.now());
      task.setUpdatedAt(Instant.now());
      return Map.of("id", id, "simulated", true, "status", task.getStatus());
    }
  }
}
