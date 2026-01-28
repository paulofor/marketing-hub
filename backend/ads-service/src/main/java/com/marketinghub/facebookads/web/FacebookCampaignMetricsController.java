package com.marketinghub.facebookads.web;

import com.marketinghub.experiment.ExperimentStatus;
import com.marketinghub.facebookads.FacebookAdsCampaign;
import com.marketinghub.facebookads.FacebookAdsCampaignRepository;
import com.marketinghub.facebookads.dto.CampaignMetricRequest;
import com.marketinghub.facebookads.dto.ExperimentPerformanceDto;
import com.marketinghub.facebookads.dto.RunningCampaignDto;
import com.marketinghub.facebookads.service.FacebookCampaignMetricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/facebook-campaigns")
public class FacebookCampaignMetricsController {

    private final FacebookAdsCampaignRepository campaignRepository;
    private final FacebookCampaignMetricService metricService;

    public FacebookCampaignMetricsController(FacebookAdsCampaignRepository campaignRepository,
                                             FacebookCampaignMetricService metricService) {
        this.campaignRepository = campaignRepository;
        this.metricService = metricService;
    }

    @GetMapping("/running-campaigns")
    public List<RunningCampaignDto> runningCampaigns() {
        return campaignRepository.findByExperimentStatus(ExperimentStatus.RUNNING).stream()
                .map(campaign -> new RunningCampaignDto(
                        campaign.getId(),
                        campaign.getAdAccountId(),
                        campaign.getExperiment().getId(),
                        campaign.getExperiment().getName()
                ))
                .toList();
    }

    @PostMapping("/{campaignId}/metrics")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void recordMetrics(@PathVariable String campaignId,
                              @Valid @RequestBody CampaignMetricRequest request) {
        FacebookAdsCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Campaign not found: " + campaignId));
        if (!campaign.getExperiment().getId().equals(request.experimentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Experiment mismatch for campaign " + campaignId);
        }
        metricService.recordSnapshot(campaign, request);
    }

    @GetMapping("/performance")
    public List<ExperimentPerformanceDto> performance(@RequestParam(value = "experimentId", required = false) Long experimentId) {
        return metricService.latestPerformance(experimentId);
    }
}
