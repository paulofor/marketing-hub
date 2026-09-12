package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: comprovar resolução de formatos, dispensa explícita e isolamento do contrato.
 */
class CommunicationFormatsActivityExecutorTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ObjectMapper json = new ObjectMapper();
  private final IrisCommunicationMaterializationContextProvider context =
      mock(IrisCommunicationMaterializationContextProvider.class);
  private final CommunicationFormatsActivityExecutor executor =
      new CommunicationFormatsActivityExecutor(
          tasks, instances, definitions, experiments, context, json);
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final BusinessProcessActivityDefinition route = new BusinessProcessActivityDefinition();
  private final BusinessProcessActivityDefinition audiovisual =
      new BusinessProcessActivityDefinition();
  private final Product product = Product.builder().id(91004L).build();
  private final AgentTask task = new AgentTask();

  /** Prepara contratos sintéticos sem checkout, publicação, IA ou qualquer efeito externo. */
  @BeforeEach
  void setup() throws Exception {
    process.setId(91064L);
    process.setProcessCode("creative-production-approval");
    route.setId(1L);
    route.setActivityId("route");
    route.setProcessDefinition(process);
    audiovisual.setId(2L);
    audiovisual.setActivityId("audiovisual");
    audiovisual.setProcessDefinition(process);
    when(definitions.findByProcessDefinitionIdAndActivityId(process.getId(), "audiovisual"))
        .thenReturn(Optional.of(audiovisual));
    var experiment = new Experiment();
    experiment.setId(91092L);
    experiment.setProduct(product);
    when(experiments.findById(91092L)).thenReturn(Optional.of(experiment));
    var parent = new BusinessProcessDefinition();
    parent.setProcessCode("pde-communication-sales-journey");
    parent.setId(91063L);
    task.setId(91001L);
    task.setAssignedAgent(Agent.builder().agentKey("communication-director").build());
    task.setProcessDefinition(parent);
    task.setProcessActivityId("communicationContract");
    task.setStatus("COMPLETED");
    task.setSourceReference("experiment:91092");
    task.setResultJson(
        "{\"contractVersion\":\"IRIS_COMMUNICATION_V1\",\"executionStatus\":\"COMPLETED\",\"outputType\":\"COMMUNICATION_PACKAGE\",\"sourceReference\":\"experiment:91092\",\"functionalOutput\":{\"messageStrategy\":\"Mensagem real\",\"channelBriefings\":[\"Demonstração privada\"],\"audiovisualBrief\":null}}");
    when(tasks.findFunctionalSnapshots(eq("experiment:91092"), anyCollection(), isNull()))
        .thenAnswer(ignored -> List.of(snapshot(task)));
  }

  /** Impede que a leitura da prontidão volte a carregar prompts de todas as tarefas do ciclo. */
  @Test
  void readinessDoesNotHydrateUnrelatedTaskAudit() {
    assertThat(executor.readiness(process, route, product, "experiment:91092").ready()).isTrue();
    verify(tasks, never()).findBySourceReferenceOrderByCreatedAtAscIdAsc(anyString());
  }

  /** Dispensa somente a produção não prevista e mantém sua condição distinta de sucesso. */
  @Test
  void recordsExplicitOmissionWithoutFabricatingVideo() {
    assertThat(executor.readiness(process, route, product, "experiment:91092").ready()).isTrue();
    assertThat(executor.execute(process, route, product, "experiment:91092").objectiveAchieved())
        .isTrue();
    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances, times(2)).saveAndFlush(saved.capture());
    var omitted = saved.getAllValues().get(1);
    assertThat(omitted.isObjectiveAchieved()).isFalse();
    assertThat(BusinessProcessOptionalActivity.isOmitted(omitted)).isTrue();
    omitted.setSourceReference("experiment:91093");
    assertThat(BusinessProcessOptionalActivity.isOmitted(omitted)).isFalse();
  }

  /**
   * Mantém audiovisual obrigatório quando houver briefing em vez de descartá-lo por conveniência.
   */
  @Test
  void retainsAudiovisualWhenRequired() throws Exception {
    var output = json.readTree(task.getResultJson());
    ((com.fasterxml.jackson.databind.node.ObjectNode) output.path("functionalOutput"))
        .put("audiovisualBrief", "Demonstração em vídeo prevista");
    task.setResultJson(output.toString());
    executor.execute(process, route, product, "experiment:91092");
    verify(instances).saveAndFlush(any());
  }

  /** Mantém a rota comercial existente e recusa dispensa enquanto Apolo ainda trabalha. */
  @Test
  void acceptsOfficialPlanAndDoesNotOmitRunningTask() {
    when(context.experimentId("commercial-plan:910@v1")).thenReturn(Optional.of(91092L));
    task.setSourceReference("commercial-plan:910@v1");
    task.setResultJson(task.getResultJson().replace("experiment:91092", "commercial-plan:910@v1"));
    when(tasks.findFunctionalSnapshots(eq(task.getSourceReference()), anyCollection(), isNull()))
        .thenAnswer(ignored -> List.of(snapshot(task)));
    assertThat(executor.readiness(process, route, product, task.getSourceReference()).ready())
        .isTrue();
    var running = new AgentTask();
    running.setId(91003L);
    running.setProcessDefinition(process);
    running.setProcessActivityId("audiovisual");
    running.setStatus("IN_PROGRESS");
    when(tasks.existsByProcessDefinitionIdAndSourceReferenceAndProcessActivityIdAndStatusIn(
            eq(process.getId()), eq(task.getSourceReference()), eq("audiovisual"), anyCollection()))
        .thenReturn(true);
    assertThatThrownBy(() -> executor.execute(process, route, product, task.getSourceReference()))
        .hasMessageContaining("em andamento");
    verify(instances, never()).saveAndFlush(argThat(i -> "NOT_APPLICABLE".equals(i.getStatus())));
  }

  /** Uma tentativa posterior bloqueada invalida a aprovação anterior da comunicação. */
  @Test
  void rejectsLatestBlockedAttemptAndOtherProduct() {
    var blocked = new AgentTask();
    blocked.setId(91002L);
    blocked.setProcessDefinition(task.getProcessDefinition());
    blocked.setProcessActivityId(task.getProcessActivityId());
    blocked.setAssignedAgent(task.getAssignedAgent());
    blocked.setStatus("BLOCKED");
    when(tasks.findFunctionalSnapshots(eq("experiment:91092"), anyCollection(), isNull()))
        .thenAnswer(ignored -> List.of(snapshot(task), snapshot(blocked)));
    assertThat(executor.readiness(process, route, product, "experiment:91092").ready()).isFalse();
    assertThatThrownBy(() -> executor.execute(process, route, product, "experiment:91092"))
        .hasMessageContaining("última tentativa");
    assertThat(
            executor
                .readiness(process, route, Product.builder().id(91010L).build(), "experiment:91092")
                .ready())
        .isFalse();
    verifyNoInteractions(instances);
  }

  /** Converte a fixture mutável na mesma projeção funcional escalar consultada em produção. */
  private AgentTaskFunctionalSnapshot snapshot(AgentTask value) {
    return new AgentTaskFunctionalSnapshot(
        value.getId(),
        value.getProcessDefinition().getId(),
        value.getProcessDefinition().getProcessCode(),
        value.getProcessActivityId(),
        value.getAssignedAgent().getAgentKey(),
        value.getStatus(),
        value.getCreatedAt(),
        value.getDeliveredAt(),
        value.getResultJson());
  }
}
