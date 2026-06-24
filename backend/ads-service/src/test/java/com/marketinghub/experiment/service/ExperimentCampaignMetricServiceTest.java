package com.marketinghub.experiment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.cost.CostAttributionService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testa a sincronização de métricas de campanha com o marco comercial real do funil.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentCampaignMetricServiceTest {

    @Mock
    private ExperimentCampaignMetricRepository repository;

    @Mock
    private FacebookAdsCampaignRepository campaignRepository;

    @Mock
    private CostAttributionService costAttributionService;

    @Mock
    private ExperimentRepository experimentRepository;

    @Mock
    private ExperimentFunnelEventRepository funnelEventRepository;

    @Mock
    private ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository;

    private ExperimentCampaignMetricService service;

    /** Monta o serviço real com repositórios controlados por mock. */
    @BeforeEach
    void setUp() {
        service = new ExperimentCampaignMetricService(
                repository,
                campaignRepository,
                costAttributionService,
                experimentRepository,
                funnelEventRepository,
                landingAnalyticsEventRepository);
    }

    /**
     * Garante que o primeiro recebimento de impressões remove eventos de teste antes de salvar a métrica real.
     */
    @Test
    void upsertResetsFunnelWhenImpressionsStart() {
        Experiment experiment = Experiment.builder().id(41L).build();
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("campaign-1");
        campaign.setExperiment(experiment);
        ExperimentCampaignMetric savedMetric = ExperimentCampaignMetric.builder().experiment(experiment).build();

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

        InOrder inOrder = inOrder(
                landingAnalyticsEventRepository,
                funnelEventRepository,
                experimentRepository,
                repository);
        inOrder.verify(landingAnalyticsEventRepository).deleteByExperimentId(41L);
        inOrder.verify(funnelEventRepository).deleteByExperimentIdAndSource(
                41L,
                ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE);
        inOrder.verify(funnelEventRepository).deleteByExperimentId(41L);
        inOrder.verify(experimentRepository).save(experiment);
        inOrder.verify(repository).save(any(ExperimentCampaignMetric.class));
        assertThat(experiment.getFunnelResetAt()).isNotNull();
    }

    /**
     * Garante que uma métrica que já tinha impressões não limpa novamente o funil em sincronizações posteriores.
     */
    @Test
    void upsertDoesNotResetFunnelWhenMetricAlreadyHadImpressions() {
        Experiment experiment = Experiment.builder()
                .id(41L)
                .funnelResetAt(Instant.parse("2026-06-24T00:00:00Z"))
                .build();
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("campaign-1");
        campaign.setExperiment(experiment);
        ExperimentCampaignMetric existingMetric = ExperimentCampaignMetric.builder()
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

        verify(landingAnalyticsEventRepository, never()).deleteByExperimentId(41L);
        verify(funnelEventRepository, never()).deleteByExperimentId(any());
        verify(experimentRepository, never()).save(experiment);
    }
}
