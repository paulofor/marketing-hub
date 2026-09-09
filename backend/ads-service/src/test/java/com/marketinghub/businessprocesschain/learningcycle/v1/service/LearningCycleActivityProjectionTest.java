package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycleEvent;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleEntry;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir divergência entre a chamada do pai e o ciclo auditável do produto. */
class LearningCycleActivityProjectionTest {
  private final LearningCycleService service = mock(LearningCycleService.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final LearningCycleActivityProjection projection =
      new LearningCycleActivityProjection(service, cycles, instances, events);

  /**
   * Um ciclo em decisão aparece em andamento, com destino direto, sem concluir os predecessores.
   */
  @Test
  void resumesPersistedDecisionFromCallingActivity() {
    var cycle = cycle();
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(4L, 13L))
        .thenReturn(Optional.of(cycle));
    var result = project(4L, null);
    assertThat(result.getFirst().operationalState()).isEqualTo("NOT_STARTED");
    var call = result.getLast();
    assertThat(call.operationalState()).isEqualTo("IN_PROGRESS");
    assertThat(call.stateReason()).contains("Ciclo #1", "experimento #91", "Decisão comercial");
    assertThat(call.objectiveAchieved()).isFalse();
    assertThat(call.stateEvidence()).isEqualTo("SUBPROCESS");
    assertThat(call.executionControl().navigationUrl())
        .isEqualTo("/business-process-chains/learning-cycles?chainId=13&productId=4&cycleId=1");
    assertThat(call.executionControl().targetProcessDefinitionId()).isEqualTo(72L);
    assertThat(call.tasks()).isEmpty();
    verifyNoMoreInteractions(instances);
    verify(cycles, never()).save(any());
  }

  /** A falta de ciclo permite a entrada explícita sem inventar andamento ou criar registros. */
  @Test
  void doesNotInventExecutionWithoutCycle() {
    var call = project(4L, null).getLast();
    assertThat(call.operationalState()).isEqualTo("NOT_STARTED");
    assertThat(call.executionControl().navigationUrl()).endsWith("chainId=13&productId=4");
    assertThat(call.executionControl().actionAvailable()).isTrue();
    verify(cycles, never()).save(any());
    verifyNoInteractions(instances, events);
  }

