package com.marketinghub.facebookads.controller;

import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignDto;
import com.marketinghub.facebookads.dto.ExperimentFacebookCampaignResetSummary;
import com.marketinghub.facebookads.service.ExperimentFacebookCampaignService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agrupa endpoints de consulta e reset de campanhas Facebook por experimento.
 */
@RestController
@RequestMapping("/api/experiments/{experimentId}/facebook-campaigns")
public class ExperimentFacebookCampaignController {
    private final ExperimentFacebookCampaignService service;

    // Executa a operação ExperimentFacebookCampaignController da integração Facebook Ads.
    public ExperimentFacebookCampaignController(ExperimentFacebookCampaignService service) {
        this.service = service;
    }

    @GetMapping
    // Executa a operação list da integração Facebook Ads.
    public List<ExperimentFacebookCampaignDto> list(@PathVariable Long experimentId) {
        return service.listByExperiment(experimentId);
    }

    @GetMapping("/reset-preview")
    // Executa a operação previewReset da integração Facebook Ads.
    public ExperimentFacebookCampaignResetSummary previewReset(@PathVariable Long experimentId) {
        return service.previewReset(experimentId);
    }

    @PostMapping("/reset")
    // Executa a operação reset da integração Facebook Ads.
    public ExperimentFacebookCampaignResetSummary reset(@PathVariable Long experimentId) {
        return service.reset(experimentId);
    }
}
