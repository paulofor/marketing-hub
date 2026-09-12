package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Responsabilidade: preservar a continuidade após ajuste sem reabrir a decisão histórica. */
class LearningCycleContinuationTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningCycleService service =
      new LearningCycleService(
          cycles,
          events,
          products,
          null,
          null,
          processes,
          null,
          new LearningCycleJson(new ObjectMapper()),
          null,
          null,
          null,
          null,
          null);

  /** Reproduz o predecessor conciliado e encerrado, sem inventar execução do sucessor. */
  private LearningSalesCycle adjustedCycle() {
    org.springframework.test.util.ReflectionTestUtils.setField(
        service,
        "videoBudget",
        new LearningCycleVideoBudget(events, new LearningCycleJson(new ObjectMapper())));
    var cycle = new LearningSalesCycle();
    cycle.setId(1L);
    cycle.setProductId(4L);
    cycle.setExperimentId(91L);
    cycle.setProcessDefinitionId(72L);
    cycle.setStage("DECISION");
    cycle.setStatus("ADJUSTED");
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    var process = new BusinessProcessDefinition();
    process.setId(72L);
    process.setVersionNumber(1);
    process.setDiagramJson("{\"nodes\":[]}");
    when(products.findById(4L)).thenReturn(Optional.of(Product.builder().id(4L).build()));
    when(cycles.findByProductIdOrderByIdDesc(4L)).thenReturn(List.of(cycle));
    when(processes.findById(72L)).thenReturn(Optional.of(process));
    return cycle;
  }

  /** Sem sucessor, oferece sua criação e não encaminha trabalho ao experimento encerrado. */
  @Test
  void adjustedCycleRequiresSuccessorInsteadOfAnotherApproval() {
    adjustedCycle();
    var result = service.list(4L).getFirst();
    assertThat(result.nextAction()).contains("Ajuste aprovado", "experimento planejado");
    assertThat(result.nextAction()).doesNotContain("Atena prepara a proposta");
    assertThat(result.workUrl()).isNull();
    assertThat(result.canCreateSuccessor()).isTrue();
    assertThat(result.commands()).isEmpty();
    verify(cycles, never()).save(any());
  }

  /** Depois do vínculo, direciona à cadeia e ao experimento reais da nova ocorrência. */
  @Test
  void adjustedCycleResumesExistingSuccessorWithoutDuplicatingIt() {
    adjustedCycle();
    var successor = new LearningSalesCycle();
    successor.setId(2L);
    successor.setProductId(4L);
    successor.setExperimentId(92L);
    successor.setChainDefinitionId(14L);
    when(cycles.findByPreviousCycleId(1L)).thenReturn(Optional.of(successor));
    var result = service.list(4L).getFirst();
    assertThat(result.workUrl())
        .isEqualTo("/business-process-chains/learning-cycles?chainId=14&productId=4&cycleId=2");
    assertThat(result.nextAction()).contains("ciclo #2", "experimento #92");
    assertThat(result.canCreateSuccessor()).isFalse();
    assertThat(result.commands()).isEmpty();
  }

  /** Preparação comprovada orienta seu registro e não encaminha à correção já terminada. */
  @Test
  void completedPreparationDirectsToEvidenceRegistration() {
    var cycle = adjustedCycle();
    cycle.setStage("ADJUSTMENT");
    cycle.setStatus("OPEN");
    cycle.setChainDefinitionId(14L);
    var chains =
        mock(
            com.marketinghub.repository.jpa.businessprocesschain
                .BusinessProcessChainDefinitionRepository.class);
    when(chains.findById(14L))
        .thenReturn(
            Optional.of(
                new com.marketinghub.businessprocesschain.BusinessProcessChainDefinition()));
    org.springframework.test.util.ReflectionTestUtils.setField(service, "chains", chains);
    var resolver = mock(LearningCycleWorkResolver.class);
    when(resolver.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(null, true));
    org.springframework.test.util.ReflectionTestUtils.setField(service, "workResolver", resolver);
    var result = service.list(4L).getFirst();
    assertThat(result.workUrl()).isNull();
    assertThat(result.nextAction())
        .contains("já comprovaram", "Registre as evidências", "não autoriza");
    assertThat(result.nextAction()).doesNotContain("execute «");
    assertThat(result.stage()).isEqualTo("ADJUSTMENT");
    verify(cycles, never()).save(any());
  }
}
