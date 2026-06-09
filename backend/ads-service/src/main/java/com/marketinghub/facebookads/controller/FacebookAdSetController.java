package com.marketinghub.facebookads.controller;

import com.marketinghub.facebookads.dto.ExperimentReadyForAdSetDto;
import com.marketinghub.facebookads.service.targetingPackage.FacebookAdSetTargetingPackageDto;
import com.marketinghub.facebookads.service.FacebookAdSetExperimentService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Lista os experimentos prontos para materializar conjuntos de anúncios, com filtro opcional por experimento.
     */
    @GetMapping("/experiments-ready")
    public List<ExperimentReadyForAdSetDto> experimentsReady(
            @RequestParam(name = "experimentId", required = false) Long experimentId) {
        return experimentService.listExperimentsReadyForAdSets(experimentId);
    }

    /**
     * Retorna o pacote de segmentação enxuto para publicação da campanha do experimento.
     */
    @GetMapping("/experiments/{experimentId}/targeting-package")
    public FacebookAdSetTargetingPackageDto targetingPackage(@PathVariable Long experimentId) {
        return experimentService.getTargetingPackageForCampaign(experimentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Targeting package not found for experiment " + experimentId));
    }
}

