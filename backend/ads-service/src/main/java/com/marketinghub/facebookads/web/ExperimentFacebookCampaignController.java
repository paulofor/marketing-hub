package com.marketinghub.facebookads.web;

import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignDto;
import com.marketinghub.facebookads.service.ExperimentFacebookCampaignService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/experiments/{experimentId}/facebook-campaigns")
public class ExperimentFacebookCampaignController {
    private final ExperimentFacebookCampaignService service;

    public ExperimentFacebookCampaignController(ExperimentFacebookCampaignService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExperimentFacebookCampaignDto> list(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId);
    }
}
