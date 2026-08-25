package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.product.Product;
import com.marketinghub.product.service.valuechainposition.ProductProcessPeriodService;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Valida a ativação atômica do experimento PDE executado por abordagem individual. */
@ExtendWith(MockitoExtension.class)
class ExperimentDirectPdeActivationServiceTest {
  private static final Instant ACTIVATED_AT = Instant.parse("2026-08-25T22:00:00Z");

  @Mock private ExperimentRunRepository experimentRunRepository;
  @Mock private ProductProcessPeriodService productProcessPeriodService;

  private ExperimentDirectPdeActivationService service;

  /** Configura um relógio fixo para conferir todos os marcos da janela comercial. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentDirectPdeActivationService(
            experimentRunRepository,
            productProcessPeriodService,
            Clock.fixed(ACTIVATED_AT, ZoneOffset.UTC));
  }

  /** Abre run e produto no mesmo comando quando o preflight produtivo foi aprovado. */
  @Test
  void shouldActivateReadyDirectPdeRunAndProduct() {
    Product product = Product.builder().id(4L).commercialStatus("VALIDACAO_COMERCIAL").build();
    Experiment experiment = directPdeExperiment(product);
    ExperimentRun run =
        ExperimentRun.builder()
            .id(7L)
            .experiment(experiment)
            .mode(ExperimentRunMode.PRODUCTION)
            .status(ExperimentRunStatus.READY_TO_PUBLISH)
            .build();
    when(experimentRunRepository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            90L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));

    service.activate(experiment);

    assertThat(run.getStatus()).isEqualTo(ExperimentRunStatus.RUNNING);
    assertThat(run.getPublicationRequestedAt()).isEqualTo(ACTIVATED_AT);
    assertThat(run.getPublishedAt()).isEqualTo(ACTIVATED_AT);
    assertThat(run.getCommercialWindowStartedAt()).isEqualTo(ACTIVATED_AT);
    assertThat(product.getCommercialStatus()).isEqualTo("ATIVO");
    verify(experimentRunRepository).save(run);
    verify(productProcessPeriodService).recordTransition(product, "VALIDACAO_COMERCIAL");
  }

  /** Impede RUNNING sem homologação produtiva e preserva o estado do produto. */
  @Test
  void shouldRejectActivationWithoutReadyProductionRun() {
    Product product = Product.builder().id(4L).commercialStatus("VALIDACAO_COMERCIAL").build();
    Experiment experiment = directPdeExperiment(product);
    when(experimentRunRepository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            90L, ExperimentRunMode.PRODUCTION))
        .thenReturn(
            Optional.of(
                ExperimentRun.builder()
                    .status(ExperimentRunStatus.PREFLIGHT_FAILED)
                    .mode(ExperimentRunMode.PRODUCTION)
                    .build()));

    assertThatThrownBy(() -> service.activate(experiment))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("READY_TO_PUBLISH");

    assertThat(product.getCommercialStatus()).isEqualTo("VALIDACAO_COMERCIAL");
    verify(experimentRunRepository, never()).save(org.mockito.ArgumentMatchers.any());
    verify(productProcessPeriodService, never())
        .recordTransition(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  /** Cria o contrato mínimo de experimento governado pela ativação PDE direta. */
  private Experiment directPdeExperiment(Product product) {
    return Experiment.builder()
        .id(90L)
        .product(product)
        .experimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL)
        .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
        .build();
  }
}
