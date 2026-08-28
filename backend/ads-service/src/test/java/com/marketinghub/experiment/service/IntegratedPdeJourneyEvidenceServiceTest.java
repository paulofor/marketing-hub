package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar a prova persistida que substitui gates legados na jornada PDE. */
class IntegratedPdeJourneyEvidenceServiceTest {
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final IntegratedPdeJourneyEvidenceService service =
      new IntegratedPdeJourneyEvidenceService(slots, plans, instances, new ObjectMapper());

  private Experiment experiment;
  private PdeProductionSlot slot;

  /** Monta Rigel, slot e plano canônicos usados por todas as provas. */
  @BeforeEach
  void setUp() {
    Product rigel = Product.builder().id(9L).slug("kit-whatsapp-pronto").build();
    experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(rigel);
    slot =
        PdeProductionSlot.builder()
            .id(7L)
            .productSlug("kit-whatsapp-pronto")
            .status(PdeProductionSlotStatus.ACTIVE)
            .validationStatus("OK")
            .build();
    CommercialPlan plan = new CommercialPlan();
    plan.setId(4L);
    when(slots.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(89L))
        .thenReturn(Optional.of(slot));
    when(plans.findByExperimentReference(89L)).thenReturn(List.of(plan));
  }

  /** Aceita somente conclusão funcional com IDs coincidentes e sem autorização comercial. */
  @Test
  void recognizesMatchingCompletedIntegrationEvidence() {
    BusinessProcessActivityInstance instance =
        integrationInstance(
            "COMPLETED",
            true,
            """
            {"evidenceType":"PDE_SALES_JOURNEY_INTEGRATION_V1","experimentId":89,
             "productId":9,"slotId":7,"publicationAuthorized":false,
             "mediaSpendAuthorized":false}
            """);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-communication-sales-journey", "experiment:89"))
        .thenReturn(List.of());
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceStartingWithOrderByCreatedAtDescIdDesc(
                "pde-communication-sales-journey", "commercial-plan:4@"))
        .thenReturn(List.of(instance));

    assertThat(service.appliesTo(experiment)).isTrue();
    assertThat(service.isReady(experiment)).isTrue();
  }

  /** Rejeita evidência de outro slot ou que tenha autorizado publicação indevidamente. */
  @Test
  void rejectsMismatchedOrExternallyAuthorizedEvidence() {
    BusinessProcessActivityInstance instance =
        integrationInstance(
            "COMPLETED",
            true,
            """
            {"evidenceType":"PDE_SALES_JOURNEY_INTEGRATION_V1","experimentId":89,
             "productId":9,"slotId":8,"publicationAuthorized":true,
             "mediaSpendAuthorized":false}
            """);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                "pde-communication-sales-journey", "experiment:89"))
        .thenReturn(List.of(instance));

    assertThat(service.isReady(experiment)).isFalse();
  }

  /** Monta uma ocorrência vinculada à atividade de integração do processo correto. */
  private BusinessProcessActivityInstance integrationInstance(
      String status, boolean objectiveAchieved, String evidence) {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("pde-communication-sales-journey");
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setProcessDefinition(process);
    activity.setActivityId("integration");
    BusinessProcessActivityInstance instance = new BusinessProcessActivityInstance();
    instance.setId(301L);
    instance.setActivityDefinition(activity);
    instance.setStatus(status);
    instance.setObjectiveAchieved(objectiveAchieved);
    instance.setObjectiveEvidenceJson(evidence);
    return instance;
  }
}
