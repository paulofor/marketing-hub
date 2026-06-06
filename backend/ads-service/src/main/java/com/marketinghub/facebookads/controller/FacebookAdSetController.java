package com.marketinghub.facebookads.controller;

import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.facebookads.service.FacebookAdSetExperimentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints that expose experiment data ready to generate Facebook ad sets.
 */
/**
 * Agrupa endpoints de conjuntos de anúncios Facebook Ads prontos para publicação.
 */
@RestController
@RequestMapping("/api/facebook-adsets")
public class FacebookAdSetController {
    private final FacebookAdSetExperimentService experimentService;

    // Executa a operação FacebookAdSetController da integração Facebook Ads.
    public FacebookAdSetController(FacebookAdSetExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @GetMapping("/experiments-ready")
    // Executa a operação experimentsReady da integração Facebook Ads.
    public List<ExperimentReadyForAdSetDto> experimentsReady() {
        return experimentService.listExperimentsReadyForAdSets();
    }
}

