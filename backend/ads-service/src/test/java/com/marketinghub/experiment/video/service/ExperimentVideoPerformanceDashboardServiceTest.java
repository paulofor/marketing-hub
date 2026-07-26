package com.marketinghub.experiment.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentCampaignMetric;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.funnel.ExperimentFunnelStage;
import com.marketinghub.experiment.funnel.dto.ExperimentFunnelStageDto;
import com.marketinghub.experiment.video.ExperimentVideoAsset;
import com.marketinghub.experiment.video.ExperimentVideoReviewStatus;
import com.marketinghub.experiment.video.ExperimentVideoSlot;
import com.marketinghub.experiment.video.ExperimentVideoStatus;
import com.marketinghub.experiment.video.dto.ExperimentVideoPerformanceDashboardDto;
import com.marketinghub.facebookads.AdCreativeKind;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Valida o painel consolidado que cruza vídeos aprovados, Meta Ads e funil.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentVideoPerformanceDashboardServiceTest {
    @Mock
    private ExperimentRepository experimentRepository;
    @Mock
    private ExperimentVideoAssetRepository videoAssetRepository;
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;
    @Mock
    private FacebookAdsAdSetRepository adSetRepository;
    @Mock
    private ExperimentCampaignMetricRepository campaignMetricRepository;
    @Mock
    private ExperimentFunnelService funnelService;

    private ExperimentVideoPerformanceDashboardService service;

    /** Inicializa o serviço com fontes simuladas para isolar a agregação do painel. */
    @BeforeEach
    void setUp() {
        service = new ExperimentVideoPerformanceDashboardService(
                experimentRepository,
                videoAssetRepository,
                campaignRepository,
                adSetRepository,
                campaignMetricRepository,
                funnelService);
    }

    /** Garante que o painel consolida asset aprovado, criativo Meta e marcos do funil. */
    @Test
    void shouldSummarizeApprovedVideoWithMetaCreativeAndFunnelProgress() {
        Experiment experiment = Experiment.builder().id(71L).build();
        given(experimentRepository.findById(71L)).willReturn(Optional.of(experiment));

        ExperimentVideoAsset asset = ExperimentVideoAsset.builder()
                .id(7L)
                .experiment(experiment)
                .slot(ExperimentVideoSlot.AD)
                .provider("HEYGEN")
                .model("avatar")
                .status(ExperimentVideoStatus.READY)
                .reviewStatus(ExperimentVideoReviewStatus.APPROVED)
                .assetUrl("https://cdn.test/video-7.mp4")
                .build();
        given(videoAssetRepository.findByExperimentIdOrderByCreatedAtDesc(71L)).willReturn(List.of(asset));

        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("camp-71");
        campaign.setName("Campanha vídeo MUSA");
        campaign.setStatus(FacebookAdStatus.ACTIVE);
        campaign.setMetricsLastSyncedAt(Instant.parse("2026-07-26T12:00:00Z"));
        given(campaignRepository.findDetailedByExperimentId(71L)).willReturn(List.of(campaign));

        FacebookAdsAdCreative creative = new FacebookAdsAdCreative();
        creative.setId("creative-71");
        creative.setKind(AdCreativeKind.VIDEO);
        creative.setVideoDataJson("{\"video_id\":\"meta-video-1\",\"experimentVideoAssetId\":7}");

        FacebookAdsAd ad = new FacebookAdsAd();
        ad.setId("ad-71");
        ad.setName("Hook presença");
        ad.setStatus(FacebookAdStatus.ACTIVE);
        ad.setCreative(creative);

        FacebookAdsAdSet adSet = new FacebookAdsAdSet();
        adSet.setId("adset-71");
        adSet.setCampaign(campaign);
        adSet.setAds(new ArrayList<>(List.of(ad)));
        given(adSetRepository.findDetailedByCampaignIds(List.of("camp-71"))).willReturn(List.of(adSet));

        ExperimentCampaignMetric metric = ExperimentCampaignMetric.builder()
                .campaign(campaign)
                .experiment(experiment)
                .impressions(320L)
                .clicks(24L)
                .spend(new BigDecimal("7.50"))
                .build();
        given(campaignMetricRepository.findByCampaignIdIn(List.of("camp-71"))).willReturn(List.of(metric));
        given(funnelService.summarize(71L)).willReturn(List.of(
                stage(ExperimentFunnelStage.ENVIO_FORM, 4),
                stage(ExperimentFunnelStage.ACESSO_CHECKOUT, 2),
                stage(ExperimentFunnelStage.COMPRA, 1)));

        ExperimentVideoPerformanceDashboardDto dashboard = service.summarize(71L);

        assertThat(dashboard.summary().approvedAssets()).isEqualTo(1);
        assertThat(dashboard.summary().metaVideoCreatives()).isEqualTo(1);
        assertThat(dashboard.summary().impressions()).isEqualTo(320);
        assertThat(dashboard.summary().clicks()).isEqualTo(24);
        assertThat(dashboard.summary().diagnosticStarts()).isEqualTo(4);
        assertThat(dashboard.summary().checkoutAccesses()).isEqualTo(2);
        assertThat(dashboard.summary().purchases()).isEqualTo(1);
        assertThat(dashboard.summary().recommendation()).contains("compra registrada");
        assertThat(dashboard.assets()).hasSize(1);
        assertThat(dashboard.assets().get(0).attributionLevel()).isEqualTo("AD");
        assertThat(dashboard.assets().get(0).metaCreatives()).hasSize(1);
        assertThat(dashboard.campaigns()).hasSize(1);
    }

    /** Cria uma etapa de funil para o snapshot usado pelo painel. */
    private ExperimentFunnelStageDto stage(ExperimentFunnelStage stage, long total) {
        ExperimentFunnelStageDto dto = new ExperimentFunnelStageDto();
        dto.setStage(stage);
        dto.setTotalCount(total);
        return dto;
    }
}
