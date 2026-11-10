package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.costWorksheet.ApproveCostCeilingRequest;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentRunningGateRequirementDto;
import com.marketinghub.experiment.service.ExperimentReadinessService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

/** Responsabilidade: ligar o teto salvo à revisão comercial sem autorizar anúncios. */
class LearningCyclePlanningMediaReadinessTest {
  private LearningCycleFinancialReviewTest fixture;
  private Experiment experiment;
  private LearningCycleCommercialReadiness readiness;
  private final ExperimentReadinessService gate = mock(ExperimentReadinessService.class);
  private final List<String> codes =
      List.of(
          "LANDING_APPROVED",
          "CREATIVE_APPROVED",
          "CHECKOUT_READY",
          "INSTRUMENTATION_READY",
          "TARGETING_READY");

  /**
   * Reutiliza o controller, o ledger e a seleção de recibos reais com fontes externas simuladas.
   */
  @BeforeEach
  void setup() {
    fixture = new LearningCycleFinancialReviewTest();
    fixture.setup();
    readiness =
        new LearningCycleCommercialReadiness(
            fixture.experiments, gate, mock(PdeProductionSlotRepository.class));
    ReflectionTestUtils.setField(
        readiness,
        "costCeilings",
        new LearningCycleCostCeilings(fixture.events, fixture.json, fixture.cumulative));
    context(7, 19, 112, 27);
  }

  /**
   * Define identidades distintas e mantém o experimento planejado, sem limite operacional ou
   * janela.
   */
  private void context(long productId, long cycleId, long experimentId, long chainId) {
    var cycle = fixture.cycle;
    cycle.setProductId(productId);
    cycle.setId(cycleId);
    cycle.setExperimentId(experimentId);
    cycle.setChainDefinitionId(chainId);
    cycle.setBudgetLimitBrl(BigDecimal.ZERO);
    cycle.setWindowStart(null);
    cycle.setWindowEnd(null);
    var product = Product.builder().id(productId).name("Contexto financeiro sintético").build();
    experiment =
        Experiment.builder()
            .id(experimentId)
            .product(product)
            .platform(ExperimentPlatform.FACEBOOK)
            .experimentType(ExperimentType.LOW_TICKET_PRODUCT)
            .status(ExperimentStatus.PLANNED)
            .build();
    when(fixture.products.findById(productId)).thenReturn(Optional.of(product));
    when(fixture.products.findLockedById(productId)).thenReturn(Optional.of(product));
    when(fixture.cycles.findById(cycleId)).thenReturn(Optional.of(cycle));
    when(fixture.cycles.findLocked(productId, cycleId)).thenReturn(Optional.of(cycle));
    when(fixture.experiments.findById(experimentId)).thenReturn(Optional.of(experiment));
    requirements(null);
  }

  /** Mantém os demais gates explícitos para separar orçamento de aprovação funcional. */
  private void requirements(String missing) {
    var inputs =
        codes.stream()
            .map(
                code ->
                    new ExperimentRunningGateRequirementDto(
                        code,
                        code,
                        !code.equals(missing),
                        "Prova sintética",
                        "Concluir a entrada ausente"))
            .toList();
    when(gate.summarize(experiment.getId()))
        .thenReturn(
            new ExperimentReadinessSummaryDto(
                false, 0, false, 0, false, false, 0, 7, List.of(), List.of(), false, inputs));
  }

