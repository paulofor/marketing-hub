package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleCommercialReadiness;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleCommercialPreparation;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: provar materialização governada e recusa de callbacks inconsistentes. */
class OpalaCommercialServiceTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpalaCommercialContext context = mock(OpalaCommercialContext.class);
  private final OpalaCommercialMaterialization materialization =
      mock(OpalaCommercialMaterialization.class);
  private final LearningCycleCommercialReadiness readiness =
      mock(LearningCycleCommercialReadiness.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final OpalaCommercialService service =
      new OpalaCommercialService(
          context, materialization, readiness, tasks, mock(OpalaCommercialRouting.class));
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Experiment experiment =
      Experiment.builder().id(92L).unitPrice(new BigDecimal("67")).build();
  private final AgentTask task = new AgentTask();
  private final String identity =
      "{\"productId\":4,\"experimentId\":92,\"cycleId\":2,\"productVersion\":\"fixture-v12\"}";

  /** Monta uma ocorrência sintética com a mesma correlação exigida no callback real. */
  @BeforeEach
  void setup() throws Exception {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setProductVersion("fixture-v12");
    cycle.setVersionChangedAt(Instant.now().minusSeconds(60));
    cycle.setBudgetLimitBrl(new BigDecimal("100"));
    var process = new BusinessProcessDefinition();
    process.setId(100L);
    process.setProcessCode(OpalaCommercialContext.CODE);
    task.setProcessDefinition(process);
    task.setSourceReference("experiment:92");
    task.setCreatedAt(Instant.now());
    when(context.scope("experiment:92"))
        .thenReturn(new OpalaCommercialContext.Scope(cycle, experiment));
    when(context.read(anyString())).thenAnswer(i -> json.readTree((String) i.getArgument(0)));
    when(context.snapshot("experiment:92"))
        .thenReturn((com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(identity));
  }

  /** Cada atividade de preparação chama somente a materialização correspondente. */
  @ParameterizedTest
  @ValueSource(strings = {"entry", "creative", "checkout", "targeting"})
  void materializesOnlyItsOwnActivity(String activity) throws Exception {
    task.setProcessActivityId(activity);
    assertThat(service.apply(task, request("{\"decision\":\"READY\",\"instruction\":{}}")))
        .isEqualTo(AgentTaskCompletionHook.CompletionDisposition.COMPLETE);
    verify(materialization).apply(eq(activity), any(), eq(json.readTree("{}")));
  }

  /** Outra versão não pode alterar vínculos comerciais desta ocorrência. */
  @Test
  void rejectsOldVersionBeforeMaterialization() {
    task.setProcessActivityId("entry");
    var request =
        new CompleteAgentTaskRequest(
            "{\"decision\":\"READY\"}",
            "{\"opalaScope\":" + identity.replace("fixture-v12", "fixture-v11") + "}");
    assertThatThrownBy(() -> service.apply(task, request)).hasMessageContaining("outra ocorrência");
    verifyNoInteractions(materialization);
  }

  /** Uma tarefa anterior à versão atual não recebe aprovação retroativa. */
  @Test
  void rejectsTaskCreatedBeforeVersionChange() {
    task.setCreatedAt(cycle.getVersionChangedAt().minusSeconds(1));
    assertThatThrownBy(() -> service.apply(task, request("{\"decision\":\"READY\"}")))
        .hasMessageContaining("antes da versão");
  }

  /** Parecer sem margem positiva não conclui economia nem concede orçamento. */
  @Test
  void rejectsNonPositiveContribution() {
    task.setProcessActivityId("economics");
    assertThatThrownBy(
            () ->
                service.apply(
                    task,
                    request(
                        "{\"decision\":\"APPROVE\",\"economics\":{\"offerPriceBrl\":67,\"variableCostPerSaleBrl\":70,\"contributionPerSaleBrl\":-3}}")))
        .hasMessageContaining("economia aprovada");
    verifyNoInteractions(materialization);
  }

  /** Revisão aprovada pelo modelo continua bloqueada quando faltam ativos reais. */
  @ParameterizedTest
  @ValueSource(strings = {"humanExperienceReview", "commercialIntegrityReview"})
  void refusesApprovalWithoutCommercialInputs(String activity) {
    task.setProcessActivityId(activity);
    when(readiness.inspect(cycle))
        .thenReturn(
            new LearningCycleCommercialPreparation(
                false, "Falta publicar a versão", "/experiments/92", List.of()));
    assertThatThrownBy(() -> service.apply(task, request("{\"decision\":\"APPROVED\"}")))
        .hasMessageContaining("condições comerciais mudaram");
  }

  /** Constrói o envelope de callback usando a identidade conhecida antes da execução. */
  private CompleteAgentTaskRequest request(String result) {
    return new CompleteAgentTaskRequest(result, "{\"opalaScope\":" + identity + "}");
  }
}
