package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.PrivacyRetentionResponse;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe o comando interno de retenção sem assumir agendamento operacional no backend PDE. */
@RestController
@RequestMapping("/api/internal/pde/privacy")
public class InternalPrivacyController {
    private static final Logger log = LoggerFactory.getLogger(InternalPrivacyController.class);
    private final AccessService accessService;
    private final InternalApiAuthorizer internalApiAuthorizer;

    /** Recebe o serviço de dados e a autorização interna compartilhada. */
    public InternalPrivacyController(AccessService accessService, InternalApiAuthorizer internalApiAuthorizer) {
        this.accessService = accessService;
        this.internalApiAuthorizer = internalApiAuthorizer;
    }

    /** Executa a política de 180 dias quando acionada pelo executor operacional autorizado. */
    @PostMapping("/retention")
    public PrivacyRetentionResponse enforceRetention(
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        Instant executedAt = Instant.now();
        int anonymizedAccesses = accessService.enforceDataRetention(executedAt);
        log.info(
                "Retenção PDE concluída; correlationId={}, anonymizedAccesses={}, executedAt={}",
                correlationId,
                anonymizedAccesses,
                executedAt);
        return new PrivacyRetentionResponse(anonymizedAccesses, executedAt.toString());
    }
}
