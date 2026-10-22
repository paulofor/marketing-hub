package com.marketinghub.communication.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.*;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.*;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.service.IntegratedPdeJourneyEvidenceService;
import com.marketinghub.pde.*;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.salesjourneyintegration.PdeSalesJourneyIntegrationActivityExecutor;
import com.marketinghub.product.service.valuechainposition.*;
import com.marketinghub.repository.jpa.agenttask.*;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: comprovar a passagem inicial pelos executores reais sem promover aceites
 * privados.
 */
class InitialExperimentCommercialJourneyTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final List<BusinessProcessActivityInstance> history = new ArrayList<>();
  private final Product product =
      Product.builder()
          .id(11L)
          .slug("produto-local")
          .commercialStatus("COMUNICACAO_E_JORNADA")
          .publicUrl("https://local.example/paid")
          .build();
  private final Experiment experiment = new Experiment();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final Map<String, BusinessProcessActivityDefinition> activities = new HashMap<>();
  private final PdeProductionSlot slot =
      PdeProductionSlot.builder()
          .id(10L)
          .slotCode("paid-v1")
          .productSlug("produto-local")
          .experienceVersion("paid-v1")
          .status(PdeProductionSlotStatus.READY)
          .publicUrl("https://local.example/paid")
          .backendUrl("https://local.example/api")
          .publishedAt(Instant.parse("2026-10-22T00:00:00Z"))
          .validationStatus("OK")
          .build();
  private final PdeCommercialCommunicationDestination destination =
      new PdeCommercialCommunicationDestination(experiments, slots, instances, json);
  private final IntegratedPdeJourneyEvidenceService evidence =
      new IntegratedPdeJourneyEvidenceService(slots, plans, instances, json);
  private final InitialExperimentCommercialJourney phase =
      new InitialExperimentCommercialJourney(experiments, cycles, destination, evidence, instances);
  private String reference;
  private PrivateCommunicationJourney privateJourney;
  private CommunicationMaterializationContextProvider context;

  /**
   * Configura fontes sintéticas e os mesmos consumidores usados pela tela, pelo BPM e por Safira.
   */
  private void fixture(long productId, long experimentId) {
    reference = "experiment:" + experimentId;
    product.setId(productId);
    experiment.setId(experimentId);
    experiment.setProduct(product);
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setCommercialCheckoutUrl("https://checkout.example/offer");
    slot.setSourceExperimentId(experimentId);
    slot.setPublishedExperienceJson(
        "{\"commercialBinding\":{\"experimentId\":"
            + experimentId
            + "},\"commercialCheckout\":{\"checkoutUrl\":\"https://checkout.example/offer\"}}");
    when(experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    when(slots.findFirstBySourceExperimentIdAndStatusInOrderByPublishedAtDesc(
            eq(experimentId), anyList()))
        .thenAnswer(
            i ->
                List.of(PdeProductionSlotStatus.READY, PdeProductionSlotStatus.ACTIVE)
                        .contains(slot.getStatus())
                    ? Optional.of(slot)
                    : Optional.empty());
    when(slots.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(experimentId))
        .thenReturn(Optional.of(slot));
    process.setId(113L);
    process.setProcessCode("pde-communication-sales-journey");
    process.setDiagramJson(
        "{\"nodes\":[{\"id\":\"communicationContract\",\"type\":\"TASK\"},{\"id\":\"creatives\",\"type\":\"TASK\"},{\"id\":\"destination\",\"type\":\"TASK\"},{\"id\":\"integration\",\"type\":\"TASK\"}]}");
    for (String code :
        List.of("communicationContract", "creatives", "destination", "integration")) {
      var activity = new BusinessProcessActivityDefinition();
      activity.setId(1000L + activities.size());
      activity.setActivityId(code);
      activity.setProcessDefinition(process);
      activities.put(code, activity);
      var old = new BusinessProcessActivityInstance();
      old.setId(2000L + history.size());
      old.setActivityDefinition(activity);
      old.setSourceReference(reference);
      old.setOccurrenceNumber(1);
      old.setStatus("COMPLETED");
      old.setObjectiveAchieved(true);
      old.setObjectiveEvidenceJson("{\"journeyMode\":\"PRIVATE\"}");
      history.add(old);
    }
    org.mockito.stubbing.Answer<Optional<BusinessProcessActivityInstance>> latest =
        i ->
            history.stream()
                .filter(v -> v.getActivityDefinition().getId().equals(i.getArgument(0)))
                .max(Comparator.comparing(BusinessProcessActivityInstance::getOccurrenceNumber));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq(reference)))
        .thenAnswer(latest);
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), eq(reference)))
        .thenAnswer(latest);
    org.mockito.stubbing.Answer<BusinessProcessActivityInstance> save =
        i -> {
          BusinessProcessActivityInstance value = i.getArgument(0);
          if (value.getId() == null) {
            value.setId(2000L + history.size());
            history.add(value);
          }
          return value;
        };
    when(instances.save(any())).thenAnswer(save);
    when(instances.saveAndFlush(any())).thenAnswer(save);
    when(instances
            .findAllByActivityDefinitionProcessDefinitionIdAndSourceReferenceOrderByActivityDefinitionIdAscOccurrenceNumberAsc(
                process.getId(), reference))
        .thenAnswer(i -> List.copyOf(history));
    when(instances
            .findAllByActivityDefinitionProcessDefinitionProcessCodeAndSourceReferenceOrderByCreatedAtDescIdDesc(
                process.getProcessCode(), reference))
        .thenAnswer(i -> List.copyOf(history));
    var plan = new CommercialPlan();
    plan.setId(34L);
    when(plans.findByExperimentReference(experimentId)).thenReturn(List.of(plan));
    context = mock(CommunicationMaterializationContextProvider.class);
    when(context.resolve(reference))
        .thenReturn(
            Optional.of(
                Map.of(
                    "mode",
                    IrisCommunicationMaterializationContextProvider.INITIAL_EXPERIMENT_PRIVATE_MODE,
                    "inputReadiness",
                    "READY",
                    "sourceReference",
                    reference,
                    "product",
                    Map.of("id", productId),
                    "experiment",
                    Map.of("id", experimentId),
                    "communicationInputHash",
                    "a".repeat(64))));
    privateJourney =
        new PrivateCommunicationJourney(
            mock(IrisLearningCycleContext.class),
            cycles,
            new ProductProcessActivityPredecessorService(tasks, instances, json),
            instances,
            tasks,
            new PrivateCommunicationCreativeProof(tasks, instances, json),
            json);
    ReflectionTestUtils.setField(
        privateJourney,
        "initialExperiments",
        new InitialExperimentPrivateJourneyContext(context, experiments, cycles, json));
    ReflectionTestUtils.setField(privateJourney, "initialCommercialJourney", phase);
  }

  /**
   * Percorre destino, integração, replay e leitura de Safira com duas identidades independentes.
   */
  @ParameterizedTest
  @CsvSource({"11,97", "240,9701"})
  void preparesCommercialJourneyWithoutReopeningPrivateApprovals(
      long productId, long experimentId) {
    fixture(productId, experimentId);
    var route =
        new CommunicationDestinationActivityExecutor(privateJourney, processes, destination);
    var integrationActivity = activities.get("integration");
    assertThat(phase.applies(reference)).isTrue();
    assertThat(phase.requiresFreshExecution(process, integrationActivity, product, reference))
        .isTrue();
    assertThat(evidence.isReady(experiment)).isFalse();
    assertThat(
            route
                .readiness(process, activities.get("destination"), product, reference)
                .actionLabel())
        .isEqualTo("Reconciliar destino publicado");
    assertThat(
            route
                .execute(process, activities.get("destination"), product, reference)
                .objectiveAchieved())
        .isTrue();
    var iris =
        new IrisProductProcessActivityReadinessProvider(
            MarketStrategicContextProvider.empty(), context);
    ReflectionTestUtils.setField(iris, "tasks", tasks);
    ReflectionTestUtils.setField(iris, "initialCommercialJourney", phase);
    assertThat(
            iris.requiresFreshExecution(
                process, activities.get("communicationContract"), product, reference))
        .isFalse();
    var validator = mock(PdeProductionSlotService.class);
    var validated = mock(PostDeployPdeProductionSlotDto.class);
    when(validated.validationStatus()).thenReturn("OK");
    when(validated.sourceExperimentId()).thenReturn(experimentId);
    when(validated.validationContractSlug()).thenReturn(product.getSlug());
    when(validated.validationResolvedUrl()).thenReturn(slot.getPublicUrl());
    when(validator.validateProductionSlot(product.getSlug(), slot.getSlotCode()))
        .thenReturn(validated);
    var executor =
        new PdeSalesJourneyIntegrationActivityExecutor(
            plans,
            experiments,
            processes,
            mock(BusinessProcessActivityDefinitionRepository.class),
            tasks,
            instances,
            mock(ProductStageMeasurementResolver.class),
            slots,
            validator,
            mock(ProductRepository.class),
            mock(ProductProcessPeriodService.class),
            json);
    ReflectionTestUtils.setField(executor, "privateJourney", privateJourney);
    assertThat(
            executor.execute(process, integrationActivity, product, reference).objectiveAchieved())
        .isTrue();
    assertThat(evidence.isReady(experiment)).isTrue();
    assertThat(phase.requiresFreshExecution(process, integrationActivity, product, reference))
        .isFalse();
    assertThat(
            executor.execute(process, integrationActivity, product, reference).objectiveAchieved())
        .isTrue();
    assertThat(history).hasSize(6);
    assertThat(history.getLast().getObjectiveEvidenceJson())
        .contains(
            "PDE_SALES_JOURNEY_INTEGRATION_V1",
            "\"publicationAuthorized\":false",
            "\"mediaSpendAuthorized\":false");
    assertThat(history.getLast().getKnownCostUsd()).isZero();
    assertThat(history.get(3).getObjectiveEvidenceJson())
        .isEqualTo("{\"journeyMode\":\"PRIVATE\"}");
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
    verify(validator).validateProductionSlot(product.getSlug(), slot.getSlotCode());
  }

  /** Preserva o percurso anterior sem antecipar fases, ciclos ou decisões encerradas. */
  @ParameterizedTest
  @ValueSource(strings = {"candidate", "cycle", "successor", "stopped", "finished"})
  void doesNotAdvanceAnIneligibleReference(String reason) {
    fixture(11, 97);
    switch (reason) {
      case "candidate" -> slot.setStatus(PdeProductionSlotStatus.CANDIDATE);
      case "cycle" ->
          when(cycles.findByExperimentId(97L))
              .thenReturn(
                  Optional.of(
                      new com.marketinghub.businessprocesschain.learningcycle.v1
                          .LearningSalesCycle()));
      case "successor" -> experiment.setSourceExperiment(new Experiment());
      case "stopped" -> experiment.setStatus(ExperimentStatus.USER_STOPPED);
      case "finished" -> experiment.setStatus(ExperimentStatus.FINISHED);
      default -> throw new IllegalArgumentException(reason);
    }
    assertThat(phase.applies(reference)).isFalse();
    assertThat(
            phase.requiresFreshExecution(
                process, activities.get("integration"), product, reference))
        .isFalse();
    verify(instances, never()).save(any());
    if (reason.equals("candidate")) assertThat(privateJourney.applies(reference)).isTrue();
  }

  /** Um destino incoerente permanece bloqueado e nunca volta silenciosamente à prova privada. */
  @ParameterizedTest
  @ValueSource(strings = {"checkout", "product", "validation"})
  void blocksInconsistentCommercialDestination(String reason) {
    fixture(11, 97);
    switch (reason) {
      case "checkout" -> experiment.setCommercialCheckoutUrl("https://checkout.example/changed");
      case "product" -> slot.setProductSlug("outro-produto");
      case "validation" -> slot.setValidationStatus("ERROR");
      default -> throw new IllegalArgumentException(reason);
    }
    var route =
        new CommunicationDestinationActivityExecutor(privateJourney, processes, destination);
    assertThat(privateJourney.applies(reference)).isFalse();
    assertThat(route.readiness(process, activities.get("destination"), product, reference).ready())
        .isFalse();
    assertThatThrownBy(
            () -> route.execute(process, activities.get("destination"), product, reference))
        .isInstanceOf(IllegalStateException.class);
    assertThat(phase.readiness(process, activities.get("integration"), product, reference).ready())
        .isFalse();
    verify(instances, never()).save(any());
  }

  /** Uma referência privada de produto não se converte em experimento comercial. */
  @Test
  void preservesPrivateProductReference() {
    fixture(11, 97);
    assertThat(phase.applies("product:11@agent-validation-v1")).isFalse();
    assertThat(privateJourney.applies("product:11@agent-validation-v1")).isTrue();
  }
}
