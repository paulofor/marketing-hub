package com.marketinghub.facebookads.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FacebookCampaignStopServiceTest {

  @Mock private FacebookAdsCampaignRepository campaignRepository;

  @InjectMocks private FacebookCampaignStopService service;

  private FacebookAdsCampaign campaign;

  @BeforeEach
  void setUp() {
    campaign = new FacebookAdsCampaign();
    campaign.setId("camp-1");
    campaign.setStatus(FacebookAdStatus.ACTIVE);
    campaign.setStopRequestedAt(Instant.now());
  }

  @Test
  void marksCampaignAsPausedWhenSuccess() {
    when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign));

    service.registerStopResult("camp-1", true, null);

    assertThat(campaign.getStatus()).isEqualTo(FacebookAdStatus.PAUSED);
    assertThat(campaign.getStopCompletedAt()).isNotNull();
    assertThat(campaign.getStopLastError()).isNull();
  }

  @Test
  void exposesPendingCampaignsToFacebookWorker() {
    when(campaignRepository.findPendingStopRequests()).thenReturn(List.of(campaign));

    assertThat(service.listPendingStopRequests()).containsExactly(campaign);
  }

  @Test
  void storesErrorWhenStopFails() {
    when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign));

    service.registerStopResult("camp-1", false, " Falha temporária ");

    assertThat(campaign.getStopCompletedAt()).isNull();
    assertThat(campaign.getStopLastError()).isEqualTo("Falha temporária");
  }

  @Test
  void throwsWhenStopNotRequested() {
    campaign.setStopRequestedAt(null);
    when(campaignRepository.findById("camp-1")).thenReturn(Optional.of(campaign));

    assertThrows(
        ResponseStatusException.class, () -> service.registerStopResult("camp-1", true, null));
  }
}
