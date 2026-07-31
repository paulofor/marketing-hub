package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.BuildIdentityResponse;
import com.marketinghub.pde.dto.DeployStatusResponse;
import com.marketinghub.pde.service.DeployStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o status operacional do deploy PDE para auditoria no Marketing Hub. */
@RestController
public class DeployStatusController {

    private final DeployStatusService deployStatusService;

    /** Recebe o serviço que consolida o manifesto do ambiente publicado. */
    public DeployStatusController(DeployStatusService deployStatusService) {
        this.deployStatusService = deployStatusService;
    }

    /** Retorna commit, compose, portas, versão e serviços declarados no deploy atual. */
    @GetMapping("/api/pde/deploy/status")
    public DeployStatusResponse status() {
        return deployStatusService.currentStatus();
    }

    /** Retorna a identidade da build PDE para confirmar commit, imagem e backend usado. */
    @GetMapping("/api/pde/build-identity")
    public BuildIdentityResponse buildIdentity() {
        return deployStatusService.buildIdentity();
    }
}
