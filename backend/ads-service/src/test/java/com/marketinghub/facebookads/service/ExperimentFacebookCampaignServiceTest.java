package com.marketinghub.facebookads.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.AdSet;
import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsAd;
import com.marketinghub.facebookads.FacebookAdsAdSet;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.facebookads.dto.ExperimentFacebookAdSetDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperimentFacebookCampaignServiceTest {
    @Mock
    private FacebookAdsCampaignRepository campaignRepository;

    private ExperimentFacebookCampaignService service;

    @BeforeEach
    void setUp() {
        service = new ExperimentFacebookCampaignService(campaignRepository);
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
}
