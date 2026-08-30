package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.dto.OperationalMissionCompletionRequest;
import com.marketinghub.pde.service.AccessService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: receber a conclusão autenticada dos marcos operacionais de um PDE assistido. */
@RestController
@RequestMapping("/api/internal/pde/assisted-operation/access")
@ConditionalOnProperty(name = "pde.assisted-operation.enabled", havingValue = "true")
public class AssistedOperationController {
    private final AccessService accessService;
    private final String operationToken;

    /** Recebe o serviço de progresso e o segredo exclusivo do módulo operador. */
    public AssistedOperationController(
            AccessService accessService,
            @Value("${pde.assisted-operation.token:}") String operationToken) {
        this.accessService = accessService;
        this.operationToken = operationToken;
    }

    /** Conclui a etapa operacional com os dois segredos fora do caminho HTTP. */
    @PostMapping("/missions/{missionId}/complete")
    public WorkspaceResponse completeOperationalMission(
            @PathVariable("missionId") String missionId,
            @RequestHeader(name = "X-PDE-Operation-Token", required = false) String informedToken,
            @RequestHeader(name = "X-PDE-Access-Token", required = false) String accessToken,
            @Valid @RequestBody(required = false) OperationalMissionCompletionRequest request) {
        if (operationToken.isBlank() || !operationToken.equals(informedToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operação PDE não autorizada");
        }
        accessService.completeOperationalMission(accessToken, missionId, request);
        return accessService.getWorkspace(accessToken);
    }
}
