package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.humanactivity.HumanProductProcessActivityReadiness;
import com.marketinghub.businessprocess.execution.service.requestProductProcessActivityExecution.ProductProcessActivityExecutionRequest;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentRunningGateRequirementDto;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar requisitos financeiros e efeito da autorização comercial PDE. */
class PdeCommercialActivationHumanActivityHandlerTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentRunRepository runs = mock(ExperimentRunRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final ExperimentReadinessService readinessService =
      mock(ExperimentReadinessService.class);
  private final ExperimentService experimentService = mock(ExperimentService.class);
  private final PdeCommercialActivationHumanActivityHandler handler =
      new PdeCommercialActivationHumanActivityHandler(
          experiments, runs, plans, cycles, readinessService, experimentService);

  /** Libera a decisão somente com gates verdes e teto financeiro positivo. */
  @Test
  void exposesAuditedActivationWhenRequirementsAreReady() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(
            List.of(CommercialPlan.builder().id(4L).maxBudget(new BigDecimal("400.00")).build()));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(java.util.Optional.of(ExperimentRun.builder().id(9L).runNumber(2).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isTrue();
    assertThat(result.confirmationMessage().replace('\u00a0', ' ')).contains("R$ 400,00");
    assertThat(result.decisionMode()).isEqualTo("REVIEW_AND_ACCEPT");
    assertThat(result.auditEvidenceReference())
        .isEqualTo(
            "experiment:89; experiment-run:9/run-number:2; commercial-plan:4; daily-budget:20.00; media-spend-limit:400.00; window:2026-09-23/2026-09-28");
    assertThat(result.requirements())
        .extracting(requirement -> requirement.code())
        .contains("PREFLIGHT_APPROVED", "BUDGET_LIMIT_DEFINED");
  }

  /** Mostra ao operador o teto efetivo do experimento sem confundi-lo com o máximo do plano. */
  @Test
  void presentsExactExperimentBudgetAndWindowInsidePlanLimit() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    experiment.setMediaSpendLimit(new BigDecimal("125.00"));
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(
            List.of(CommercialPlan.builder().id(4L).maxBudget(new BigDecimal("400.00")).build()));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(java.util.Optional.of(ExperimentRun.builder().id(9L).runNumber(2).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isTrue();
    assertThat(result.confirmationMessage().replace('\u00a0', ' '))
        .contains("teto total de R$ 125,00", "R$ 20,00", "2026-09-23", "2026-09-28")
        .doesNotContain("teto total de R$ 400,00");
    assertThat(result.requirements())
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("BUDGET_LIMIT_DEFINED");
              assertThat(requirement.satisfied()).isTrue();
              assertThat(requirement.detail().replace('\u00a0', ' '))
                  .contains("R$ 125,00", "R$ 400,00");
            });
  }

  /** Distingue o consentimento para mídia Meta da operação direta sem disparar a autorização. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(
      com.marketinghub.experiment.ExperimentPlatform.class)
  void explainsAuthorizationEffectForSelectedChannel(
      com.marketinghub.experiment.ExperimentPlatform platform) {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    experiment.setPlatform(platform);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(readinessService.summarize(89L)).thenReturn(readiness(false));
    var result = handler.readiness(process(), activity(), product, "experiment:89");
    if (platform == com.marketinghub.experiment.ExperimentPlatform.FACEBOOK) {
      assertThat(result.description())
          .contains("publicação da campanha na Meta", "gasto de mídia", "janela aprovada")
          .doesNotContain("sem criar campanha paga");
      assertThat(result.confirmationMessage()).contains("autoriza a publicação na Meta");
    } else {
      assertThat(result.description()).contains("sem criar campanha paga");
      assertThat(result.confirmationMessage()).doesNotContain("autoriza a publicação na Meta");
    }
    org.mockito.Mockito.verifyNoInteractions(experimentService);
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.PLANNED);
  }

  /** Mantém a ativação bloqueada quando o plano não limita o gasto. */
  @Test
  void blocksActivationWithoutPositiveBudgetLimit() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(List.of(CommercialPlan.builder().id(4L).maxBudget(BigDecimal.ZERO).build()));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(java.util.Optional.of(ExperimentRun.builder().id(9L).runNumber(2).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).contains("Defina o teto");
    verify(experimentService, never()).updateStatus(89L, ExperimentStatus.RUNNING);
  }

  /** Usa o teto do ciclo aberto e bloqueia divergência com o experimento operacional. */
  @Test
  void usesOpenCycleBudgetInsteadOfLargerHistoricalPlan() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    experiment.setMediaSpendLimit(new BigDecimal("200.00"));
    LearningSalesCycle cycle = new LearningSalesCycle();
    cycle.setStatus("OPEN");
    cycle.setBudgetLimitBrl(new BigDecimal("100.00"));
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(cycles.findByExperimentId(89L)).thenReturn(java.util.Optional.of(cycle));
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(
            List.of(CommercialPlan.builder().id(4L).maxBudget(new BigDecimal("400.00")).build()));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(java.util.Optional.of(ExperimentRun.builder().id(9L).runNumber(2).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isFalse();
    assertThat(result.confirmationMessage().replace('\u00a0', ' ')).contains("R$ 100,00");
    assertThat(result.requirements())
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("BUDGET_LIMIT_DEFINED");
              assertThat(requirement.satisfied()).isFalse();
              assertThat(requirement.detail()).contains("limite vigente");
            });
  }

  /** Bloqueia o aceite simples quando o run ainda não possui referência auditável. */
  @Test
  void blocksReviewAndAcceptWithoutAuditableProductionRun() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(
            List.of(CommercialPlan.builder().id(4L).maxBudget(new BigDecimal("540.00")).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isFalse();
    assertThat(result.auditEvidenceReference()).isNull();
    assertThat(result.requirements())
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("AUDIT_CONTEXT_READY");
              assertThat(requirement.satisfied()).isFalse();
            });
    verify(experimentService, never()).updateStatus(89L, ExperimentStatus.RUNNING);
  }

  /** Bloqueia a autorização quando a preparação específica do tipo perdeu vigência. */
  @Test
  void blocksAuthorizationWhenOpalaPreparationIsNotCurrent() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    experiment.setMediaSpendLimit(new BigDecimal("100.00"));
    LearningSalesCycle cycle = new LearningSalesCycle();
    cycle.setProductId(9L);
    cycle.setStatus("OPEN");
    cycle.setBudgetLimitBrl(new BigDecimal("100.00"));
    var routing = mock(com.marketinghub.opala.commercial.v1.service.OpalaCommercialRouting.class);
    org.springframework.test.util.ReflectionTestUtils.setField(handler, "opalaRouting", routing);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(cycles.findByExperimentId(89L)).thenReturn(java.util.Optional.of(cycle));
    when(routing.target(cycle)).thenReturn(new BusinessProcessDefinition());
    when(routing.completed(cycle)).thenReturn(false);
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(
            List.of(CommercialPlan.builder().id(4L).maxBudget(new BigDecimal("100.00")).build()));
    when(runs.findTopByExperimentIdAndModeOrderByRunNumberDesc(89L, ExperimentRunMode.PRODUCTION))
        .thenReturn(java.util.Optional.of(ExperimentRun.builder().id(9L).runNumber(2).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).contains("Abra o subprocesso");
    assertThat(result.requirements())
        .anySatisfy(
            requirement -> {
              assertThat(requirement.code()).isEqualTo("OPALA_PREPARATION_READY");
              assertThat(requirement.satisfied()).isFalse();
            });
  }

  /** Libera o Facebook pelo contrato canônico sem antecipar RUNNING à campanha. */
  @Test
  void releasesFacebookExperimentThroughCanonicalService() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    experiment.setPlatform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));

    handler.approve(
        process(),
        activity(),
        product,
        "experiment:89",
        new ProductProcessActivityExecutionRequest(
            "APPROVE",
            "Paulo Operador",
            "Preflight e teto financeiro foram revisados.",
            "experiment-run:12",
            "CONFIRM:pde-commercial-homologation-activation:authorization"));

    verify(experimentService).releaseForFacebook(89L);
    verify(experimentService, never()).updateStatus(89L, ExperimentStatus.RUNNING);
  }

  /** Reconcilia run e produto mesmo quando um estado legado já deixou o experimento em RUNNING. */
  @Test
  void reconcilesCommercialStatesWhenExperimentWasAlreadyRunning() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.RUNNING);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));

    handler.approve(
        process(),
        activity(),
        product,
        "experiment:89",
        new ProductProcessActivityExecutionRequest(
            "APPROVE",
            "Paulo Operador",
            "Estados comerciais revisados para reconciliação.",
            "experiment-run:9",
            "CONFIRM:pde-commercial-homologation-activation:authorization"));

    verify(experimentService, never()).updateStatus(89L, ExperimentStatus.RUNNING);
    verify(experimentService, never()).releaseForFacebook(89L);
  }

  /** Impede que uma decisão do produto atual ative experimento pertencente a outro produto. */
  @Test
  void rejectsExperimentFromAnotherProduct() {
    Product selectedProduct = Product.builder().id(9L).build();
    Product anotherProduct = Product.builder().id(10L).build();
    when(experiments.findById(89L))
        .thenReturn(java.util.Optional.of(experiment(anotherProduct, ExperimentStatus.PLANNED)));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> handler.readiness(process(), activity(), selectedProduct, "experiment:89"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("não pertence ao produto");
    verify(experimentService, never()).updateStatus(89L, ExperimentStatus.RUNNING);
  }

  /** Monta o resumo de prontidão usado pelo gate RUNNING. */
  private ExperimentReadinessSummaryDto readiness(boolean ready) {
    return new ExperimentReadinessSummaryDto(
        true,
        1,
        true,
        1,
        true,
        true,
        4,
        4,
        List.of(),
        List.of(),
        ready,
        List.of(
            new ExperimentRunningGateRequirementDto(
                "PREFLIGHT_APPROVED",
                "Preflight aprovado",
                ready,
                ready ? "Run produtivo aprovado." : "Run ainda pendente.",
                ready ? "Preserve as evidências." : "Conclua o preflight.")));
  }

  /** Monta o experimento operacional mais recente do produto. */
  private Experiment experiment(Product product, ExperimentStatus status) {
    return Experiment.builder()
        .id(89L)
        .product(product)
        .name("Rigel direto")
        .sampleSize(15)
        .platform(com.marketinghub.experiment.ExperimentPlatform.FACEBOOK)
        .dailyBudget(new BigDecimal("20.00"))
        .mediaSpendLimit(new BigDecimal("400.00"))
        .startDate(LocalDate.of(2026, 9, 23))
        .endDate(LocalDate.of(2026, 9, 28))
        .status(status)
        .build();
  }

  /** Monta o processo comercial reconhecido pelo handler. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setProcessCode("pde-commercial-homologation-activation");
    return process;
  }

  /** Monta a atividade humana reconhecida pelo handler. */
  private BusinessProcessActivityDefinition activity() {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("authorization");
    return activity;
  }
}