  /** Salva a mesma decisão de teto usada pela tela financeira, sem preencher uma previsão. */
  private void approve(ApproveCostCeilingRequest.Group group, BigDecimal amount) throws Exception {
    var cycle = fixture.cycle;
    var request =
        new ApproveCostCeilingRequest(
            UUID.randomUUID(),
            cycle.getRevision(),
            cycle.getChainDefinitionId(),
            cycle.getExperimentId(),
            cycle.getProductVersion(),
            group,
            amount,
            "Operador sintético",
            "Planejamento do experimento local",
            true);
    fixture
        .http
        .perform(
            post("/api/business-process-chains/learning-cycles/v1/products/"
                    + cycle.getProductId()
                    + "/"
                    + cycle.getId()
                    + "/cost-ceilings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(fixture.json.write(request)))
        .andExpect(status().isOk());
  }

  /**
   * Reproduz Capella e outro produto: o recibo libera revisão, mas não muda execução, verba ou
   * janela.
   */
  @ParameterizedTest
  @CsvSource({"7,19,112,27", "99107,99119,99112,99127"})
  void approvedPlanningLimitSupportsReviewWithoutActivatingMedia(
      long productId, long cycleId, long experimentId, long chainId) throws Exception {
    context(productId, cycleId, experimentId, chainId);
    assertThat(readiness.inspect(fixture.cycle).readyForReview()).isFalse();
    approve(ApproveCostCeilingRequest.Group.MEDIA, new BigDecimal("100"));
    clearInvocations(fixture.cycles, fixture.experiments);

    assertThat(readiness.inspect(fixture.cycle).readyForReview()).isTrue();
    assertThat(fixture.cycle.getBudgetLimitBrl()).isZero();
    assertThat(fixture.cycle.getWindowStart()).isNull();
    assertThat(fixture.cycle.getWindowEnd()).isNull();
    assertThat(fixture.cycle.getStage()).isEqualTo("PLANNING");
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
    assertThat(experiment.getMediaSpendLimit()).isNull();
    assertThat(fixture.history).hasSize(1);
    verify(fixture.cycles, never()).save(any());
    verify(fixture.experiments, never()).save(any());
  }

  /** Recusa teto ausente, zerado, substituído ou pertencente a outro contexto. */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "missing",
        "zero",
        "zero-with-legacy",
        "reduced-to-zero",
        "version",
        "chain",
        "experiment",
        "cycle",
        "stale",
        "other-group"
      })
  void rejectsMissingOrIneligiblePlanningLimit(String failure) throws Exception {
    if ("zero-with-legacy".equals(failure)) fixture.cycle.setBudgetLimitBrl(new BigDecimal("500"));
    if (!"missing".equals(failure))
      approve(
          "other-group".equals(failure)
              ? ApproveCostCeilingRequest.Group.PRODUCTION
              : ApproveCostCeilingRequest.Group.MEDIA,
          List.of("zero", "zero-with-legacy").contains(failure)
              ? BigDecimal.ZERO
              : new BigDecimal("100"));
    switch (failure) {
      case "reduced-to-zero" -> approve(ApproveCostCeilingRequest.Group.MEDIA, BigDecimal.ZERO);
      case "version" -> fixture.cycle.setProductVersion("nova-versao");
      case "chain" -> fixture.cycle.setChainDefinitionId(999L);
      case "cycle" -> fixture.cycle.setId(999L);
      case "experiment" -> {
        fixture.cycle.setExperimentId(999L);
        experiment.setId(999L);
        when(fixture.experiments.findById(999L)).thenReturn(Optional.of(experiment));
        requirements(null);
      }
      case "stale" ->
          fixture.cycle.setVersionChangedAt(
              fixture.history.getFirst().getCreatedAt().plusSeconds(1));
      default -> {}
    }
    assertThat(readiness.inspect(fixture.cycle).readyForReview()).isFalse();
    assertThat(readiness.inspect(fixture.cycle).requirements())
        .anySatisfy(r -> assertThat(r.code()).isEqualTo("CYCLE_MEDIA_LIMIT"));
  }

  /** O teto não aprova um criativo ausente nem altera os demais gates. */
  @Test
  void planningLimitDoesNotReplaceCreativeApproval() throws Exception {
    approve(ApproveCostCeilingRequest.Group.MEDIA, new BigDecimal("100"));
    requirements("CREATIVE_APPROVED");
    var result = readiness.inspect(fixture.cycle);
    assertThat(result.readyForReview()).isFalse();
    assertThat(result.requirements())
        .noneSatisfy(r -> assertThat(r.code()).isEqualTo("CYCLE_MEDIA_LIMIT"));
    assertThat(result.requirements())
        .anySatisfy(
            r -> {
              assertThat(r.code()).isEqualTo("CREATIVE_APPROVED");
              assertThat(r.ready()).isFalse();
            });
  }
}
