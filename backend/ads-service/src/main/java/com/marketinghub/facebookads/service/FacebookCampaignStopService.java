package com.marketinghub.facebookads.service;

import com.marketinghub.facebookads.FacebookAdStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.repository.jpa.facebookads.FacebookAdsCampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class FacebookCampaignStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookCampaignStopService.class);

    private final FacebookAdsCampaignRepository campaignRepository;

    public FacebookCampaignStopService(FacebookAdsCampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional(readOnly = true)
    public List<FacebookAdsCampaign> listPendingStopRequests() {
        return campaignRepository.findPendingStopRequests();
    }

    @Transactional
    public void registerStopResult(String campaignId, boolean success, String message) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Facebook campaign not found: " + campaignId));
        if (campaign.getStopRequestedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Stop was not requested for campaign " + campaignId);
        }
        if (!success) {
            campaign.setStopLastError(normalizeMessage(message));
            LOGGER.warn("Stop attempt for Facebook campaign {} failed: {}", campaignId, message);
            return;
        }
        campaign.setStatus(FacebookAdStatus.PAUSED);
        campaign.setStopCompletedAt(Instant.now());
        campaign.setStopLastError(null);
        LOGGER.info("Stop request completed for Facebook campaign {}", campaignId);
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
