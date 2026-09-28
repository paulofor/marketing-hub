package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.agenttask.AgentTaskService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentPlatform;
import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.experiment.funnel.ExperimentFunnelStandbyService;
import com.marketinghub.experiment.run.service.ExperimentRunMetricLifecycleService;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookCampaignStopReason;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentStatusChangeRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valida o encerramento idempotente de execuções fora da janela comercial autorizada. */
class ExperimentWindowReconciliationServiceTest {
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final ExperimentStatusChangeRepository statusChanges =
      mock(ExperimentStatusChangeRepository.class);
  private final ExperimentFunnelStandbyService standby = mock(ExperimentFunnelStandbyService.class);
  private final ExperimentRunMetricLifecycleService runs =
      mock(ExperimentRunMetricLifecycleService.class);
  private final AgentTaskService tasks = mock(AgentTaskService.class);
  private ExperimentWindowReconciliationService service;

  /** Fixa a data analisada para cobrir os registros históricos da inconsistência. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentWindowReconciliationService(
            experiments,
            statusChanges,
            standby,
            runs,
            tasks,
            Clock.fixed(Instant.parse("2026-09-28T12:00:00Z"), ZoneOffset.UTC));
  }

  /** Encerra tanto a janela direta vencida quanto a execução sem prazo e preserva a vigente. */
  @Test
  void reconcilesExpiredAndMissingDirectWindows() {
    Experiment expired =
        directExperiment(89L, LocalDate.parse("2026-08-22"), LocalDate.parse("2026-09-11"));
    Experiment missing = directExperiment(90L, null, null);
    Experiment current =
        directExperiment(92L, LocalDate.parse("2026-09-27"), LocalDate.parse("2026-09-29"));
    when(experiments.findForWindowReconciliation(
            ExperimentStatus.RUNNING, ExperimentPlatform.DIRECT_ONE_TO_ONE))
        .thenReturn(List.of(expired, missing, current));

    var result = service.reconcileDirectRunningWindows();

    assertThat(result.scanned()).isEqualTo(3);
    assertThat(result.closedExperimentIds()).containsExactly(89L, 90L);
    assertThat(expired.getStatus()).isEqualTo(ExperimentStatus.INCONCLUSIVE);
    assertThat(missing.getStatus()).isEqualTo(ExperimentStatus.INCONCLUSIVE);
    assertThat(current.getStatus()).isEqualTo(ExperimentStatus.RUNNING);
    var repeated = service.reconcileDirectRunningWindows();
    assertThat(repeated.closed()).isZero();
    verify(statusChanges, times(2)).save(any());
    verify(runs, times(2))
        .completeCommercialStop(
            any(),
            org.mockito.ArgumentMatchers.eq(
                FacebookCampaignStopReason.CAMPAIGN_AUTHORIZED_WINDOW_ENDED),
            any());
    verify(tasks, times(2)).cancelActiveTasksBySourceReference(any(), any());
  }

  /** Encerra a campanha Meta quando o término oficial já passou. */
  @Test
  void reconcilesFacebookWindowFromOfficialStopTime() {
    Experiment experiment = Experiment.builder().id(91L).status(ExperimentStatus.RUNNING).build();
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId("cmp-91");
    campaign.setExperiment(experiment);
    campaign.setMetaStopTime(Instant.parse("2026-09-27T02:59:59Z"));

    boolean changed =
        service.reconcileFacebookWindow(campaign, Instant.parse("2026-09-28T12:00:00Z"), false);

    assertThat(changed).isTrue();
    assertThat(experiment.getStatus()).isEqualTo(ExperimentStatus.INCONCLUSIVE);
    verify(standby)
        .requestFacebookCampaignStops(
            91L,
            FacebookCampaignStopReason.CAMPAIGN_AUTHORIZED_WINDOW_ENDED,
            experiment.getLastStatusChangeReason());
  }

  /** Cria um experimento direto no estado usado pela varredura. */
  private Experiment directExperiment(Long id, LocalDate startDate, LocalDate endDate) {
    return Experiment.builder()
        .id(id)
        .platform(ExperimentPlatform.DIRECT_ONE_TO_ONE)
        .status(ExperimentStatus.RUNNING)
        .startDate(startDate)
        .endDate(endDate)
        .build();
  }
}
