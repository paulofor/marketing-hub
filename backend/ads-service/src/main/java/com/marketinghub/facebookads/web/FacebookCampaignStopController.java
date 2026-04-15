package com.marketinghub.facebookads.web;

import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.dto.FacebookCampaignStopRequestDto;
import com.marketinghub.facebookads.dto.FacebookCampaignStopResultRequest;
import com.marketinghub.facebookads.service.FacebookCampaignStopService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/facebook-campaigns")
public class FacebookCampaignStopController {

    private final FacebookCampaignStopService stopService;

    public FacebookCampaignStopController(FacebookCampaignStopService stopService) {
        this.stopService = stopService;
    }

    @GetMapping("/stop-requests")
    public List<FacebookCampaignStopRequestDto> listStopRequests() {
        return stopService.listPendingStopRequests().stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping("/{campaignId}/stop-results")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerStopResult(@PathVariable String campaignId,
                                   @RequestBody FacebookCampaignStopResultRequest request) {
        boolean success = request != null && request.success();
        String message = request != null ? request.message() : null;
        stopService.registerStopResult(campaignId, success, message);
    }

    private FacebookCampaignStopRequestDto toDto(FacebookAdsCampaign campaign) {
        return new FacebookCampaignStopRequestDto(
                campaign.getId(),
                campaign.getExternalId(),
                campaign.getAdAccountId(),
                campaign.getExperiment() != null ? campaign.getExperiment().getId() : null,
                campaign.getStopReason(),
                campaign.getStopRequestedAt(),
                campaign.getStopLastError()
        );
    }
}
