package com.marketinghub.opala.commercial.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.targeting.TargetingElementRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Responsabilidade: impedir contaminação de produto, experimento e versão sem bloquear leitura
 * histórica.
 */
class OpalaCommercialContextTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final OpalaCommercialContext context =
      new OpalaCommercialContext(
          cycles,
          experiments,
          slots,
          mock(ExperimentVideoAssetRepository.class),
          mock(TargetingElementRepository.class),
          new ObjectMapper(),
          mock(CreativeRepository.class),
          mock(ExperimentTargetingSelectionService.class));
  private final Product product =
      Product.builder()
          .id(4L)
          .slug("fixture-opala")
          .productTypeDefinition(ProductTypeDefinition.builder().code("PDE").build())
          .build();
  private final Experiment experiment =
      Experiment.builder().id(92L).product(product).status(ExperimentStatus.PLANNED).build();
  private final LearningSalesCycle cycle = new LearningSalesCycle();

  /** Prepara identidade e três slots com versões ou experimentos distintos. */
  @BeforeEach
  void setup() {
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setProductVersion("fixture-v12");
    cycle.setStatus("OPEN");
    cycle.setStage("PUBLICATION");
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));
    when(experiments.findById(92L)).thenReturn(Optional.of(experiment));
    when(slots.findByProductSlugOrderBySlotCodeAsc("fixture-opala"))
        .thenReturn(
            List.of(
                slot(1L, 92L, "fixture-v12"),
                slot(2L, 93L, "fixture-v12"),
                slot(3L, 92L, "fixture-v11")));
  }

  /** O contexto dos agentes contém somente a entrada exata da ocorrência. */
  @Test
  void selectsOnlySameExperimentAndVersion() {
    assertThat(context.scope("experiment:92").experiment()).isSameAs(experiment);
    var selected = context.snapshot("experiment:92").path("slots");
    assertThat(selected.size()).isEqualTo(1);
    assertThat(selected.get(0).path("id").asLong()).isEqualTo(1L);
  }

  /** Nome comercial ou nome mineral não substituem o tipo oficial e a identidade do produto. */
  @Test
  void rejectsWrongTypeAndProduct() {
    product.getProductTypeDefinition().setCode("OTHER");
    assertThatThrownBy(() -> context.scope("experiment:92"))
        .hasMessageContaining("tipo cadastrado");
    product.getProductTypeDefinition().setCode("PDE");
    cycle.setProductId(9L);
    assertThatThrownBy(() -> context.scope("experiment:92")).hasMessageContaining("outro produto");
    assertThatThrownBy(() -> context.scope("product:4")).hasMessageContaining("experimento exato");
  }

  /** Depois da publicação, o histórico continua legível enquanto novas mutações são recusadas. */
  @Test
  void readsAfterReleaseWithoutAuthorizingMoreWork() {
    experiment.setFacebookReleaseRequestedAt(java.time.Instant.now());
    assertThat(context.snapshot("experiment:92").path("productVersion").asText())
        .isEqualTo("fixture-v12");
    assertThatThrownBy(() -> context.scope("experiment:92"))
        .hasMessageContaining("ainda não liberado");
    cycle.setStatus("CLOSED");
    assertThat(context.snapshot("experiment:92").path("cycleId").asLong()).isEqualTo(2L);
    assertThatThrownBy(() -> context.scope("experiment:92"))
        .hasMessageContaining("preparação comercial");
  }

  /** Cria um slot sintético sem URL pública real nem implantação. */
  private PdeProductionSlot slot(Long id, Long experimentId, String version) {
    var slot = new PdeProductionSlot();
    slot.setId(id);
    slot.setSourceExperimentId(experimentId);
    slot.setExperienceVersion(version);
    slot.setStatus(PdeProductionSlotStatus.PLANNED);
    return slot;
  }
}
