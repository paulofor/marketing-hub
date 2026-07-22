package com.marketinghub.experiment.web;

import com.marketinghub.experiment.monitoring.PostDeployMonitorService;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorResponseDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployRequestDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionDeployResponseDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o painel de monitoramento pós-deploy para experimentos do Marketing Hub. */
@RestController
@RequestMapping("/api/experiments/{experimentId}/post-deploy-monitor")
public class PostDeployMonitorController {

    private final PostDeployMonitorService service;

    /** Inicializa o controller com o serviço agregador de monitoramento. */
    public PostDeployMonitorController(PostDeployMonitorService service) {
        this.service = service;
    }

    /** Retorna a decisão consolidada cruzando Meta Ads, eventos PDE e logs. */
    @GetMapping
    public PostDeployMonitorResponseDto summarize(
            @PathVariable Long experimentId,
            @RequestParam(name = "productSlug", required = false) String productSlug) {
        return service.summarize(experimentId, productSlug);
    }

    /** Solicita o deploy produtivo do PDE quando a homologação está validada. */
    @PostMapping("/pde/production-deploy")
    public PostDeployPdeProductionDeployResponseDto requestProductionDeploy(
            @PathVariable Long experimentId,
            @RequestBody(required = false) PostDeployPdeProductionDeployRequestDto request) {
        return service.requestProductionDeploy(experimentId, request);
    }
}
