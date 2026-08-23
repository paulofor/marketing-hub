package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.AccessRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe acesso pago simulado somente para homologações internas explicitamente habilitadas. */
@RestController
@RequestMapping("/api/internal/pde/test-access")
public class InternalAccessController {
    private final AccessService accessService;
    private final InternalApiAuthorizer internalApiAuthorizer;
    private final boolean internalQaEnabled;

    /** Recebe o acesso, a autorização interna e a trava de ambiente de homologação. */
    public InternalAccessController(
            AccessService accessService,
            InternalApiAuthorizer internalApiAuthorizer,
            @Value("${pde.access.internal-qa-enabled:false}") boolean internalQaEnabled) {
        this.accessService = accessService;
        this.internalApiAuthorizer = internalApiAuthorizer;
        this.internalQaEnabled = internalQaEnabled;
    }

    /** Cria acesso segregado sem registrar compra, venda ou receita humana. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse createInternalQaAccess(
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody AccessRequest request) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        if (!internalQaEnabled) {
            throw new SecurityException("Acesso de homologação PDE está desabilitado neste ambiente");
        }
        return accessService.createInternalQaAccess(
                request.productSlug(), request.email(), request.experienceVersion());
    }

    /** Expira um acesso segregado para validar a jornada pós-90 dias sem criar cobrança ou venda. */
    @PostMapping("/{token}/expire")
    public WorkspaceResponse expireInternalQaAccess(
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
            @PathVariable("token") String token) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        if (!internalQaEnabled) {
            throw new SecurityException("Acesso de homologação PDE está desabilitado neste ambiente");
        }
        return accessService.expireInternalQaAccess(token);
    }
}
