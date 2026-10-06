package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.command.RegisterCyclePrototypeRequest;
import com.marketinghub.experiment.*;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.*;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Comprova o handoff inicial sem trocar condições, aprovações ou história de qualquer produto. */
class LearningCyclePrototypeRegistrationTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningSalesCycleEventRepository events =
      mock(LearningSalesCycleEventRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningCycleEvidence evidence = mock(LearningCycleEvidence.class);
  private final ObjectMapper json = new ObjectMapper();
  private final List<LearningSalesCycleEvent> history = new ArrayList<>();
  private final LearningCycleService service =
      new LearningCycleService(
          cycles,
          events,
          products,
          experiments,
          chains,
          processes,
          null,
          new LearningCycleJson(json),
          null,
          evidence,
          mock(LearningCycleVideoEvidence.class),
          null,
          null);
  private LearningSalesCycle cycle;
  private Experiment experiment;

  /** Monta ciclo e experimento sintéticos com política que exige sucessor quando houver mudança. */
  @BeforeEach
  void setUp() {
    cycle = new LearningSalesCycle();
    cycle.setId(8006L);
    cycle.setProductId(9010L);
    cycle.setExperimentId(7099L);
    cycle.setChainDefinitionId(2600L);
    cycle.setProcessDefinitionId(7500L);
    cycle.setProductVersion("declared-version-17");
    cycle.setStage("ADJUSTMENT");
    cycle.setStatus("OPEN");
    cycle.setBudgetLimitBrl(BigDecimal.ZERO);
    cycle.setBriefJson("{}");
    cycle.setInheritedLearningJson("{}");
    cycle.setVersionChangedAt(Instant.EPOCH);
    var product = Product.builder().id(9010L).build();
    experiment =
        Experiment.builder().id(7099L).product(product).status(ExperimentStatus.PLANNED).build();
    var process = new BusinessProcessDefinition();
    process.setId(7500L);
    process.setVersionNumber(1);
    process.setDiagramJson("{\"nodes\":[],\"experimentChangePolicy\":\"CHANGE_PER_CYCLE_V1\"}");
    var chain = new BusinessProcessChainDefinition();
    chain.setId(2600L);
    chain.setItems(List.of());
    when(cycles.findLocked(9010L, 8006L)).thenReturn(Optional.of(cycle));
    when(cycles.findByProductIdOrderByIdDesc(9010L)).thenReturn(List.of(cycle));
    when(cycles.findByPreviousCycleId(8006L)).thenReturn(Optional.empty());
    when(products.findLockedById(9010L)).thenReturn(Optional.of(product));
    when(products.findById(9010L)).thenReturn(Optional.of(product));
    when(experiments.findById(7099L)).thenReturn(Optional.of(experiment));
    when(processes.findById(7500L)).thenReturn(Optional.of(process));
    when(chains.findById(2600L)).thenReturn(Optional.of(chain));
    when(events.findByCycleIdOrderByRevisionAsc(8006L)).thenAnswer(a -> history);
    when(events.findByCycleIdAndRequestKey(eq(8006L), anyString()))
        .thenAnswer(
            a ->
                history.stream()
                    .filter(e -> e.getRequestKey().equals(a.getArgument(1)))
                    .findFirst());
    when(events.saveAndFlush(any()))
        .thenAnswer(
            a -> {
              LearningSalesCycleEvent e = a.getArgument(0);
              e.setId((long) history.size() + 1);
              history.add(e);
              return e;
            });
    ReflectionTestUtils.setField(
        service, "prototypeContext", new LearningCyclePrototypeContext(events, json));
    ReflectionTestUtils.setField(service, "videoBudget", mock(LearningCycleVideoBudget.class));
  }

  /** Aceita uma única prova, preserva a versão e permite replay sem novo evento ou gasto. */
  @Test
  void registersFirstExactVersionAndReplaysWithoutChangingCycle() {
    var request = request();
    assertThat(service.list(9010L).getFirst().prototypeRegistration().available()).isTrue();
    var response = service.registerPrototype(9010L, 8006L, request);
    assertThat(response.stage()).isEqualTo("ADJUSTMENT");
    assertThat(response.productVersion()).isEqualTo("declared-version-17");
    assertThat(response.budgetLimitBrl()).isZero();
    assertThat(response.prototypeRegistration().available()).isFalse();
    assertThat(cycle.getVersionChangedAt()).isEqualTo(Instant.EPOCH);
    assertThat(service.registerPrototype(9010L, 8006L, request).revision()).isEqualTo(1);
    assertThat(history).hasSize(1);
    assertThat(history.getFirst().getAction()).isEqualTo("REGISTER_PROTOTYPE");
    assertThat(new LearningCyclePrototypeContext(events, json).resolve(cycle)).isPresent();
    verify(experiments, never()).save(any());
    assertThatThrownBy(() -> service.registerPrototype(9010L, 8006L, request()))
        .hasMessageContaining("já foi registrada");
  }

  /** Bloqueia troca de versão, revisão concorrente e experimento já exposto ou liberado. */
  @Test
  void rejectsChangesAndCommercialExposure() {
    var proof = (com.fasterxml.jackson.databind.node.ObjectNode) request().privatePrototype();
    proof.put("prototypeVersion", "another-version");
    assertThatThrownBy(
            () ->
                service.registerPrototype(
                    9010L,
                    8006L,
                    new RegisterCyclePrototypeRequest(UUID.randomUUID(), 0, "Teste", proof)))
        .hasMessageContaining("mesma versão");
    var stale = request();
    cycle.setRevision(1);
    assertThatThrownBy(() -> service.registerPrototype(9010L, 8006L, stale))
        .hasMessageContaining("ciclo mudou");
    experiment.setFacebookReleaseRequestedAt(Instant.now());
    assertThatThrownBy(() -> service.registerPrototype(9010L, 8006L, request()))
        .hasMessageContaining("liberação");
    assertThat(history).isEmpty();
  }

  /** Impede registro no histórico encerrado e no produto errado. */
  @Test
  void preservesClosedCyclesAndProductBoundaries() {
    cycle.setStatus("ADJUSTED");
    assertThatThrownBy(() -> service.registerPrototype(9010L, 8006L, request()))
        .hasMessageContaining("indisponível");
    when(products.findLockedById(9011L))
        .thenReturn(Optional.of(Product.builder().id(9011L).build()));
    assertThatThrownBy(() -> service.registerPrototype(9011L, 8006L, request()))
        .hasMessageContaining("neste produto");
    assertThat(history).isEmpty();
  }

  /**
   * Constrói prova sintética completa da versão declarada, sem segredo em URL ou efeitos externos.
   */
  private RegisterCyclePrototypeRequest request() {
    var p = json.createObjectNode();
    p.put("prototypeVersion", cycle.getProductVersion());
    p.put("privateAccessUrl", "https://private.invalid/candidate");
    p.put("image", "repo/image:validated");
    p.put("evidenceReference", "Relatório sintético completo dos testes locais desta candidata");
    p.put("observedAt", Instant.now().toString());
    for (String k :
        List.of(
            "desktopValidated",
            "mobileValidated",
            "firstResultValidated",
            "resumeValidated",
            "failuresValidated",
            "testDataExcluded",
            "noExternalSideEffects")) p.put(k, true);
    return new RegisterCyclePrototypeRequest(
        UUID.randomUUID(), cycle.getRevision(), "Operador de teste", p);
  }
}
