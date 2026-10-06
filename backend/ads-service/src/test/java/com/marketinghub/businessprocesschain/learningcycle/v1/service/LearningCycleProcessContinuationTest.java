package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleProcessContext.Work;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: comprovar passagem entre processos sem misturar contexto, gates ou autorização.
 */
class LearningCycleProcessContinuationTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final LearningCycleWorkResolver work = mock(LearningCycleWorkResolver.class);
  private final LearningCycleService service = mock(LearningCycleService.class);
  private final LearningCycleProcessContinuation continuation =
      new LearningCycleProcessContinuation(cycles, chains, products, work, service);
  private ProcessRun run;
  private LearningSalesCycle cycle;
  private Product product;
  private Work next;

  /** Reproduz a aprovação do planejamento com identidades inteiramente substituíveis. */
  private void fixture(long id) {
    run = new ProcessRun();
    run.setId(id + 1);
    run.setProductId(id);
    run.setLearningCycleId(id + 2);
    run.setChainDefinitionId(id + 3);
    run.setSourceReference("experiment:" + (id + 4));
    run.setProcessDefinitionId(id + 5);
    run.setStatus("COMPLETED");
    cycle = new LearningSalesCycle();
    cycle.setId(id + 2);
    cycle.setProductId(id);
    cycle.setChainDefinitionId(id + 3);
    cycle.setExperimentId(id + 4);
    cycle.setStatus("OPEN");
    cycle.setStage("PLANNING");
    product = Product.builder().id(id).automaticExecutionEnabled(true).build();
    when(cycles.findLocked(id, id + 2)).thenReturn(Optional.of(cycle));
    when(products.findById(id)).thenReturn(Optional.of(product));
    var chain = new BusinessProcessChainDefinition();
    chain.setItems(new ArrayList<>());
    int number = 0;
    for (String code :
        List.of(
            "pde-commercial-plan-offer",
            "pde-construction-approval",
            "pde-communication-sales-journey")) {
      var process = new BusinessProcessDefinition();
      process.setId(id + 5 + number);
      process.setProcessCode(code);
      var item = new BusinessProcessChainItem();
      item.setProcessDefinition(process);
      item.setSequenceNumber(++number);
      chain.getItems().add(item);
    }
    when(chains.findById(id + 3)).thenReturn(Optional.of(chain));
    next =
        new Work(
            id + 6,
            3,
            "Construção",
            "journey",
            2,
            "Jornada",
            "Dédalo",
            "NOT_STARTED",
            "Pronto",
            "/construction");
  }

  /** O caso original e outro produto avançam somente depois das provas do planejamento. */
  @ParameterizedTest
  @ValueSource(longs = {7, 97001})
  void completesPlanningAndResolvesConstruction(long id) {
    fixture(id);
    when(work.resolvePreparation(cycle))
        .thenReturn(
            new LearningCycleWorkResolver.Resolution(null, true),
            new LearningCycleWorkResolver.Resolution(next, false));
    assertThat(continuation.next(run))
        .isEqualTo(
            new LearningCycleProcessContinuation.Next(
                next, "experiment:" + cycle.getExperimentId()));
    verify(service).completePreparationFromProcesses(id, id + 2, id + 1);
  }

  /** O formato PDE usa sua referência canônica sem perder a identidade do ciclo no comando. */
  @Test
  void preservesCanonicalConstructionReference() {
    fixture(7);
    cycle.setStage("ADJUSTMENT");
    product.setValidationDefinitionVersion("PDE_AGENT_VALIDATION_V1");
    when(work.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(next, false));
    assertThat(continuation.next(run).sourceReference()).isEqualTo("product:7@agent-validation-v1");
    run.setProcessDefinitionId(13L);
    run.setSourceReference("product:7@agent-validation-v1");
    var communication =
        new Work(
            14L,
            4,
            "Comunicação",
            "communicationContract",
            1,
            "Contrato",
            "Íris",
            "NOT_STARTED",
            "Pronto",
            "/communication");
    when(work.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(communication, false));
    assertThat(continuation.next(run).sourceReference()).isEqualTo("experiment:11");
  }

  /** Prova do planejamento já registrada é preservada e a construção continua disponível. */
  @Test
  void preservesExistingStageDecision() {
    fixture(7);
    cycle.setStage("ADJUSTMENT");
    when(work.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(next, false));
    assertThat(continuation.next(run))
        .isEqualTo(
            new LearningCycleProcessContinuation.Next(
                next, "experiment:" + cycle.getExperimentId()));
    verifyNoInteractions(service);
  }

  /** Comunicação só recebe o contexto após a conclusão da construção, sem antecipar ajuste. */
  @Test
  void handsConstructionToCommunication() {
    fixture(7);
    cycle.setStage("ADJUSTMENT");
    run.setProcessDefinitionId(13L);
    var communication =
        new Work(
            14L,
            4,
            "Comunicação",
            "communicationContract",
            1,
            "Contrato",
            "Íris",
            "NOT_STARTED",
            "Pronto",
            "/communication");
    when(work.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(communication, false));
    assertThat(continuation.next(run).work()).isEqualTo(communication);
    verifyNoInteractions(service);
  }

  /** Ajuste comprovado termina na próxima etapa contratual, sem inventar verba audiovisual. */
  @Test
  void finishesAdjustmentWithoutAuthorizingCommercialWork() {
    fixture(7);
    cycle.setStage("ADJUSTMENT");
    run.setProcessDefinitionId(14L);
    when(work.resolvePreparation(cycle))
        .thenReturn(
            new LearningCycleWorkResolver.Resolution(null, true),
            new LearningCycleWorkResolver.Resolution(null, false));
    assertThat(continuation.next(run)).isNull();
    verify(service).completePreparationFromProcesses(7L, 9L, 8L);
  }

  /** Protege STOP, pausa, contexto histórico, subprocessos e fases com autorização própria. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "STOP",
        "PAUSED",
        "CLOSED",
        "PENDING",
        "CHILD",
        "NO_CYCLE",
        "VIDEO_BRIEF",
        "AUTHORIZATION",
        "PUBLICATION",
        "MEASUREMENT"
      })
  void preservesGuards(String guard) {
    fixture(7);
    switch (guard) {
      case "STOP" -> product.setAutomaticExecutionEnabled(false);
      case "PAUSED", "PENDING" -> run.setStatus(guard);
      case "CLOSED" -> cycle.setStatus(guard);
      case "CHILD" -> run.setParentRunId(100L);
      case "NO_CYCLE" -> run.setLearningCycleId(null);
      default -> cycle.setStage(guard);
    }
    assertThat(continuation.next(run)).isNull();
    verifyNoInteractions(work, service);
  }

  /** Fonte ou cadeia incompatível é erro explícito antes de qualquer transição. */
  @ParameterizedTest
  @ValueSource(strings = {"chain", "experiment"})
  void rejectsCrossContext(String field) {
    fixture(7);
    if ("chain".equals(field)) run.setChainDefinitionId(999L);
    else run.setSourceReference("experiment:999");
    assertThatThrownBy(() -> continuation.next(run)).hasMessageContaining("outro contexto");
    verifyNoInteractions(work, service);
  }

  /** Uma prova revogada no próprio processo não gera nova tarefa ou aceite automático. */
  @Test
  void doesNotRetryIncompleteProcess() {
    fixture(7);
    var current =
        new Work(
            12L,
            2,
            "Planejamento",
            "economics",
            2,
            "Economia",
            "Plutus",
            "BLOCKED",
            "Revisar",
            "/plan");
    when(work.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(current, false));
    assertThat(continuation.next(run)).isNull();
    verifyNoInteractions(service);
  }
}
