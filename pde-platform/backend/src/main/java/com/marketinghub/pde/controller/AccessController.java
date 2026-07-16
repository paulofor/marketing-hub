package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.AccessRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.PepperWebhookRequest;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.service.AccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe endpoints de liberação e consulta de acesso da área PDE. */
@RestController
@RequestMapping("/api/pde/access")
public class AccessController {

    private final AccessService accessService;

    /** Recebe o serviço que controla acessos liberados. */
    public AccessController(AccessService accessService) {
        this.accessService = accessService;
    }

    /** Cria um acesso de desenvolvimento para validar a experiência antes do checkout real. */
    @PostMapping("/dev")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse createDevAccess(@Valid @RequestBody AccessRequest request) {
        return accessService.createAccess(request.productSlug(), request.email(), "DEV");
    }

    /** Libera acesso da cliente a partir do e-mail informado no fluxo de compra. */
    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse createCheckoutAccess(@Valid @RequestBody AccessRequest request) {
        return accessService.createAccess(request.productSlug(), request.email(), "CHECKOUT");
    }

    /** Recebe webhook de compra aprovada pela Pepper e libera acesso ao produto. */
    @PostMapping("/pepper/webhook")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse receivePepperWebhook(@Valid @RequestBody PepperWebhookRequest request) {
        return accessService.createAccess(request.productSlug(), request.buyerEmail(), "PEPPER");
    }

    /** Retorna a área de trabalho da cliente autenticada por token de acesso. */
    @GetMapping("/{token}/workspace")
    public WorkspaceResponse getWorkspace(@PathVariable("token") String token) {
        return accessService.getWorkspace(token);
    }

    /** Marca uma missão como concluída para atualizar o progresso da experiência. */
    @PostMapping("/{token}/missions/{missionId}/complete")
    public WorkspaceResponse completeMission(
            @PathVariable("token") String token,
            @PathVariable("missionId") String missionId) {
        accessService.completeMission(token, missionId);
        return accessService.getWorkspace(token);
    }
}
