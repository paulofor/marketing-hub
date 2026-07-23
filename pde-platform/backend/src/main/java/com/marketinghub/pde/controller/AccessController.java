package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.AccessRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsJourneyResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsResetResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsSummaryResponse;
import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.FunnelEventResponse;
import com.marketinghub.pde.dto.GoogleAccessRequest;
import com.marketinghub.pde.dto.MagicLinkResponse;
import com.marketinghub.pde.dto.MissionInteractionRequest;
import com.marketinghub.pde.dto.PepperWebhookRequest;
import com.marketinghub.pde.dto.PepperSyncRequest;
import com.marketinghub.pde.dto.PepperSyncResponse;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.PepperTransactionSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expõe endpoints de liberação e consulta de acesso da área PDE. */
@RestController
@RequestMapping("/api/pde/access")
public class AccessController {
    private static final Logger log = LoggerFactory.getLogger(AccessController.class);

    private final AccessService accessService;
    private final PepperTransactionSyncService pepperTransactionSyncService;

    /** Recebe os serviços que controlam acessos liberados e reconciliação Pepper. */
    public AccessController(AccessService accessService, PepperTransactionSyncService pepperTransactionSyncService) {
        this.accessService = accessService;
        this.pepperTransactionSyncService = pepperTransactionSyncService;
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
        try {
            return accessService.loginCustomer(request.productSlug(), request.email());
        } catch (IllegalArgumentException ex) {
            log.info(
                    "Login PDE sem acesso local, tentando reconciliacao Pepper; productSlug={}, email={}",
                    request.productSlug(),
                    request.email(),
                    ex);
            pepperTransactionSyncService.syncPaidTransactions(request.productSlug(), request.email());
            return accessService.loginCustomer(request.productSlug(), request.email());
        }
    }

    /** Envia um link mágico para a cliente entrar sem senha na Área MUSA. */
    @PostMapping("/magic-link")
    public MagicLinkResponse requestMagicLink(@Valid @RequestBody AccessRequest request) {
        return accessService.requestMagicLink(request.productSlug(), request.email());
    }

    /** Envia um link mágico somente para cliente que já possui cadastro na Área MUSA. */
    @PostMapping("/login-link")
    public MagicLinkResponse requestLoginLink(@Valid @RequestBody AccessRequest request) {
        try {
            return accessService.requestExistingMagicLink(request.productSlug(), request.email());
        } catch (IllegalArgumentException ex) {
            log.info(
                    "Magic link PDE sem acesso local, tentando reconciliacao Pepper; productSlug={}, email={}",
                    request.productSlug(),
                    request.email(),
                    ex);
            pepperTransactionSyncService.syncPaidTransactions(request.productSlug(), request.email());
            return accessService.requestExistingMagicLink(request.productSlug(), request.email());
        }
    }

    /** Autentica a cliente pelo Google e cria ou reutiliza o acesso da Área MUSA. */
    @PostMapping("/google")
    public AccessResponse loginWithGoogle(@Valid @RequestBody GoogleAccessRequest request) {
        return accessService.loginWithGoogle(request.productSlug(), request.idToken());
    }

    /** Registra eventos comerciais da jornada MUSA/PDE para medir o funil de assinatura. */
    @PostMapping("/events")
    public FunnelEventResponse recordFunnelEvent(
            @Valid @RequestBody FunnelEventRequest request,
            HttpServletRequest httpRequest) {
        return accessService.recordFunnelEvent(request.withClientIp(resolveClientIp(httpRequest)));
    }

    /** Resolve o IP público mais provável preservado pelo proxy antes do backend PDE. */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    /** Retorna métricas consolidadas do funil e analytics do produto PDE. */
    @GetMapping("/analytics/{productSlug}/summary")
    public FunnelAnalyticsSummaryResponse summarizeFunnelAnalytics(@PathVariable("productSlug") String productSlug) {
        return accessService.summarizeFunnelAnalytics(productSlug);
    }

    /** Retorna jornadas individuais por sessão para localizar abandono antes do primeiro acesso. */
    @GetMapping("/analytics/{productSlug}/journeys")
    public FunnelAnalyticsJourneyResponse summarizeSessionJourneys(
            @PathVariable("productSlug") String productSlug,
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        return accessService.summarizeSessionJourneys(productSlug, limit);
    }

    /** Limpa métricas acumuladas antes da primeira impressão de campanha paga real. */
    @PostMapping("/analytics/{productSlug}/reset-campaign-start")
    public FunnelAnalyticsResetResponse resetFunnelAnalyticsForCampaignStart(
            @PathVariable("productSlug") String productSlug) {
        return accessService.resetFunnelAnalyticsForCampaignStart(productSlug);
    }

    /** Recebe webhook de compra aprovada pela Pepper e libera acesso ao produto. */
    @PostMapping("/pepper/webhook")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessResponse receivePepperWebhook(@Valid @RequestBody PepperWebhookRequest request) {
        return accessService.receivePepperWebhook(request);
    }

    /** Reconcila compras pagas na Pepper para liberar acessos quando o webhook nao chegou. */
    @PostMapping("/pepper/sync")
    public PepperSyncResponse syncPepperTransactions(@RequestBody(required = false) PepperSyncRequest request) {
        String productSlug = request == null ? null : request.productSlug();
        String search = request == null ? null : request.search();
        String transactionHash = request == null ? null : request.transactionHash();
        return pepperTransactionSyncService.syncPaidTransactions(productSlug, search, transactionHash);
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

    /** Salva respostas de personalização da cliente em uma missão da experiência. */
    @PostMapping("/{token}/missions/{missionId}/interactions")
    public WorkspaceResponse saveMissionInteraction(
            @PathVariable("token") String token,
            @PathVariable("missionId") String missionId,
            @Valid @RequestBody MissionInteractionRequest request) {
        accessService.saveMissionInteraction(token, missionId, request);
        return accessService.getWorkspace(token);
    }
}
