package com.marketinghub.product.service.salesjourneyintegration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.product.service.valuechainposition.ProductStageMeasurementResolver;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: comprovar o gate auditável que leva o Rigel ao processo de homologação. */
class PdeSalesJourneyIntegrationActivityExecutorTest {
  private static final Instant NOW = Instant.parse("2026-08-28T14:00:00Z");
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository definitions =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductStageMeasurementResolver measurements =
      mock(ProductStageMeasurementResolver.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final PdeProductionSlotService slotService = mock(PdeProductionSlotService.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProductProcessPeriodService periods = mock(ProductProcessPeriodService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private PdeSalesJourneyIntegrationActivityExecutor executor;
  private BusinessProcessDefinition process;
  private BusinessProcessActivityDefinition integration;
  private Product rigel;
  private Experiment experiment;
  private CommercialPlan plan;
  private PdeProductionSlot slot;

  /** Monta o processo 4 completo com as fontes persistidas do Rigel. */
  @BeforeEach
  void setUp() {
    executor =
        new PdeSalesJourneyIntegrationActivityExecutor(
            plans,
            experiments,
            processes,
            definitions,
            tasks,
            instances,
            measurements,
            slots,
            slotService,
            products,
            periods,
            objectMapper,
            Clock.fixed(NOW, ZoneOffset.UTC));
    process = process(55L, "pde-communication-sales-journey", "PUBLISHED");
    process.setDiagramJson(
        """
        {"nodes":[
          {"id":"creatives","type":"TASK","label":"Produzir criativos","subprocessCode":"creative-production-approval"},
          {"id":"destination","type":"TASK","label":"Construir landing","subprocessCode":"landing-page-generation"},
          {"id":"integration","type":"TASK","label":"Integrar canal, checkout, acesso e eventos"}
        ]}
        """);
    integration = activity(175L, process, "integration");
    rigel =
        Product.builder()
            .id(9L)
            .slug("kit-whatsapp-pronto")
            .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
            .commercialStatus("COMUNICACAO_E_JORNADA")
            .build();
    experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(rigel);
    plan = new CommercialPlan();
    plan.setId(4L);
    slot =
        PdeProductionSlot.builder()
            .id(7L)
            .slotCode("v1")
            .productSlug("kit-whatsapp-pronto")
            .domain("kit-whatsapp-pronto.digicomdigital.com.br")
            .publicUrl("https://kit-whatsapp-pronto.digicomdigital.com.br")
            .backendUrl("https://kit-whatsapp-pronto.digicomdigital.com.br/api")
            .experienceVersion("kit-whatsapp-pronto-pde-v2")
            .layoutKey("assisted-service")
            .targetEnvironment("production-v1")
            .status(PdeProductionSlotStatus.ACTIVE)
            .sourceExperimentId(89L)
            .publishedExperienceJson("{\"version\":\"v2\"}")
            .publishedAt(NOW.minusSeconds(3600))
            .build();

    when(experiments.findByProductIdOrderByUpdatedAtDescIdDesc(9L)).thenReturn(List.of(experiment));
    when(plans.findByExperimentReference(89L)).thenReturn(List.of(plan));
    when(plans.findByProductId(9L)).thenReturn(List.of(plan));
    when(tasks.findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc("commercial-plan:4@"))
        .thenReturn(List.of());
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("experiment:89"))
        .thenReturn(List.of());
    BusinessProcessDefinition creatives = process(41L, "creative-production-approval", "PUBLISHED");
    BusinessProcessDefinition landing = process(18L, "landing-page-generation", "PUBLISHED");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "creative-production-approval", "PUBLISHED"))
        .thenReturn(Optional.of(creatives));
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "landing-page-generation", "PUBLISHED"))
        .thenReturn(Optional.of(landing));
    when(measurements.objectiveAchieved(rigel, creatives)).thenReturn(true);
    when(measurements.objectiveAchieved(rigel, landing)).thenReturn(true);
    when(definitions.findByProcessDefinitionIdAndActivityId(55L, "creatives"))
        .thenReturn(Optional.of(activity(173L, process, "creatives")));
    when(definitions.findByProcessDefinitionIdAndActivityId(55L, "destination"))
        .thenReturn(Optional.of(activity(174L, process, "destination")));
    when(slots.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(89L))
        .thenReturn(Optional.of(slot));
    when(instances.saveAndFlush(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(instances.save(any(BusinessProcessActivityInstance.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  /** Conclui a integração, registra evidência e move o produto para o processo 5. */
  @Test
  void completesPreparedJourneyAndAdvancesProduct() {
    when(slotService.validateProductionSlot("kit-whatsapp-pronto", "v1"))
        .thenReturn(validatedSlot("OK", "Jornada íntegra"));

    var result = executor.execute(process, integration, rigel, "commercial-plan:4@v3:journey");

    assertThat(result.operationalState()).isEqualTo("COMPLETED");
    assertThat(result.objectiveAchieved()).isTrue();
    assertThat(rigel.getCommercialStatus()).isEqualTo("VALIDACAO_COMERCIAL");
    assertThat(experiment.getFollowUpActionUrl())
        .isEqualTo("https://kit-whatsapp-pronto.digicomdigital.com.br");
    verify(products).save(rigel);
    verify(periods).recordTransition(rigel, "COMUNICACAO_E_JORNADA");
    ArgumentCaptor<BusinessProcessActivityInstance> persisted =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(persisted.capture());
    assertThat(persisted.getValue().getOccurrenceNumber()).isEqualTo(1);
    assertThat(persisted.getValue().getSourceReference()).isEqualTo("commercial-plan:4@v3:journey");
    ArgumentCaptor<BusinessProcessActivityInstance> completed =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances, org.mockito.Mockito.atLeastOnce()).save(completed.capture());
    assertThat(completed.getAllValues())
        .anySatisfy(
            instance -> {
              assertThat(instance.getActivityDefinition().getActivityId()).isEqualTo("integration");
              assertThat(instance.getStatus()).isEqualTo("COMPLETED");
              assertThat(instance.getObjectiveEvidenceJson())
                  .contains("PDE_SALES_JOURNEY_INTEGRATION_V1")
                  .contains("\"publicationAuthorized\":false")
                  .contains("\"mediaSpendAuthorized\":false")
                  .contains("\"testPaymentExecuted\":false");
            });
  }

  /** Persiste a falha atual da URL pública sem avançar produto nem mascarar o bloqueio. */
  @Test
  void blocksWhenPublicJourneyValidationFails() {
    when(slotService.validateProductionSlot("kit-whatsapp-pronto", "v1"))
        .thenReturn(validatedSlot("FAILED", "Health público não respondeu como UP"));

    var result = executor.execute(process, integration, rigel, "commercial-plan:4@v3:journey");

    assertThat(result.operationalState()).isEqualTo("BLOCKED");
    assertThat(result.objectiveAchieved()).isFalse();
    assertThat(result.message()).contains("Health público não respondeu como UP");
    assertThat(rigel.getCommercialStatus()).isEqualTo("COMUNICACAO_E_JORNADA");
    verify(products, never()).save(any(Product.class));
    verify(periods, never()).recordTransition(any(Product.class), any());
  }

  /** Exige a comunicação de Íris antes de integrar uma nova versão do processo comercial. */
  @Test
  void blocksVersionWithMissingCommunicationContract() {
    process.setDiagramJson(
        """
        {"nodes":[
          {"id":"communicationContract","type":"TASK","label":"Materializar comunicação","responsibleAgentKeys":["communication-agent"]},
          {"id":"integration","type":"TASK","label":"Integrar canal, checkout, acesso e eventos"}
        ]}
        """);

    var readiness = executor.readiness(process, integration, rigel, "commercial-plan:4@v4:journey");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("Materializar comunicação");
    verify(slotService, never()).validateProductionSlot(any(), any());
  }

  /** Impede que uma rota antiga regrida produto que já está em outro processo comercial. */
  @Test
  void blocksExecutionFromHistoricalProcessRoute() {
    rigel.setCommercialStatus("OPERACAO_E_OTIMIZACAO");

    var readiness = executor.readiness(process, integration, rigel, "commercial-plan:4@v3:journey");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("rota histórica");
    verify(slotService, never()).validateProductionSlot(any(), any());
  }

  /** Cria uma versão de processo suficiente para os testes determinísticos. */
  private BusinessProcessDefinition process(long id, String code, String status) {
    BusinessProcessDefinition definition = new BusinessProcessDefinition();
    definition.setId(id);
    definition.setProcessCode(code);
    definition.setStatus(status);
    definition.setName(code);
    definition.setVersionNumber(1);
    definition.setDiagramJson("{\"nodes\":[]}");
    return definition;
  }

  /** Cria uma atividade relacional vinculada à versão do processo. */
  private BusinessProcessActivityDefinition activity(
      long id, BusinessProcessDefinition owner, String activityId) {
    BusinessProcessActivityDefinition definition = new BusinessProcessActivityDefinition();
    definition.setId(id);
    definition.setProcessDefinition(owner);
    definition.setActivityId(activityId);
    definition.setName(activityId);
    definition.setDefinitionJson("{}");
    return definition;
  }

  /** Monta o retorno atual da validação pública do slot. */
  private PostDeployPdeProductionSlotDto validatedSlot(String status, String summary) {
    return new PostDeployPdeProductionSlotDto(
        7L,
        "v1",
        "kit-whatsapp-pronto",
        "kit-whatsapp-pronto.digicomdigital.com.br",
        "https://kit-whatsapp-pronto.digicomdigital.com.br",
        "https://kit-whatsapp-pronto.digicomdigital.com.br/api",
        "kit-whatsapp-pronto-pde-v2",
        "assisted-service",
        "production-v1",
        PdeProductionSlotStatus.ACTIVE,
        89L,
        null,
        null,
        "{\"version\":\"v2\"}",
        "pipeline",
        NOW.minusSeconds(3600),
        status,
        NOW,
        "OK".equals(status) ? 200 : null,
        summary,
        summary,
        "kit-whatsapp-pronto",
        "/",
        "https://kit-whatsapp-pronto.digicomdigital.com.br",
        NOW.minusSeconds(7200),
        NOW);
  }
}
