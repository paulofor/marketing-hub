package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.experiment.ExperimentRunRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa a abertura auditável da janela comercial pela primeira impressão sincronizada. */
@ExtendWith(MockitoExtension.class)
class ExperimentRunMetricLifecycleServiceTest {

  @Mock private ExperimentRunRepository experimentRunRepository;

  private ExperimentRunMetricLifecycleService service;

  /** Inicializa o serviço com repositório controlado pelo teste. */
  @BeforeEach
  void setUp() {
    service = new ExperimentRunMetricLifecycleService(experimentRunRepository);
  }

  /** Marca a publicação sem abrir janela comercial quando a Meta ainda retorna zero impressão. */
  @Test
  void synchronizeKeepsPublishedRunAwaitingFirstExposure() {
    Experiment experiment =
        Experiment.builder()
            .id(88L)
            .facebookReleaseRequestedAt(Instant.parse("2026-08-20T23:22:17Z"))
            .build();
    FacebookAdsCampaign campaign = campaign(experiment, "2026-08-20T23:26:24Z");
    ExperimentRun run = run(experiment, ExperimentRunStatus.READY_TO_PUBLISH);
    when(experimentRunRepository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            88L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));

    service.synchronize(experiment, campaign, 0L);

    assertThat(run.getPublicationRequestedAt()).isEqualTo(Instant.parse("2026-08-20T23:22:17Z"));
    assertThat(run.getPublishedAt()).isEqualTo(Instant.parse("2026-08-20T23:26:24Z"));
    assertThat(run.getStatus()).isEqualTo(ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE);
    assertThat(run.getFirstVerifiedImpressionAt()).isNull();
    assertThat(run.getCommercialWindowStartedAt()).isNull();
    verify(experimentRunRepository).save(run);
  }

  /** Abre uma única janela comercial quando a primeira impressão real aparece. */
  @Test
  void synchronizeStartsCommercialWindowOnFirstVerifiedImpression() {
    Experiment experiment = Experiment.builder().id(88L).build();
    FacebookAdsCampaign campaign = campaign(experiment, "2026-08-20T23:26:24Z");
    ExperimentRun run = run(experiment, ExperimentRunStatus.PUBLISHED_AWAITING_EXPOSURE);
    run.setPublishedAt(campaign.getCreatedAt());
    when(experimentRunRepository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            88L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));

    service.synchronize(experiment, campaign, 1L);

    assertThat(run.getStatus()).isEqualTo(ExperimentRunStatus.RUNNING);
    assertThat(run.getFirstVerifiedImpressionAt()).isNotNull();
    assertThat(run.getCommercialWindowStartedAt()).isEqualTo(run.getFirstVerifiedImpressionAt());
    verify(experimentRunRepository).save(run);
  }

  /** Impede que uma sincronização tardia reabra um run terminal. */
  @Test
  void synchronizeDoesNotReopenTerminalRun() {
    Experiment experiment = Experiment.builder().id(88L).build();
    FacebookAdsCampaign campaign = campaign(experiment, "2026-08-20T23:26:24Z");
    ExperimentRun run = run(experiment, ExperimentRunStatus.COMPLETED);
    when(experimentRunRepository.findTopByExperimentIdAndModeOrderByRunNumberDesc(
            88L, ExperimentRunMode.PRODUCTION))
        .thenReturn(Optional.of(run));

    service.synchronize(experiment, campaign, 100L);

    assertThat(run.getStatus()).isEqualTo(ExperimentRunStatus.COMPLETED);
    verify(experimentRunRepository, never()).save(run);
  }

  /** Cria uma campanha mínima com o marco de publicação informado. */
  private FacebookAdsCampaign campaign(Experiment experiment, String createdAt) {
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId("120251282333490326");
    campaign.setExperiment(experiment);
    campaign.setCreatedAt(Instant.parse(createdAt));
    return campaign;
  }

  /** Cria um run produtivo mínimo no status informado. */
  private ExperimentRun run(Experiment experiment, ExperimentRunStatus status) {
    return ExperimentRun.builder()
        .id(6L)
        .experiment(experiment)
        .runNumber(1)
        .mode(ExperimentRunMode.PRODUCTION)
        .status(status)
        .build();
  }
}
