package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.dto.FlowResponse;
import com.marketinghub.leadportal.service.FlowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe operações internas e idempotentes de manutenção dos fluxos persistidos. */
@RestController
@RequestMapping("/api/internal/flows")
public class FlowMaintenanceController {

    private final FlowService flowService;

    /** Inicializa a manutenção interna com o serviço canônico de fluxos. */
    public FlowMaintenanceController(FlowService flowService) {
        this.flowService = flowService;
    }

    /** Reprocessa os ativos do fluxo para garantir derivados adequados à entrega web. */
    @PostMapping("/{slug}/optimize-assets")
    public ResponseEntity<FlowResponse> optimizeAssets(
            @PathVariable("slug") String slug, HttpServletRequest request) {
        if (!isLoopback(request.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(FlowResponse.from(flowService.optimizeExistingAssets(slug)));
    }

    /** Reconhece somente os endereços de loopback usados pelo comando interno do deploy. */
    private boolean isLoopback(String remoteAddress) {
        return "127.0.0.1".equals(remoteAddress)
                || "0:0:0:0:0:0:0:1".equals(remoteAddress)
                || "::1".equals(remoteAddress);
    }
}