  /** O último bloqueio da medição permanece visível mesmo após abrir outra instância pendente. */
  @Test
  void showsMeasurementBlockerAfterLedgerOpenedPendingInstance() {
    var cycle = cycle();
    cycle.setStage("MEASUREMENT");
    cycle.setCurrentInstanceId(220L);
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(4L, 13L))
        .thenReturn(Optional.of(cycle));
    var instance = new BusinessProcessActivityInstance();
    instance.setStatus("PENDING");
    when(instances.findById(220L)).thenReturn(Optional.of(instance));
    var event = new LearningSalesCycleEvent();
    event.setAction("MEASUREMENT_BLOCKED");
    event.setSummary("Snapshot de mídia indisponível; atualizar a fonte.");
    when(events.findFirstByCycleIdOrderByRevisionDesc(1L)).thenReturn(Optional.of(event));
    var call = project(4L, null).getLast();
    assertThat(call.operationalState()).isEqualTo("BLOCKED");
    assertThat(call.stateReason()).contains("Snapshot de mídia indisponível");
    assertThat(call.executionControl().actionAvailable()).isTrue();
  }

  /** Encerramento comprovado conclui somente a chamada e preserva acesso ao histórico. */
  @Test
  void keepsClosedCycleAccessibleWithoutClaimingSalesSuccess() {
    var cycle = cycle();
    cycle.setStatus("ADJUSTED");
    cycle.setClosedAt(Instant.parse("2026-09-09T00:00:00Z"));
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(4L, 13L))
        .thenReturn(Optional.of(cycle));
    var call = project(4L, null).getLast();
    assertThat(call.operationalState()).isEqualTo("COMPLETED");
    assertThat(call.objectiveAchieved()).isTrue();
    assertThat(call.executionControl().actionLabel()).contains("Consultar");
    assertThat(call.executionControl().actionAvailable()).isTrue();
  }

  /** Outro produto não recebe o andamento nem a referência do ciclo do Vega. */
  @Test
  void separatesProducts() {
    when(cycles.findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(4L, 13L))
        .thenReturn(Optional.of(cycle()));
    var call = project(5L, null).getLast();
    assertThat(call.operationalState()).isEqualTo("NOT_STARTED");
    assertThat(call.executionControl().navigationUrl()).endsWith("productId=5");
    verify(cycles, never()).findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(4L, 13L);
  }

  /** A escolha histórica explícita conserva sua cadeia, sem selecionar o ciclo mais recente. */
  @Test
  void respectsExplicitCycleVersion() {
    var cycle = cycle();
    when(cycles.findById(1L)).thenReturn(Optional.of(cycle));
    var call = project(4L, 1L).getLast();
    assertThat(call.executionControl().navigationUrl())
        .endsWith("chainId=13&productId=4&cycleId=1");
    verify(service).entry(73L, 4L, 13L);
    verify(cycles, never())
        .findFirstByProductIdAndChainDefinitionIdOrderByIdDesc(anyLong(), anyLong());
  }

  /** Processos sem chamada do ciclo não consultam nem recebem sua projeção. */
  @Test
  void doesNotAffectUnrelatedProcesses() {
    var groups = List.of(group("optimization", 1));
    assertThat(projection.apply(new BusinessProcessDefinition(), 4L, null, Map.of(), groups))
        .isSameAs(groups);
    verifyNoInteractions(service, cycles, instances, events);
  }

  /** Uma cadeia sem vínculo não pode reutilizar a entrada genérica nem o estado de outra cadeia. */
  @Test
  void disablesNavigationForUnrelatedSelectedChain() {
    var process = new BusinessProcessDefinition();
    process.setId(73L);
    var definition = new BusinessProcessActivityDefinition();
    definition.setSubprocessCode(LearningCycleRules.PROCESS_CODE);
    var call =
        projection
            .apply(
                process,
                4L,
                null,
                Map.of("learningCycle", definition),
                List.of(group("learningCycle", 4)),
                99L)
            .getFirst();
    verify(service).entry(73L, 4L, 99L);
    assertThat(call.executionControl().actionAvailable()).isFalse();
    assertThat(call.executionControl().navigationUrl()).isNull();
    verifyNoInteractions(cycles, instances, events);
  }

  /** A URL não pode combinar um ciclo válido com uma cadeia diferente da sua origem. */
  @Test
  void rejectsMixedCycleAndChainContext() {
    var process = new BusinessProcessDefinition();
    process.setId(73L);
    var definition = new BusinessProcessActivityDefinition();
    definition.setSubprocessCode(LearningCycleRules.PROCESS_CODE);
    when(cycles.findById(1L)).thenReturn(Optional.of(cycle()));
    var call =
        projection
            .apply(
                process,
                4L,
                1L,
                Map.of("learningCycle", definition),
                List.of(group("learningCycle", 4)),
                99L)
            .getFirst();
    assertThat(call.executionControl().actionAvailable()).isFalse();
    verifyNoInteractions(service, instances, events);
  }

  /** Monta a projeção com a mesma origem e o mesmo destino fornecidos pelo contrato de entrada. */
  private List<ProductProcessActivityExecutionGroupResponse> project(
      Long productId, Long explicitCycleId) {
    var process = new BusinessProcessDefinition();
    process.setId(73L);
    var definition = new BusinessProcessActivityDefinition();
    definition.setSubprocessCode(LearningCycleRules.PROCESS_CODE);
    var entry =
        new LearningCycleEntry(
            13L,
            "Cadeia PDE",
            73L,
            "Venda e aprendizado",
            6,
            "learningCycle",
            74L,
            "Ciclos de aprendizado e vendas",
            true,
            true,
            "Abrir atividade",
            "/business-process-chains/learning-cycles?chainId=13",
            "Retomar",
            "/products/4/",
            List.of());
    when(service.entry(73L, productId, explicitCycleId == null ? null : 13L)).thenReturn(entry);
    return projection.apply(
        process,
        productId,
        explicitCycleId,
        Map.of("learningCycle", definition),
        List.of(group("optimization", 1), group("learningCycle", 4)));
  }

  /** Cria uma atividade sem tarefas do pai para reproduzir a lacuna observada no Vega. */
  private ProductProcessActivityExecutionGroupResponse group(String id, int sequence) {
    return new ProductProcessActivityExecutionGroupResponse(
        1L,
        id,
        id,
        "Objetivo",
        "Operador",
        sequence,
        true,
        "NOT_STARTED",
        "Nenhuma tarefa",
        false,
        "NOT_RECORDED",
        null,
        null,
        0,
        List.of());
  }

  /** Cria somente a identidade persistida da referência histórica, sem métricas fabricadas. */
  private LearningSalesCycle cycle() {
    var cycle = new LearningSalesCycle();
    cycle.setId(1L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(13L);
    cycle.setProcessDefinitionId(72L);
    cycle.setExperimentId(91L);
    cycle.setStage("DECISION");
    cycle.setStatus("OPEN");
    return cycle;
  }
}
