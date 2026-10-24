package com.marketinghub.safira.commercial.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.service.ExperimentTargetingSelectionService;
import com.marketinghub.experiment.service.IntegratedPdeJourneyEvidenceService;
import com.marketinghub.financialplan.v1.FinancialPlanRevision.Environment;
import com.marketinghub.financialplan.v1.service.FinancialPlanService;
import com.marketinghub.financialplan.v1.service.getplan.PlanView;
import com.marketinghub.financialplan.v1.service.saveplan.PlanAssumptions;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.PdeProductionSlotStatus;
import com.marketinghub.pde.service.PdeCommercialCheckoutContractResolver;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.productai.ProductAiSubtype;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: testar identidade, versão e fingerprints do percurso comercial Safira. */
class SafiraCommercialContextTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final PdeProductionSlotRepository slots = mock(PdeProductionSlotRepository.class);
  private final IntegratedPdeJourneyEvidenceService journey =
      mock(IntegratedPdeJourneyEvidenceService.class);
  private final PdeCommercialCheckoutContractResolver checkout =
      mock(PdeCommercialCheckoutContractResolver.class);
  private final CreativeRepository creatives = mock(CreativeRepository.class);
  private final ExperimentTargetingSelectionService targeting =
      mock(ExperimentTargetingSelectionService.class);
  private final FinancialPlanService finances = mock(FinancialPlanService.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final SafiraCommercialContext context =
      new SafiraCommercialContext(
          experiments,
          cycles,
          slots,
          journey,
          checkout,
          creatives,
          targeting,
          finances,
          plans,
          new ObjectMapper().findAndRegisterModules());
  private Product product;
  private Experiment experiment;
  private PdeProductionSlot slot;

  /** Monta uma candidata genérica sem depender do identificador real de Mira. */
  @BeforeEach
  void setup() {
    product =
        Product.builder()
            .id(10L)
            .slug("synthetic-ai-product")
            .validationDefinitionVersion("private-v1")
            .productTypeDefinition(
                ProductTypeDefinition.builder().code(SafiraCommercialContext.TYPE).build())
            .currentPriceBrl(new BigDecimal("49"))
            .pdeExperienceJson("{\"kind\":\"guided-routine\"}")
            .build();
    experiment = new Experiment();
    experiment.setId(301L);
    experiment.setProduct(product);
    experiment.setExperimentType(ExperimentType.LOW_TICKET_PRODUCT);
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    experiment.setStatus(ExperimentStatus.PLANNED);
    experiment.setUnitPrice(new BigDecimal("49"));
    slot =
        PdeProductionSlot.builder()
            .id(901L)
            .slotCode("safira-test")
            .productSlug(product.getSlug())
            .experienceVersion("public-v1")
            .status(PdeProductionSlotStatus.READY)
            .validationStatus("OK")
            .publicUrl("https://example.test/mira")
            .backendUrl("https://api.example.test/mira")
            .publishedExperienceJson("{\"version\":\"public-v1\"}")
            .sourceExperimentId(301L)
            .build();
    when(experiments.findById(301L)).thenReturn(Optional.of(experiment));
    when(cycles.findByExperimentId(301L)).thenReturn(Optional.empty());
    when(slots.findFirstBySourceExperimentIdOrderByUpdatedAtDesc(301L))
        .thenReturn(Optional.of(slot));
    when(plans.findByExperimentReference(301L)).thenReturn(List.of());
    when(finances.list(any(), any(), any())).thenReturn(List.of());
    when(creatives.findByExperimentId(301L)).thenReturn(List.of());
    when(checkout.resolve(any(), any())).thenReturn(Optional.empty());
  }

  /** Aceita somente o experimento explícito e preserva a versão pública sem alterar seu estado. */
  @Test
  void scopesExactAiProductExperimentWithoutPromotingPrivateValidation() {
    var scope = context.scope("experiment:301", 10L, true);

    assertThat(scope.productVersion()).isEqualTo("public-v1");
    assertThat(scope.cycleId()).isNull();
    var snapshot = context.snapshot("experiment:301");
    assertThat(snapshot.path("privateValidationReusedAsProductReference").asBoolean()).isTrue();
    assertThat(snapshot.path("humanEvidenceClaimed").asBoolean()).isFalse();
    assertThat(snapshot.path("commercialEvidenceClaimed").asBoolean()).isFalse();
    assertThat(snapshot.path("experienceHash").asText()).hasSize(64);
    verify(experiments, never()).save(any());
  }

  /** Seleciona a economia pelo contrato do produto sem confundi-lo com a versão pública do slot. */
  @Test
  void selectsFinancialPlanByProductContractVersionWhenRuntimeVersionDiffers() {
    var commercialPlan = mock(CommercialPlan.class);
    when(commercialPlan.getId()).thenReturn(81L);
    when(plans.findByExperimentReference(301L)).thenReturn(List.of(commercialPlan));
    var assumptions =
        new PlanAssumptions(
            "private-v1",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    var financialPlan =
        new PlanView(
            41L,
            "PRODUCT",
            10L,
            Environment.LIVE,
            "Plano sintético",
            3,
            null,
            81L,
            6,
            "teste",
            Instant.parse("2026-09-29T00:00:00Z"),
            assumptions,
            null,
            false,
            List.of(),
            false,
            null);
    when(finances.list("PRODUCT", 10L, Environment.LIVE)).thenReturn(List.of(financialPlan));

    var snapshot = context.snapshot("experiment:301");

    assertThat(snapshot.path("productVersion").asText()).isEqualTo("public-v1");
    assertThat(snapshot.path("financialPlan").path("id").asLong()).isEqualTo(41L);
    assertThat(snapshot.path("financialPlan").path("assumptions").path("productVersion").asText())
        .isEqualTo("private-v1");
  }

  /** Falha fechado para referência privada, subtipo ausente, outro produto e campanha ativa. */
  @Test
  void rejectsPrivateOrIncompatibleScopeBeforeMutation() {
    assertThatThrownBy(() -> context.scope("product:10@agent-validation-v1", 10L, true))
        .hasMessageContaining("experimento comercial exato");
    assertThatThrownBy(() -> context.scope("experiment:301", 11L, true))
        .hasMessageContaining("outro produto");
    experiment.setProductAiSubtype(null);
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, true))
        .hasMessageContaining("subtipo");
    experiment.setProductAiSubtype(ProductAiSubtype.AI_PERSONALIZED_SAMPLE);
    experiment.setStatus(ExperimentStatus.RUNNING);
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, true))
        .hasMessageContaining("operação");
    assertThat(context.scope("experiment:301", 10L, false)).isNotNull();
  }

  /** Não confunde referência encerrada com campanha ativa nem consulta economia para renová-la. */
  @Test
  void blocksTerminalAndExpiredPreparationBeforeLiveInputs() {
    experiment.setStatus(ExperimentStatus.INVALIDATED);
    experiment.setProductAiSubtype(null);
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, true))
        .hasMessageContaining("encerrado", "não renove Plutus");
    assertThat(context.historicalBlockReason("experiment:301", 10L)).contains("#301");
    assertThatThrownBy(() -> context.historicalBlockReason("experiment:301", 999L))
        .hasMessageContaining("outro produto");
    experiment.setStatus(ExperimentStatus.PAUSED);
    experiment.setEndDate(java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(1));
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, true))
        .hasMessageContaining("janela encerrada");
    org.mockito.Mockito.verifyNoInteractions(finances, slots, plans);
  }

  /** Recupera o ciclo pela referência e não exige vigência atual das fontes de um arquivo. */
  @Test
  void preservesClosedCycleIdentityAndOpenCandidateValidation() {
    var cycle = new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle();
    cycle.setId(702L);
    cycle.setProductId(10L);
    cycle.setStatus("ADJUSTED");
    when(cycles.findByExperimentId(301L)).thenReturn(Optional.of(cycle));
    assertThat(context.historicalBlockReason("experiment:301", 10L)).contains("#702", "encerrado");
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, true))
        .hasMessageContaining("encerrado");
    cycle.setProductId(999L);
    assertThatThrownBy(() -> context.historicalBlockReason("experiment:301", 10L))
        .hasMessageContaining("outro produto");
    cycle.setProductId(10L);
    cycle.setStatus("OPEN");
    cycle.setStage("AUTHORIZATION");
    assertThat(context.historicalBlockReason("experiment:301", 10L)).isNull();
    assertThat(context.scope("experiment:301", 10L, true).cycleId()).isEqualTo(702L);
  }

  /** Recusa um slot que tente misturar outro produto, experimento ou versão comercial. */
  @Test
  void rejectsIncompatiblePublicSlotIdentity() {
    slot.setProductSlug("another-product");
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, false))
        .hasMessageContaining("outro produto ou experimento");

    slot.setProductSlug(product.getSlug());
    slot.setSourceExperimentId(999L);
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, false))
        .hasMessageContaining("outro produto ou experimento");

    slot.setSourceExperimentId(301L);
    slot.setExperienceVersion(" ");
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, false))
        .hasMessageContaining("versão comercial");
  }

  /** Reproduz Mira e outra identidade com retorno ao ciclo, sem executar a preparação prematura. */
  @Test
  void unfinishedCycleNavigatesToItsOwnStageWithoutWrites() {
    for (long[] ids : List.of(new long[] {10, 102, 9, 26}, new long[] {44, 710, 71, 88})) {
      product.setId(ids[0]);
      experiment.setId(ids[1]);
      var cycle = new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle();
      cycle.setId(ids[2]);
      cycle.setProductId(ids[0]);
      cycle.setExperimentId(ids[1]);
      cycle.setChainDefinitionId(ids[3]);
      cycle.setStatus("OPEN");
      when(experiments.findById(ids[1])).thenReturn(Optional.of(experiment));
      when(cycles.findByExperimentId(ids[1])).thenReturn(Optional.of(cycle));
      for (String stage : List.of("VIDEO_APPROVAL", "PLANNING", "VALIDATION")) {
        cycle.setStage(stage);
        assertThatThrownBy(() -> context.scope("experiment:" + ids[1], ids[0], true))
            .isInstanceOfSatisfying(
                SafiraCommercialBlocker.class,
                blocker -> {
                  assertThat(blocker.getMessage())
                      .contains("O ciclo #" + ids[2], "Conclua essa etapa");
                  assertThat(blocker.actionLabel()).isEqualTo("Abrir etapa atual do ciclo");
                  assertThat(blocker.navigationUrl())
                      .isEqualTo(
                          "/business-process-chains/learning-cycles?productId="
                              + ids[0]
                              + "&chainId="
                              + ids[3]
                              + "&cycleId="
                              + ids[2]);
                });
      }
      assertThatThrownBy(() -> context.scope("experiment:" + ids[1], ids[0] + 1, true))
          .isNotInstanceOf(SafiraCommercialBlocker.class)
          .hasMessageContaining("outro produto");
    }
    verify(experiments, never()).save(any());
    verify(cycles, never()).save(any());
  }

  /** Preserva a candidata antes válida nas duas etapas comerciais e não altera o ciclo. */
  @Test
  void commercialStagesRemainEligibleWithExactCycleIdentity() {
    var cycle = new com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle();
    cycle.setId(71L);
    cycle.setProductId(product.getId());
    cycle.setExperimentId(experiment.getId());
    cycle.setChainDefinitionId(88L);
    cycle.setProductVersion(slot.getExperienceVersion());
    cycle.setStatus("OPEN");
    when(cycles.findByExperimentId(experiment.getId())).thenReturn(Optional.of(cycle));
    for (String stage : List.of("AUTHORIZATION", "PUBLICATION")) {
      cycle.setStage(stage);
      var scope = context.scope("experiment:301", product.getId(), true);
      assertThat(scope.cycleId()).isEqualTo(71L);
      assertThat(scope.chainId()).isEqualTo(88L);
      assertThat(cycle.getStage()).isEqualTo(stage);
    }
    verify(cycles, never()).save(any());
  }

  /** Isola a mudança econômica da jornada e uma mudança visual da economia. */
  @Test
  void fingerprintsPreparationActivitiesByTheirOwnSources() {
    var initial = context.snapshot("experiment:301");
    var financeChange = initial.deepCopy();
    financeChange.putObject("financialPlan").put("revision", 2);
    assertThat(SafiraCommercialContext.activityFingerprint("journey", financeChange))
        .isEqualTo(SafiraCommercialContext.activityFingerprint("journey", initial));
    assertThat(SafiraCommercialContext.activityFingerprint("economics", financeChange))
        .isNotEqualTo(SafiraCommercialContext.activityFingerprint("economics", initial));

    var visualChange = initial.deepCopy();
    visualChange.put("experienceHash", "b".repeat(64));
    assertThat(SafiraCommercialContext.activityFingerprint("journey", visualChange))
        .isNotEqualTo(SafiraCommercialContext.activityFingerprint("journey", initial));
    assertThat(SafiraCommercialContext.activityFingerprint("economics", visualChange))
        .isEqualTo(SafiraCommercialContext.activityFingerprint("economics", initial));
  }

  /** Recusa canal direto e invalida provas ao alterar controles da candidata paga. */
  @Test
  void rejectsDirectPilotAndInvalidatesChangedPaidControls() {
    experiment.setPlatform(ExperimentPlatform.DIRECT_ONE_TO_ONE);
    assertThatThrownBy(() -> context.scope("experiment:301", 10L, true))
        .hasMessageContaining("aquisição paga no Instagram");
    experiment.setPlatform(ExperimentPlatform.FACEBOOK);
    var initial = context.snapshot("experiment:301");
    for (String field : List.of("platform", "sampleSize", "dailyBudgetBrl", "mediaSpendLimitBrl")) {
      var changed = initial.deepCopy();
      changed.put(field, "changed");
      for (String activity : List.of("journey", "economics")) {
        assertThat(SafiraCommercialContext.activityFingerprint(activity, changed))
            .isNotEqualTo(SafiraCommercialContext.activityFingerprint(activity, initial));
      }
    }
    verify(experiments, never()).save(any());
  }
}
