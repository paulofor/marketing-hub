package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.LearningCycleDecisionApproval;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.LearningCycleCommand;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles.LearningCycleProcessContext.Work;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleEventRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir avanço manual que ignore o trabalho delegado do mesmo ciclo. */
class LearningCycleDelegatedWorkTest {
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningCycleBpmLedger ledger = mock(LearningCycleBpmLedger.class);
  private final LearningCycleWorkResolver resolver = mock(LearningCycleWorkResolver.class);
  private final LearningCycleDecisionApproval approval = mock(LearningCycleDecisionApproval.class);
  private final LearningCycleService service =
      new LearningCycleService(
          cycles,
          events,
          products,
          experiments,
          chains,
          processes,
          null,
          new LearningCycleJson(mapper),
          ledger,
          mock(LearningCycleEvidence.class),
          null,
          null,
          null);

  /** Modela Capella e outro produto sem exceção por nome, mantendo a identidade da passagem. */
  private LearningSalesCycle fixture(long productId) {
    var product = Product.builder().id(productId).build();
    var cycle = new LearningSalesCycle();
    cycle.setId(productId + 10);
    cycle.setProductId(productId);
    cycle.setExperimentId(productId + 20);
    cycle.setChainDefinitionId(productId + 30);
    cycle.setProcessDefinitionId(productId + 40);
    cycle.setProductVersion("v1");
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setRevision(3);
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    var experiment = new Experiment();
    experiment.setId(cycle.getExperimentId());
    experiment.setProduct(product);
    var definition = new BusinessProcessDefinition();
    definition.setId(cycle.getProcessDefinitionId());
    definition.setVersionNumber(1);
    definition.setDiagramJson("{\"nodes\":[]}");
    when(products.findById(productId)).thenReturn(Optional.of(product));
    when(products.findLockedById(productId)).thenReturn(Optional.of(product));
    when(cycles.findByProductIdOrderByIdDesc(productId)).thenReturn(List.of(cycle));
    when(cycles.findLocked(productId, cycle.getId())).thenReturn(Optional.of(cycle));
    when(experiments.findById(cycle.getExperimentId())).thenReturn(Optional.of(experiment));
    when(processes.findById(cycle.getProcessDefinitionId())).thenReturn(Optional.of(definition));
    when(chains.findById(cycle.getChainDefinitionId()))
        .thenReturn(Optional.of(new BusinessProcessChainDefinition()));
    ReflectionTestUtils.setField(service, "workResolver", resolver);
    ReflectionTestUtils.setField(service, "decisionApproval", approval);
    ReflectionTestUtils.setField(
        service,
        "videoBudget",
        new LearningCycleVideoBudget(events, new LearningCycleJson(mapper)));
    return cycle;
  }

  /** Produz evidência textual válida para comprovar que ela não contorna uma entrega ausente. */
  private LearningCycleCommand complete(LearningSalesCycle cycle) {
    return new LearningCycleCommand(
        UUID.randomUUID(),
        cycle.getRevision(),
        LearningCycleCommand.Action.COMPLETE,
        "Operador de teste",
        "Ajuste declarado concluído",
        "internal://fixture/completion",
        mapper
            .createObjectNode()
            .put("productVersion", "v1")
            .put("changeEvidence", "internal://fixture/change"));
  }

  /** Leitura e comando preservam o bloqueio, sem gravar evento, custo ou conclusão artificial. */
  @ParameterizedTest
  @ValueSource(longs = {7L, 7007L})
  void pendingWorkCannotBeCompletedWithTextEvidence(long productId) {
    var cycle = fixture(productId);
    var work =
        new Work(
            productId + 50,
            3,
            "Construção",
            "technicalHomologation",
            6,
            "Homologar tecnicamente a versão real",
            "Psique",
            "NOT_STARTED",
            "Dédalo precisa concluir a implementação privada.",
            "/products/"
                + productId
                + "/value-chain-history/processes/"
                + (productId + 50)
                + "/activities?learningCycleId="
                + cycle.getId()
                + "&chainId="
                + cycle.getChainDefinitionId()
                + "#activity-technicalHomologation");
    when(resolver.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(work, false));
    var response = service.list(productId).getFirst();
    assertThat(response.delegatedWork()).isEqualTo(work);
    assertThat(response.workUrl()).isEqualTo(work.url());
    assertThat(response.commands())
        .filteredOn(c -> c.action().equals("COMPLETE"))
        .hasSize(1)
        .allSatisfy(
            c -> {
              assertThat(c.available()).isFalse();
              assertThat(c.reason()).contains("3.6", "não substitui");
            });
    assertThatThrownBy(() -> service.command(productId, cycle.getId(), complete(cycle)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("ainda não comprovou");
    assertThat(cycle.getStage()).isEqualTo("ADJUSTMENT");
    assertThat(cycle.getRevision()).isEqualTo(3);
    verify(events, never()).saveAndFlush(any());
    verify(cycles, never()).saveAndFlush(any());
    verifyNoInteractions(ledger);
  }

  /** A preparação antes válida continua avançando para homologação sem conceder publicação. */
  @Test
  void provenPreparationStillAdvancesThroughTheExistingContract() {
    var cycle = fixture(7007L);
    when(resolver.resolvePreparation(cycle))
        .thenReturn(new LearningCycleWorkResolver.Resolution(null, true));
    assertThat(service.list(7007L).getFirst().delegatedWork()).isNull();
    var response = service.command(7007L, cycle.getId(), complete(cycle));
    assertThat(response.stage()).isEqualTo("VALIDATION");
    assertThat(response.status()).isEqualTo("OPEN");
    verify(events).saveAndFlush(any());
    verify(cycles).saveAndFlush(cycle);
  }
}
