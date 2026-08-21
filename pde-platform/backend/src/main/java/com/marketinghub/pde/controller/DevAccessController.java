package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.AccessRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.service.AccessService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: liberar acessos segregados somente em homologações explicitamente habilitadas. */
@RestController
@RequestMapping("/api/pde/access")
@ConditionalOnProperty(name = "pde.access.dev-enabled", havingValue = "true")
public class DevAccessController {
    private final AccessService accessService;

    /** Recebe o serviço canônico que persiste o acesso de homologação. */
    public DevAccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    /** Cria acesso DEV sem registrar compra ou venda e apenas quando o ambiente autoriza. */
    @PostMapping("/dev")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse createDevAccess(@Valid @RequestBody AccessRequest request) {
        return accessService.createAccess(request.productSlug(), request.email(), "DEV");
    }
}
