package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.executionprofile.v1.ExecutionProfile;
import com.marketinghub.product.executionprofile.v1.service.ExecutionProfileContext;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: impedir reserva eterna por versão retirada sem invalidar fichas congeladas. */
class ProcessRunVersionAuthorizationTest {
  private final BusinessProcessDefinitionRepository definitions =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ExecutionProfileContext profiles = mock(ExecutionProfileContext.class);
  private final ProcessRunContext context =
      new ProcessRunContext(
          null,
          definitions,
          null,
          cycles,
          null,
          new ObjectMapper(),
          org.mockito.Mockito.mock(
              com.marketinghub.repository.jpa.experiment.ExperimentRepository.class));
  private final ProcessRun run = new ProcessRun();
  private final BusinessProcessDefinition definition = new BusinessProcessDefinition();

  /** Reproduz a identidade retirada observada em Capella, sem consultar dados produtivos. */
  @BeforeEach
  void setup() {
    run.setProductId(7L);
    run.setProcessDefinitionId(96L);
    run.setChainDefinitionId(23L);
    run.setSourceReference("experiment:94");
    definition.setId(96L);
    definition.setVersionNumber(10);
    definition.setStatus("RETIRED");
    when(definitions.findById(96L)).thenReturn(Optional.of(definition));
    ReflectionTestUtils.setField(context, "executionProfileContext", profiles);
  }

  /** Uma referência legada sem ciclo ou ficha não pode reservar o produto indefinidamente. */
  @Test
  void blocksUnboundRetiredVersionWithoutCycle() {
    assertThat(context.dispatchBlockReason(run))
        .contains("v10", "#96", "não está publicada nem fixada", "Resultados preservados");
    verify(profiles).bound(7L, "experiment:94");
    verify(profiles, never()).pins(anyString(), anyLong());
  }

  /**
   * Mantém o mesmo bloqueio para ciclos abertos e não confunde ficha com publicação de rascunho.
   */
  @ParameterizedTest
  @ValueSource(strings = {"RETIRED", "DRAFT"})
  void blocksUnpublishedVersionWithOpenCycle(String status) {
    definition.setStatus(status);
    run.setLearningCycleId(2L);
    var cycle = new LearningSalesCycle();
    cycle.setStatus("OPEN");
    when(cycles.findById(2L)).thenReturn(Optional.of(cycle));
    when(profiles.bound(7L, "experiment:94")).thenReturn(Optional.of(new ExecutionProfile()));
    when(profiles.pins("experiment:94", 96L)).thenReturn("DRAFT".equals(status));
    assertThat(context.dispatchBlockReason(run)).contains("não está publicada nem fixada");
  }

  /** Uma versão publicada permanece autorizada sem exigir migração ou criação de ficha. */
  @Test
  void preservesPublishedVersion() {
    definition.setStatus("PUBLISHED");
    assertThat(context.dispatchBlockReason(run)).isNull();
    verifyNoInteractions(profiles);
  }

  /** A ficha da referência exata permite continuar sua versão retirada, conforme o contrato. */
  @Test
  void preservesExactlyPinnedRetiredVersion() {
    when(profiles.bound(7L, "experiment:94")).thenReturn(Optional.of(new ExecutionProfile()));
    when(profiles.pins("experiment:94", 96L)).thenReturn(true);
    assertThat(context.dispatchBlockReason(run)).isNull();
  }

  /** Uma ficha de outra referência ou outro processo não autoriza a definição retirada. */
  @Test
  void refusesPinsFromAnotherReferenceOrDefinition() {
    when(profiles.bound(7L, "experiment:94")).thenReturn(Optional.of(new ExecutionProfile()));
    when(profiles.pins("experiment:88", 96L)).thenReturn(true);
    when(profiles.pins("experiment:94", 105L)).thenReturn(true);
    assertThat(context.dispatchBlockReason(run)).isNotNull();
    verify(profiles).pins("experiment:94", 96L);
  }

  /** Encerrar o ciclo impede novos disparos mesmo quando a ficha preserva a versão retirada. */
  @Test
  void closedCycleStillBlocksPinnedVersion() {
    run.setLearningCycleId(2L);
    var cycle = new LearningSalesCycle();
    cycle.setStatus("CLOSED");
    when(cycles.findById(2L)).thenReturn(Optional.of(cycle));
    assertThat(context.dispatchBlockReason(run)).contains("O ciclo está encerrado");
    verifyNoInteractions(definitions, profiles);
  }

  /** Confere a referência exata em homologação, sem bloquear o processo de aprendizado. */
  @Test
  void blocksClosedExperimentOnlyForHomologation() {
    definition.setStatus("PUBLISHED");
    definition.setProcessCode("experiment-homologation-activation");
    var experiments =
        (com.marketinghub.repository.jpa.experiment.ExperimentRepository)
            ReflectionTestUtils.getField(context, "experiments");
    var experiment = new com.marketinghub.experiment.Experiment();
    experiment.setId(94L);
    experiment.setProduct(com.marketinghub.product.Product.builder().id(7L).build());
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.INVALIDATED);
    when(experiments.findById(94L)).thenReturn(Optional.of(experiment));
    assertThat(context.dispatchBlockReason(run)).contains("#94", "não renove Plutus");
    definition.setProcessCode("pde-commercial-homologation-activation");
    assertThat(context.dispatchBlockReason(run)).contains("#94", "não renove Plutus");
    definition.setProcessCode("pde-sales-delivery-learning");
    assertThat(context.dispatchBlockReason(run)).isNull();
    definition.setProcessCode("experiment-homologation-activation");
    experiment.setStatus(com.marketinghub.experiment.ExperimentStatus.PAUSED);
    assertThat(context.dispatchBlockReason(run)).isNull();
    experiment.setProduct(com.marketinghub.product.Product.builder().id(97001L).build());
    assertThatThrownBy(() -> context.dispatchBlockReason(run)).hasMessageContaining("não pertence");
  }
}
