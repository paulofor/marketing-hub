package com.marketinghub.experiment.web;

import com.marketinghub.experiment.monitoring.PostDeployMonitorService;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorResponseDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotDto;
import com.marketinghub.experiment.monitoring.dto.PostDeployPdeProductionSlotRequestDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

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

    /** Lista os slots produtivos versionados do PDE para o produto monitorado. */
    @GetMapping("/pde/production-slots")
    public List<PostDeployPdeProductionSlotDto> listProductionSlots(
            @PathVariable Long experimentId,
            @RequestParam(name = "productSlug", required = false) String productSlug) {
        return service.listProductionSlots(experimentId, productSlug);
    }

    /** Cria ou atualiza um slot produtivo versionado do PDE pelo Marketing Hub. */
    @PostMapping("/pde/production-slots")
    public PostDeployPdeProductionSlotDto saveProductionSlot(
            @PathVariable Long experimentId,
            @Valid @RequestBody PostDeployPdeProductionSlotRequestDto request) {
        return service.saveProductionSlot(experimentId, request);
    }
}
