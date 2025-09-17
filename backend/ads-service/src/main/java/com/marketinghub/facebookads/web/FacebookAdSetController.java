package com.marketinghub.facebookads.web;

import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.facebookads.service.FacebookAdSetExperimentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints that expose experiment data ready to generate Facebook ad sets.
 */
@RestController
@RequestMapping("/api/facebook-adsets")
public class FacebookAdSetController {
    private final FacebookAdSetExperimentService experimentService;

    public FacebookAdSetController(FacebookAdSetExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @GetMapping("/experiments-ready")
    public List<ExperimentReadyForAdSetDto> experimentsReady() {
        return experimentService.listExperimentsReadyForAdSets();
    }
}

