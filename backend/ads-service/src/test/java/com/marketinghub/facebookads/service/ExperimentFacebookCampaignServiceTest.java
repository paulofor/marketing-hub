package com.marketinghub.facebookads.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.AdSet;
import com.marketinghub.experiment.funnel.ExperimentFunnelAttributionService;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdCreative;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdCreativeRepository;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdRepository;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsAdSetRepository;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.facebookads.dto.ExperimentFacebookAdSetDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignResetSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperimentFacebookCampaignServiceTest {
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;
    @Mock
    private FacebookAdsAdSetRepository adSetRepository;
    @Mock
    private FacebookAdsAdRepository adRepository;
    @Mock
    private FacebookAdsAdCreativeRepository adCreativeRepository;
    @Mock
    private ExperimentCampaignMetricRepository campaignMetricRepository;
    @Mock
    private ExperimentFunnelAttributionService funnelAttributionService;

    private ExperimentFacebookCampaignService service;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(funnelAttributionService.aggregateByCampaignCode(anyLong())).thenReturn(Map.of());
        service = new ExperimentFacebookCampaignService(
                campaignRepository,
                adSetRepository,
                adRepository,
                adCreativeRepository,
                campaignMetricRepository,
                funnelAttributionService);
    }

    @Test
    void mapsCampaignsAndHighlightsIssues() {
        FacebookAdsCampaign campaign = new FacebookAdsCampaign();
        campaign.setId("C1");
        campaign.setName("Camp 1");
        campaign.setObjective("OUTCOME_TRAFFIC");
        campaign.setStatus(FacebookAdStatus.PAUSED);
        campaign.setCreatedAt(Instant.parse("2026-02-18T20:00:00Z"));

        AdSet experimentAdSet = new AdSet();
        experimentAdSet.setId(5L);

        FacebookAdsAdSet linkedAdSet = new FacebookAdsAdSet();
        linkedAdSet.setId("AS1");
        linkedAdSet.setName("Segmento principal");
        linkedAdSet.setStatus(FacebookAdStatus.PAUSED);
        linkedAdSet.setCreatedAt(Instant.parse("2026-02-18T20:05:00Z"));
        linkedAdSet.setExperimentAdSet(experimentAdSet);

        FacebookAdsAd ad = new FacebookAdsAd();
        ad.setId("AD1");
        ad.setName("Anúncio 1");
        ad.setStatus(FacebookAdStatus.PAUSED);
        ad.setCreatedAt(Instant.parse("2026-02-18T20:06:00Z"));
        ad.setAdSet(linkedAdSet);

        linkedAdSet.setAds(new ArrayList<>(List.of(ad)));

        FacebookAdsAdSet orphanAdSet = new FacebookAdsAdSet();
        orphanAdSet.setId("AS2");
        orphanAdSet.setName("Segmento sem vínculo");
        orphanAdSet.setStatus(FacebookAdStatus.ACTIVE);
        orphanAdSet.setCreatedAt(Instant.parse("2026-02-18T20:10:00Z"));
        orphanAdSet.setAds(new ArrayList<>());

        campaign.setAdSets(new ArrayList<>(List.of(linkedAdSet, orphanAdSet)));

        when(campaignRepository.findDetailedByExperimentId(1L)).thenReturn(List.of(campaign));
        linkedAdSet.setCampaign(campaign);
        orphanAdSet.setCampaign(campaign);
        when(adSetRepository.findDetailedByCampaignIds(List.of("C1")))
                .thenReturn(List.of(linkedAdSet, orphanAdSet));

        List<ExperimentFacebookCampaignDto> dtos = service.listByExperiment(1L);
        assertEquals(1, dtos.size());
        ExperimentFacebookCampaignDto dto = dtos.get(0);
        assertEquals("C1", dto.id());
        assertTrue(dto.issues().isEmpty(), "Campaign should not report issues when ad sets exist");
        assertEquals(2, dto.adSets().size());

        ExperimentFacebookAdSetDto linkedDto = dto.adSets().get(0);
        assertTrue(linkedDto.issues().isEmpty());
        assertEquals(5L, linkedDto.experimentAdSetId());

        ExperimentFacebookAdSetDto orphanDto = dto.adSets().get(1);
        assertEquals("AS2", orphanDto.id());
        assertFalse(orphanDto.issues().isEmpty());
        assertTrue(orphanDto.issues().stream().anyMatch(msg -> msg.contains("segmentação")));
        assertTrue(orphanDto.issues().stream().anyMatch(msg -> msg.contains("Nenhum anúncio")));
    }

    @Test
    void returnsEmptyListWhenExperimentIdIsNull() {
        List<ExperimentFacebookCampaignDto> dtos = service.listByExperiment(null);
        assertTrue(dtos.isEmpty());
    }

    @Test
    void previewResetCountsPendingArtifacts() {
        FacebookAdsCampaign pending = new FacebookAdsCampaign();
        pending.setId("C10");
        when(campaignRepository.findByExperimentId(10L)).thenReturn(List.of(pending));

        FacebookAdsAdSet adSet = new FacebookAdsAdSet();
        adSet.setId("AS10");
        when(adSetRepository.findByCampaignIdIn(List.of("C10"))).thenReturn(List.of(adSet));

        FacebookAdsAdCreative creative = new FacebookAdsAdCreative();
        creative.setId("CR10");
        FacebookAdsAd ad = new FacebookAdsAd();
        ad.setId("AD10");
        ad.setAdSet(adSet);
        ad.setCreative(creative);
        when(adRepository.findByAdSetIdIn(List.of("AS10"))).thenReturn(List.of(ad));

        ExperimentFacebookCampaignResetSummary summary = service.previewReset(10L);
        assertEquals(1, summary.campaigns());
        assertEquals(1, summary.adSets());
        assertEquals(1, summary.ads());
        assertEquals(1, summary.creatives());
    }

    @Test
    void resetRemovesPendingArtifacts() {
        FacebookAdsCampaign pending = new FacebookAdsCampaign();
        pending.setId("C42");
        when(campaignRepository.findByExperimentId(42L)).thenReturn(List.of(pending));

        FacebookAdsAdSet adSet = new FacebookAdsAdSet();
        adSet.setId("AS42");
        when(adSetRepository.findByCampaignIdIn(List.of("C42"))).thenReturn(List.of(adSet));

        FacebookAdsAdCreative creative = new FacebookAdsAdCreative();
        creative.setId("CR42");
        FacebookAdsAd ad = new FacebookAdsAd();
        ad.setId("AD42");
        ad.setAdSet(adSet);
        ad.setCreative(creative);
        when(adRepository.findByAdSetIdIn(List.of("AS42"))).thenReturn(List.of(ad));

        ExperimentFacebookCampaignResetSummary summary = service.reset(42L);
        assertEquals(1, summary.campaigns());
        verify(campaignMetricRepository).deleteByCampaignIds(List.of("C42"));
        verify(campaignRepository).deleteAllInBatch(List.of(pending));
        verify(adCreativeRepository).deleteAllByIdInBatch(List.of("CR42"));
    }

    @Test
    void resetDoesNothingWhenNoPendingCampaignExists() {
        FacebookAdsCampaign published = new FacebookAdsCampaign();
        published.setId("C50");
        published.setExternalId("META-1");
        when(campaignRepository.findByExperimentId(50L)).thenReturn(List.of(published));

        ExperimentFacebookCampaignResetSummary summary = service.reset(50L);
        assertEquals(0, summary.campaigns());
        verify(campaignMetricRepository, never()).deleteByCampaignIds(List.of("C50"));
        verify(campaignRepository, never()).deleteAllInBatch(List.of(published));
        verifyNoInteractions(adSetRepository, adRepository, adCreativeRepository);
    }
}
