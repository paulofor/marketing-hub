package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.AccessRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.FunnelEventResponse;
import com.marketinghub.pde.dto.GoogleAccessRequest;
import com.marketinghub.pde.dto.MagicLinkResponse;
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

    /** Cadastra uma nova cliente na Área MUSA ou reutiliza o acesso já existente para o e-mail. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse registerCustomer(@Valid @RequestBody AccessRequest request) {
        return accessService.registerCustomer(request.productSlug(), request.email());
    }

    /** Autentica uma cliente já cadastrada pelo e-mail do produto. */
    @PostMapping("/login")
    public AccessResponse loginCustomer(@Valid @RequestBody AccessRequest request) {
        return accessService.loginCustomer(request.productSlug(), request.email());
    }

    /** Envia um link magico para a cliente entrar sem senha na Área MUSA. */
    @PostMapping("/magic-link")
    public MagicLinkResponse requestMagicLink(@Valid @RequestBody AccessRequest request) {
        return accessService.requestMagicLink(request.productSlug(), request.email());
    }

    /** Autentica a cliente pelo Google e cria ou reutiliza o acesso da Área MUSA. */
    @PostMapping("/google")
    public AccessResponse loginWithGoogle(@Valid @RequestBody GoogleAccessRequest request) {
        return accessService.loginWithGoogle(request.productSlug(), request.idToken());
    }

    /** Registra eventos comerciais da jornada MUSA/PDE para medir o funil de assinatura. */
    @PostMapping("/events")
    public FunnelEventResponse recordFunnelEvent(@Valid @RequestBody FunnelEventRequest request) {
        return accessService.recordFunnelEvent(request);
    }

    /** Recebe webhook de compra aprovada pela Pepper e libera acesso ao produto. */
    @PostMapping("/pepper/webhook")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse receivePepperWebhook(@Valid @RequestBody PepperWebhookRequest request) {
        return accessService.receivePepperWebhook(request);
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
