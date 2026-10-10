package com.marketinghub.financialplan.v1.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.service.ExperimentAgentTaskTargetContextProvider;
import com.marketinghub.hypothesis.Hypothesis;
import com.marketinghub.openai.service.OpenAiPricingService;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocessresource.BusinessProcessExecutionResourceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.bind.annotation.*;

/** Responsabilidade: testar a reserva HTTP nativa com contexto e revisão financeira reais. */
@TestConfiguration
@Import(FinancialPlanNativePlanningFixture.TaskController.class)
public class FinancialPlanNativePlanningFixture {
  private final Map<Long, AgentTask> tasks = new ConcurrentHashMap<>();

  /** Monta o alvo real com identidades sintéticas, sem substituir o serviço financeiro. */
  private ExperimentAgentTaskTargetContextProvider planningTarget(
      ProductRepository products,
      CommercialPlanRepository plans,
      ObjectMapper json,
      FinancialPlanService finances) {
    var experiments = mock(ExperimentRepository.class);
    when(experiments.findById(anyLong()))
        .thenAnswer(
            invocation -> {
              Long id = invocation.getArgument(0);
              return products
                  .findById(id)
                  .map(
                      product ->
                          Experiment.builder()
                              .id(id)
                              .product(product)
                              .status(ExperimentStatus.PLANNED)
                              .hypothesisRef(
                                  Hypothesis.builder()
                                      .id(
                                          UUID.nameUUIDFromBytes(
                                              id.toString()
                                                  .getBytes(
                                                      java.nio.charset.StandardCharsets.UTF_8)))
                                      .build())
                              .build());
            });
    when(plans.findByExperimentReference(anyLong()))
        .thenAnswer(
            invocation -> {
              Long id = invocation.getArgument(0);
              var plan = plans.findById(id).orElseThrow();
              plan.setExperiment(experiments.findById(id).orElseThrow());
              return List.of(plan);
            });
    var target =
        new ExperimentAgentTaskTargetContextProvider(
            experiments,
            products,
            json,
            null,
            plans,
            new com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver(json));
    org.springframework.test.util.ReflectionTestUtils.setField(target, "financialPlans", finances);
    return target;
  }

  /** Simula somente a fila de agentes; a reserva e a montagem da resposta são produtivas. */
  @Bean
  AgentTaskService nativeTasks(
      ProductRepository products,
      CommercialPlanRepository plans,
      ObjectMapper json,
      FinancialPlanService finances) {
    var target = planningTarget(products, plans, json, finances);
    var repository = mock(AgentTaskRepository.class);
    when(repository.findByAssignedAgentAgentKeyAndTaskKindAndStatusOrderByCreatedAtAscIdAsc(
            anyString(), anyString(), anyString()))
        .thenAnswer(
            invocation ->
                tasks.values().stream()
                    .filter(
                        task ->
                            task.getAssignedAgent().getAgentKey().equals(invocation.getArgument(0)))
                    .filter(task -> task.getTaskKind().equals(invocation.getArgument(1)))
                    .filter(task -> task.getStatus().equals(invocation.getArgument(2)))
                    .sorted(Comparator.comparing(AgentTask::getId))
                    .toList());
    when(repository.findById(anyLong()))
        .thenAnswer(invocation -> Optional.ofNullable(tasks.get(invocation.getArgument(0))));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var agents = mock(AgentRepository.class);
    when(agents.findByAgentKey("experiment-strategist")).thenReturn(Optional.of(agent()));
    return new AgentTaskService(
        repository,
        mock(BusinessProcessActivityInstanceRepository.class),
        agents,
        mock(BusinessProcessDefinitionRepository.class),
        mock(BusinessProcessActivityDefinitionRepository.class),
        mock(BusinessProcessExecutionResourceRepository.class),
        json,
        mock(OpenAiPricingService.class),
        MarketStrategicContextProvider.empty(),
        target);
  }

  /** Expõe o endpoint canônico real; a fixture não executa modelos nem callbacks externos. */
  @Bean
  InternalAgentTaskExecutionController nativePending(AgentTaskService service) {
    return new InternalAgentTaskExecutionController(
        service, mock(AgentTaskVisualEvidenceService.class));
  }

  /** Identifica o executor sintético de Atena sem alterar agentes publicados. */
  private static Agent agent() {
    return Agent.builder().id(95190L).agentKey("experiment-strategist").name("Atena local").build();
  }

  /** Prepara somente uma tarefa sintética, mantendo custo e resultado desconhecidos. */
  void prepare(long id) {
    if (id != 95121 && id != 95122) throw new IllegalArgumentException("Identidade não sintética");
    var process = new BusinessProcessDefinition();
    process.setId(95191L);
    process.setProcessCode("pde-commercial-plan-offer");
    process.setVersionNumber(12);
    process.setStatus("PUBLISHED");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"marketStrategy\",\"type\":\"TASK\",\"owner\":\"Atena\"}],\"edges\":[]}");
    var task = new AgentTask();
    task.setId(id + 100);
    task.setAssignedAgent(agent());
    task.setProcessDefinition(process);
    task.setProcessActivityId("marketStrategy");
    task.setProcessActivityName("Estratégia privada");
    task.setTaskKind("WORK");
    task.setStatus("PENDING");
    task.setSourceReference("experiment:" + id);
    task.setCreatedAt(Instant.now());
    task.setUpdatedAt(task.getCreatedAt());
    tasks.put(task.getId(), task);
  }

  /** Disponibiliza a preparação da fila somente no perfil local de homologação. */
  @RestController
  @RequestMapping("/fixture/native-planning")
  static class TaskController {
    private final FinancialPlanNativePlanningFixture fixture;

    /** Recebe o catálogo exclusivamente sintético da topologia isolada. */
    TaskController(FinancialPlanNativePlanningFixture fixture) {
      this.fixture = fixture;
    }

    /** Enfileira uma tarefa sem inferência, autorização ou aprovação comercial. */
    @PostMapping("/{id}")
    public void prepare(@PathVariable long id) {
      fixture.prepare(id);
    }
  }
}
