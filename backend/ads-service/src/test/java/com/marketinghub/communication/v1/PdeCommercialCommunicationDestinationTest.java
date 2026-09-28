package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: comprovar a reutilização auditável do destino PDE comercial no Processo 4. */
class PdeCommercialCommunicationDestinationTest {
  private static final Instant NOW = Instant.parse("2026-09-28T16:00:00Z");
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final Product product = Product.builder().id(10L).slug("pde-planejado-36").build();
  private final Experiment experiment = new Experiment();
  private final PdeProductionSlot slot =
      PdeProductionSlot.builder()
          .id(9L)
          .slotCode("v1")
          .productSlug("pde-planejado-36")
          .publicUrl("https://mira.digicomdigital.com.br")
          .experienceVersion("mira-commercial-v1")
          .status(PdeProductionSlotStatus.ACTIVE)
          .sourceExperimentId(93L)
          .publishedExperienceJson(
              """
              {"commercialBinding":{"experimentId":93},
               "commercialCheckout":{"checkoutUrl":"https://checkout.example/mira"}}
              """)
          .publishedAt(NOW.minusSeconds(3600))
          .validationStatus("OK")
          .build();
  private final PdeCommercialCommunicationDestination destination =
      new PdeCommercialCommunicationDestination(
          experiments, slots, instances, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

  /** Prepara a mesma oferta, produto e slot comercial para cada cenário. */
  @BeforeEach
  void setUp() {
    experiment.setId(93L);
    experiment.setProduct(product);
    experiment.setCommercialCheckoutUrl("https://checkout.example/mira");
    when(experiments.findById(93L)).thenReturn(Optional.of(experiment));
    when(slots.findFirstBySourceExperimentIdAndStatusInOrderByPublishedAtDesc(
            93L, java.util.List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE)))
        .thenReturn(Optional.of(slot));
    when(instances.save(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(call -> call.getArgument(0));
  }

  /** Conclui o destino com a identidade e o hash do slot sem abrir outra landing. */
  @Test
  void reconcilesPublishedCommercialSlotAsDestination() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(95L);
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(951L);
    activity.setActivityId("destination");
    activity.setProcessDefinition(process);

    assertThat(destination.readiness(product, "experiment:93").orElseThrow().ready()).isTrue();
    assertThat(
            destination.complete(process, activity, product, "experiment:93").objectiveAchieved())
        .isTrue();

    ArgumentCaptor<BusinessProcessActivityInstance> persisted =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).save(persisted.capture());
    assertThat(persisted.getValue().getStatus()).isEqualTo("COMPLETED");
    assertThat(persisted.getValue().getObjectiveEvidenceJson())
        .contains(PdeCommercialCommunicationDestination.EVIDENCE_TYPE)
        .contains("mira-commercial-v1")
        .contains("publishedContractSha256")
        .contains("\"mediaSpendAuthorized\":false");
  }

  /** Bloqueia a projeção quando o contrato publicado aponta para outro checkout. */
  @Test
  void rejectsContradictoryPublishedCheckout() {
    experiment.setCommercialCheckoutUrl("https://checkout.example/changed");

    var readiness = destination.readiness(product, "experiment:93").orElseThrow();

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("checkout canônico");
  }

  /**
   * Reabre uma conclusão quando o checkout corrente deixa de corresponder ao contrato publicado.
   */
  @Test
  void reopensCompletedDestinationWhenCheckoutBecomesContradictory() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(95L);
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(951L);
    activity.setActivityId("destination");
    activity.setProcessDefinition(process);
    destination.complete(process, activity, product, "experiment:93");
    ArgumentCaptor<BusinessProcessActivityInstance> persisted =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).save(persisted.capture());
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            951L, "experiment:93"))
        .thenReturn(Optional.of(persisted.getValue()));
    experiment.setCommercialCheckoutUrl("https://checkout.example/changed");

    assertThat(destination.requiresFreshExecution(activity, product, "experiment:93")).isTrue();
  }
}
