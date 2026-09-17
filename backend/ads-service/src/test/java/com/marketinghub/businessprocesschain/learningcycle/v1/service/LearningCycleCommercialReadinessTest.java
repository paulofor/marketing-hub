package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentRunningGateRequirementDto;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: impedir revisão sem insumos, autorização implícita e contaminação entre
 * produtos.
 */
class LearningCycleCommercialReadinessTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentReadinessService gate = mock(ExperimentReadinessService.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository slots =
      mock(com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository.class);
  private final LearningCycleCommercialReadiness readiness =
      new LearningCycleCommercialReadiness(experiments, gate, slots);
  private final com.marketinghub.pde.PdeProductionSlot slot =
      new com.marketinghub.pde.PdeProductionSlot();
  private final LearningCycleCommercialReviewReadiness provider =
      new LearningCycleCommercialReviewReadiness(cycles, readiness);
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Product product = Product.builder().id(4L).build();
  private final Experiment experiment =
      Experiment.builder().id(92L).product(product).platform(ExperimentPlatform.FACEBOOK).build();
  private final List<String> codes =
      List.of(
          "LANDING_APPROVED",
          "CREATIVE_APPROVED",
          "CHECKOUT_READY",
          "INSTRUMENTATION_READY",
          "TARGETING_READY");
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final BusinessProcessActivityDefinition activity =
      new BusinessProcessActivityDefinition();

  /** Reproduz a identidade real usando somente fontes locais simuladas. */
  @BeforeEach
  void setup() {
    experiment.setExperimentType(
        com.marketinghub.experiment.ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL);
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setStage("AUTHORIZATION");
    cycle.setStatus("OPEN");
    cycle.setProductVersion("fixture-v12");
    cycle.setBudgetLimitBrl(new java.math.BigDecimal("100"));
    product.setSlug("fixture-product");
    slot.setProductSlug(product.getSlug());
    slot.setSourceExperimentId(92L);
    slot.setExperienceVersion(cycle.getProductVersion());
    slot.setStatus(com.marketinghub.pde.PdeProductionSlotStatus.READY);
    slot.setValidationStatus("OK");
    slot.setValidationCheckedAt(java.time.Instant.now());
    slot.setValidationHttpStatus(200);
    slot.setValidationContractSlug(product.getSlug());
    slot.setPublicUrl("https://fixture.test");
    slot.setValidationResolvedUrl("https://fixture.test/");
    slot.setDraftExperienceJson("{\"fixture\":true}");
    slot.setPublishedAt(java.time.Instant.now());
    slot.setPublishedExperienceJson("{\"fixture\":true}");
    when(slots.findByProductSlugOrderBySlotCodeAsc(product.getSlug())).thenReturn(List.of(slot));
    process.setProcessCode("pde-commercial-homologation-activation");
    activity.setActivityId("humanExperienceReview");
    when(experiments.findById(92L)).thenReturn(Optional.of(experiment));
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));
  }

  /**
   * Outros formatos conservam seus requisitos canônicos sem serem convertidos em webapp com slot.
   */
  @Test
  void nonMembershipProductUsesItsOwnCommercialDestination() {
    experiment.setExperimentType(com.marketinghub.experiment.ExperimentType.LOW_TICKET_PRODUCT);
    requirements(codes.stream().map(code -> item(code, true)).toList());
    assertThat(readiness.inspect(cycle).readyForReview()).isTrue();
    assertThat(readiness.inspect(cycle).requirements()).hasSize(5);
    verifyNoInteractions(slots);
  }

  /** Simula as cinco pré-condições sem declarar autorização, prontidão final ou custo zero. */
  private void requirements(List<ExperimentRunningGateRequirementDto> inputs) {
    when(gate.summarize(92L))
        .thenReturn(
            new ExperimentReadinessSummaryDto(
                false, 0, false, 0, false, false, 0, 7, List.of(), List.of(), false, inputs));
  }

  /** Monta um requisito com diagnóstico e orientação distinguíveis. */
  private ExperimentRunningGateRequirementDto item(String code, boolean ready) {
    return new ExperimentRunningGateRequirementDto(
        code, code, ready, "Diagnóstico " + code, "Correção " + code);
  }

  /** Cada entrada ausente bloqueia os revisores com a mesma causa apresentada na tela. */
  @ParameterizedTest
  @ValueSource(
      strings = {"CREATIVE_APPROVED", "CHECKOUT_READY", "INSTRUMENTATION_READY", "TARGETING_READY"})
  void missingInputBlocksReviewWithoutQueuingWork(String missing) {
    requirements(codes.stream().map(code -> item(code, !code.equals(missing))).toList());
    var result = readiness.inspect(cycle);
    assertThat(result.readyForReview()).isFalse();
    assertThat(result.guidance()).contains("#92", missing, "antes de iniciar");
    assertThat(result.experimentUrl()).isEqualTo("/experiments/92");
    assertThat(provider.readiness(process, activity, product, "experiment:92").ready()).isFalse();
    assertThat(provider.readiness(process, activity, product, "experiment:92").reason())
        .isEqualTo(result.guidance());
    verify(experiments, never()).save(any());
    verify(cycles, never()).save(any());
  }

  /**
   * Permite que Psique revise a candidata validada antes de existir snapshot público ou
   * autorização.
   */
  @Test
  void validatedCandidateStartsReviewWithoutPublishedSnapshot() {
    slot.setStatus(com.marketinghub.pde.PdeProductionSlotStatus.CANDIDATE);
    slot.setPublishedAt(null);
    slot.setPublishedExperienceJson(null);
    requirements(codes.stream().map(code -> item(code, !"LANDING_APPROVED".equals(code))).toList());

    var result = readiness.inspect(cycle);

    assertThat(result.readyForReview()).isTrue();
    assertThat(result.requirements())
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("CURRENT_VERSION_READY");
              assertThat(requirement.ready()).isTrue();
              assertThat(requirement.detail()).contains("preflight");
            })
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("LANDING_APPROVED");
              assertThat(requirement.ready()).isTrue();
              assertThat(requirement.detail()).contains("sem antecipar a publicação");
            });
    assertThat(slot.getPublishedAt()).isNull();
  }

  /** Insumos prontos não exigem orçamento já materializado nem substituem o preflight posterior. */
  @Test
  void preparedInputsDoNotClaimReadyToRunOrRequireBudgetTwice() {
    var inputs = new ArrayList<>(codes.stream().map(code -> item(code, true)).toList());
    inputs.add(item("MEDIA_BUDGET_READY", false));
    inputs.add(item("NO_BLOCKING_STAGES", false));
    requirements(inputs);
    var result = readiness.inspect(cycle);
    assertThat(result.readyForReview()).isTrue();
    assertThat(result.guidance()).contains("preflight", "autorização final");
    assertThat(result.requirements()).hasSize(6);
    assertThat(experiment.getMediaSpendLimit()).isNull();
  }

  /** Contrato incompleto ou ambíguo nunca é interpretado como prontidão. */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void missingOrDuplicatedRequirementFailsClosed(boolean duplicated) {
    var inputs =
        new ArrayList<>(
            codes.stream()
                .filter(code -> !code.equals("CHECKOUT_READY"))
                .map(code -> item(code, true))
                .toList());
    if (duplicated) {
      inputs.add(item("CHECKOUT_READY", true));
      inputs.add(item("CHECKOUT_READY", true));
    }
    requirements(inputs);
    var result = readiness.inspect(cycle);
    assertThat(result.readyForReview()).isFalse();
    assertThat(result.requirements())
        .anySatisfy(
            r -> {
              assertThat(r.code()).isEqualTo("CHECKOUT_READY");
              assertThat(r.ready()).isFalse();
              assertThat(r.detail()).contains("único");
            });
  }

  /** Fonte de outro produto bloqueia antes de ler a prontidão ou selecionar outro experimento. */
  @Test
  void mismatchedProductFailsBeforeReadingGate() {
    cycle.setProductId(104L);
    assertThatThrownBy(() -> readiness.inspect(cycle)).hasMessageContaining("outro produto");
    assertThat(provider.readiness(process, activity, product, "experiment:92").ready()).isFalse();
    verifyNoInteractions(gate);
  }

  /** Outro identificador válido recebe requisitos próprios sem exceção fixa para Vega. */
  @Test
  void appliesToAnotherCycleAndExperiment() {
    cycle.setId(302L);
    cycle.setExperimentId(192L);
    experiment.setId(192L);
    when(experiments.findById(192L)).thenReturn(Optional.of(experiment));
    requirements(codes.stream().map(code -> item(code, false)).toList());
    var otherGate = gate.summarize(92L);
    when(gate.summarize(192L)).thenReturn(otherGate);
    assertThat(readiness.inspect(cycle).guidance()).contains("#192").doesNotContain("#92");
    assertThat(readiness.inspect(cycle).experimentUrl()).isEqualTo("/experiments/192");
  }

  /** Canais sem mídia Meta conservam seu próprio contrato e não recebem requisitos circulares. */
  @Test
  void nonFacebookKeepsOwnPreparationPolicy() {
    experiment.setPlatform(null);
    assertThat(readiness.inspect(cycle)).isNull();
    assertThat(provider.readiness(process, activity, product, "experiment:92").ready()).isTrue();
    verifyNoInteractions(gate);
  }

  /** O gate novo não captura a homologação privada já concluída nem processos sem ciclo. */
  @Test
  void privateReviewAndUnmanagedExperimentKeepExistingGates() {
    assertThat(provider.supports(process, activity)).isTrue();
    process.setProcessCode("pde-construction-approval");
    assertThat(provider.supports(process, activity)).isFalse();
    assertThat(provider.readiness(process, activity, product, "experiment:999").ready()).isTrue();
    verifyNoInteractions(gate);
  }

  /** Superfície de outro produto, versão ou experimento nunca satisfaz o ciclo sucessor. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "version",
        "product",
        "experiment",
        "contract",
        "status",
        "unvalidated",
        "stale-url",
        "ambiguous",
        "missing"
      })
  void rejectsWrongCommercialSurface(String mismatch) {
    requirements(codes.stream().map(code -> item(code, true)).toList());
    switch (mismatch) {
      case "version" -> slot.setExperienceVersion("fixture-v7");
      case "product" -> slot.setProductSlug("another-product");
      case "experiment" -> slot.setSourceExperimentId(91L);
      case "contract" -> {
        slot.setDraftExperienceJson(null);
        slot.setPublishedExperienceJson(null);
      }
      case "status" -> slot.setStatus(com.marketinghub.pde.PdeProductionSlotStatus.PLANNED);
      case "unvalidated" -> slot.setValidationStatus("ERROR");
      case "stale-url" -> slot.setValidationResolvedUrl("https://another.test");
      case "ambiguous" -> {
        var duplicate = new com.marketinghub.pde.PdeProductionSlot();
        duplicate.setProductSlug(product.getSlug());
        duplicate.setSourceExperimentId(92L);
        duplicate.setExperienceVersion(cycle.getProductVersion());
        when(slots.findByProductSlugOrderBySlotCodeAsc(product.getSlug()))
            .thenReturn(List.of(slot, duplicate));
      }
      case "missing" ->
          when(slots.findByProductSlugOrderBySlotCodeAsc(product.getSlug())).thenReturn(List.of());
      default -> throw new AssertionError(mismatch);
    }
    assertThat(readiness.inspect(cycle).readyForReview()).isFalse();
    assertThat(readiness.inspect(cycle).requirements().getFirst().ready()).isFalse();
  }
}
