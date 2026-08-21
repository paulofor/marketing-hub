package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.run.service.ExperimentRunMetricLifecycleService;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testa a sincronização de métricas de campanha com o marco comercial real do funil. */
@ExtendWith(MockitoExtension.class)
class ExperimentCampaignMetricServiceTest {

  @Mock private ExperimentCampaignMetricRepository repository;

  @Mock private FacebookAdsCampaignRepository campaignRepository;

  @Mock private CostAttributionService costAttributionService;

  @Mock private ExperimentRunMetricLifecycleService runMetricLifecycleService;

  private ExperimentCampaignMetricService service;

  /** Monta o serviço real com repositórios controlados por mock. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentCampaignMetricService(
            repository, campaignRepository, costAttributionService, runMetricLifecycleService, "");
  }

  /**
   * Garante que o primeiro recebimento de impressões preserva os eventos auditáveis e salva a
   * métrica real.
   */
  @Test
  void upsertPreservesFunnelWhenImpressionsStart() {
    Experiment experiment = Experiment.builder().id(41L).build();
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId("campaign-1");
    campaign.setExperiment(experiment);
    ExperimentCampaignMetric savedMetric =
        ExperimentCampaignMetric.builder().experiment(experiment).build();

    when(campaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
    when(repository.findByExperiment(experiment)).thenReturn(Optional.empty());
    when(repository.save(any(ExperimentCampaignMetric.class))).thenReturn(savedMetric);

    service.upsert(
        "campaign-1",
        LocalDate.parse("2026-06-24"),
        LocalDate.parse("2026-06-24"),
        10L,
        194L,
        3L,
        0L,
        new BigDecimal("1.10"));

    verify(repository).save(any(ExperimentCampaignMetric.class));
    assertThat(experiment.getFunnelResetAt()).isNull();
  }

  /**
   * Garante que uma métrica que já tinha impressões não limpa novamente o funil em sincronizações
   * posteriores.
   */
  @Test
  void upsertDoesNotResetFunnelWhenMetricAlreadyHadImpressions() {
    Experiment experiment = Experiment.builder().id(41L).build();
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId("campaign-1");
    campaign.setExperiment(experiment);
    ExperimentCampaignMetric existingMetric =
        ExperimentCampaignMetric.builder()
            .experiment(experiment)
            .impressions(100L)
            .spend(BigDecimal.ZERO)
            .build();

    when(campaignRepository.findById("campaign-1")).thenReturn(Optional.of(campaign));
    when(repository.findByExperiment(experiment)).thenReturn(Optional.of(existingMetric));
    when(repository.save(existingMetric)).thenReturn(existingMetric);

    service.upsert(
        "campaign-1",
        LocalDate.parse("2026-06-24"),
        LocalDate.parse("2026-06-24"),
        10L,
        194L,
        3L,
        0L,
        BigDecimal.ZERO);

    verify(repository).save(existingMetric);
  }

  /** Garante que toda sincronização reconcilia o run com a exposição recebida da Meta. */
  @Test
  void upsertSynchronizesRunMetricLifecycle() {
    Experiment experiment = Experiment.builder().id(88L).build();
    FacebookAdsCampaign campaign = new FacebookAdsCampaign();
    campaign.setId("campaign-88");
    campaign.setExperiment(experiment);
    ExperimentCampaignMetric metric =
        ExperimentCampaignMetric.builder()
            .experiment(experiment)
            .campaign(campaign)
            .impressions(0L)
            .build();

    when(campaignRepository.findById("campaign-88")).thenReturn(Optional.of(campaign));
    when(repository.findByExperiment(experiment)).thenReturn(Optional.of(metric));
    when(repository.save(metric)).thenReturn(metric);

    service.upsert("campaign-88", null, null, 0L, 0L, 0L, 0L, BigDecimal.ZERO);

    verify(runMetricLifecycleService).synchronize(experiment, campaign, 0L);
  }

  /**
   * Garante que campanha do Clube MUSA tambem limpa analytics PDE antes de salvar a primeira
   * metrica real.
   */
  @Test
  void upsertResetsPdeAnalyticsForClubMusaWhenImpressionsStart() throws IOException {
    AtomicInteger resetCalls = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/reset",
        exchange -> {
          resetCalls.incrementAndGet();
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    server.start();
    try {
      service =
          new ExperimentCampaignMetricService(
              repository,
              campaignRepository,
              costAttributionService,
              runMetricLifecycleService,
              "http://localhost:" + server.getAddress().getPort() + "/reset");
      Experiment experiment =
          Experiment.builder().id(67L).followUpActionUrl("https://clubemusa.com.br").build();
      FacebookAdsCampaign campaign = new FacebookAdsCampaign();
      campaign.setId("campaign-musa");
      campaign.setExperiment(experiment);
      ExperimentCampaignMetric savedMetric =
          ExperimentCampaignMetric.builder().experiment(experiment).build();

      when(campaignRepository.findById("campaign-musa")).thenReturn(Optional.of(campaign));
      when(repository.findByExperiment(experiment)).thenReturn(Optional.empty());
      when(repository.save(any(ExperimentCampaignMetric.class))).thenReturn(savedMetric);

      service.upsert(
          "campaign-musa",
          LocalDate.parse("2026-07-20"),
          LocalDate.parse("2026-07-20"),
          10L,
          1L,
          0L,
          0L,
          BigDecimal.ZERO);

      assertThat(resetCalls).hasValue(1);
    } finally {
      server.stop(0);
    }
  }
}
