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
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.dto.ExperimentReadinessSummaryDto;
import com.marketinghub.experiment.dto.ExperimentRunningGateRequirementDto;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: comprovar requisitos financeiros e efeito da autorização comercial PDE. */
class PdeCommercialActivationHumanActivityHandlerTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);
  private final ExperimentReadinessService readinessService =
      mock(ExperimentReadinessService.class);
  private final ExperimentService experimentService = mock(ExperimentService.class);
  private final PdeCommercialActivationHumanActivityHandler handler =
      new PdeCommercialActivationHumanActivityHandler(
          experiments, plans, readinessService, experimentService);

  /** Libera a decisão somente com gates verdes e teto financeiro positivo. */
  @Test
  void exposesAuditedActivationWhenRequirementsAreReady() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(List.of(CommercialPlan.builder().maxBudget(new BigDecimal("400.00")).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isTrue();
    assertThat(result.confirmationMessage().replace('\u00a0', ' ')).contains("R$ 400,00");
    assertThat(result.requirements())
        .extracting(requirement -> requirement.code())
        .contains("PREFLIGHT_APPROVED", "BUDGET_LIMIT_DEFINED");
  }

  /** Mantém a ativação bloqueada quando o plano não limita o gasto. */
  @Test
  void blocksActivationWithoutPositiveBudgetLimit() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
    when(experiments.findById(89L)).thenReturn(java.util.Optional.of(experiment));
    when(readinessService.summarize(89L)).thenReturn(readiness(true));
    when(plans.findByExperimentReference(89L))
        .thenReturn(List.of(CommercialPlan.builder().maxBudget(BigDecimal.ZERO).build()));

    HumanProductProcessActivityReadiness result =
        handler.readiness(process(), activity(), product, "experiment:89");

    assertThat(result.ready()).isFalse();
    assertThat(result.reason()).contains("Defina o teto");
    verify(experimentService, never()).updateStatus(89L, ExperimentStatus.RUNNING);
  }

  /** Aplica RUNNING pelo serviço canônico depois da confirmação humana. */
  @Test
  void activatesExperimentThroughCanonicalService() {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = experiment(product, ExperimentStatus.PLANNED);
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

    verify(experimentService).updateStatus(89L, ExperimentStatus.RUNNING);
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

    verify(experimentService).updateStatus(89L, ExperimentStatus.RUNNING);
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
