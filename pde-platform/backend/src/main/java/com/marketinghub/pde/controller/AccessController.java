package com.marketinghub.pde.controller;

import com.marketinghub.pde.dto.AccessRequest;
import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsJourneyResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsResetResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsSummaryResponse;
import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.FunnelEventResponse;
import com.marketinghub.pde.dto.DeliveryArtifactResponse;
import com.marketinghub.pde.dto.GoogleAccessRequest;
import com.marketinghub.pde.dto.MagicLinkResponse;
import com.marketinghub.pde.dto.MissionInteractionRequest;
import com.marketinghub.pde.dto.SupportRequest;
import com.marketinghub.pde.dto.SupportRequestResponse;
import com.marketinghub.pde.dto.PepperWebhookRequest;
import com.marketinghub.pde.dto.PepperSyncRequest;
import com.marketinghub.pde.dto.PepperSyncResponse;
import com.marketinghub.pde.dto.PepperRefundResponse;
import com.marketinghub.pde.dto.PrivacyActionRequest;
import com.marketinghub.pde.dto.PrivacyActionResponse;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import com.marketinghub.pde.service.PepperTransactionSyncService;
import com.marketinghub.pde.service.PepperRefundSyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    private final PepperRefundSyncService pepperRefundSyncService;
    private final InternalApiAuthorizer internalApiAuthorizer;

    /** Recebe os serviços que controlam acessos liberados e reconciliação Pepper. */
    public AccessController(
            AccessService accessService,
            PepperTransactionSyncService pepperTransactionSyncService,
            PepperRefundSyncService pepperRefundSyncService,
            InternalApiAuthorizer internalApiAuthorizer) {
        this.accessService = accessService;
        this.pepperTransactionSyncService = pepperTransactionSyncService;
        this.pepperRefundSyncService = pepperRefundSyncService;
        this.internalApiAuthorizer = internalApiAuthorizer;
    }

    /** Envia um link mágico para a cliente entrar sem senha na Área MUSA. */
    @PostMapping("/magic-link")
    public MagicLinkResponse requestMagicLink(@Valid @RequestBody AccessRequest request) {
        return accessService.requestMagicLink(
                request.productSlug(), request.email(), request.experienceVersion());
    }

    /** Envia um link mágico somente para cliente que já possui cadastro na Área MUSA. */
    @PostMapping("/login-link")
    public MagicLinkResponse requestLoginLink(@Valid @RequestBody AccessRequest request) {
        try {
            return accessService.requestExistingMagicLink(request.productSlug(), request.email());
        } catch (IllegalArgumentException ex) {
            if (!accessService.supportsPepperLoginReconciliation(request.productSlug())) {
                throw ex;
            }
            log.info(
                    "Magic link PDE sem acesso local, tentando reconciliacao Pepper; productSlug={}",
                    request.productSlug(),
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
        return accessService.recordPublicFunnelEvent(
                request.withRequestTrafficContext(resolveClientIp(httpRequest), httpRequest.getHeader("User-Agent")));
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
    public FunnelAnalyticsSummaryResponse summarizeFunnelAnalytics(
            @PathVariable("productSlug") String productSlug,
            @RequestParam(name = "includeNonHumanTraffic", defaultValue = "false") boolean includeNonHumanTraffic,
            @RequestParam(name = "experienceVersion", required = false) String experienceVersion,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken) {
        if (includeNonHumanTraffic) {
            internalApiAuthorizer.requireAuthorized(internalToken);
        }
        return accessService.summarizeFunnelAnalytics(productSlug, includeNonHumanTraffic, experienceVersion);
    }

    /** Retorna jornadas individuais por sessão para localizar abandono antes do primeiro acesso. */
    @GetMapping("/analytics/{productSlug}/journeys")
    public FunnelAnalyticsJourneyResponse summarizeSessionJourneys(
            @PathVariable("productSlug") String productSlug,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        return accessService.summarizeSessionJourneys(productSlug, limit);
    }

    /** Limpa métricas acumuladas antes da primeira impressão de campanha paga real. */
    @PostMapping("/analytics/{productSlug}/reset-campaign-start")
    public FunnelAnalyticsResetResponse resetFunnelAnalyticsForCampaignStart(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        return accessService.resetFunnelAnalyticsForCampaignStart(productSlug);
    }

    /** Recebe pagamento ou reembolso Pepper e aplica somente o estado confirmado no provedor. */
    @PostMapping("/pepper/webhook")
    @ResponseStatus(HttpStatus.CREATED)
    public Object receivePepperWebhook(@Valid @RequestBody PepperWebhookRequest request) {
        String transactionId = request.resolvedTransactionId();
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Webhook Pepper sem identificador de transação");
        }
        if ("refunded".equalsIgnoreCase(request.resolvedStatus())
                || "chargeback".equalsIgnoreCase(request.resolvedStatus())) {
            PepperRefundResponse refund = pepperRefundSyncService.reconcile(
                    request.resolvedProductSlug("metodo-musa-7-dias"), transactionId);
            return refund;
        }
        if (!"paid".equalsIgnoreCase(request.resolvedStatus())) {
            throw new IllegalArgumentException("Webhook Pepper sem estado financeiro final suportado");
        }
        PepperSyncResponse verified = pepperTransactionSyncService.syncPaidTransactions(
                request.resolvedProductSlug("metodo-musa-7-dias"), null, transactionId);
        if (verified.releasedAccesses() != 1 || verified.accesses().isEmpty()) {
            throw new IllegalArgumentException("Transação Pepper não comprovada para a oferta e o valor aprovados");
        }
        return verified.accesses().getFirst();
    }

    /** Reconcila compras pagas na Pepper para liberar acessos quando o webhook nao chegou. */
    @PostMapping("/pepper/sync")
    public PepperSyncResponse syncPepperTransactions(
            @RequestBody(required = false) PepperSyncRequest request,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken) {
        internalApiAuthorizer.requireAuthorized(internalToken);
        String productSlug = request == null ? null : request.productSlug();
        String search = request == null ? null : request.search();
        String transactionHash = request == null ? null : request.transactionHash();
        return pepperTransactionSyncService.syncPaidTransactions(productSlug, search, transactionHash);
    }

    /** Retorna a área de trabalho usando o bearer somente no header protegido. */
    @GetMapping("/workspace")
    public WorkspaceResponse getWorkspace(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token) {
        return accessService.getWorkspace(token);
    }

    /** Marca uma missão como concluída sem registrar o bearer no caminho HTTP. */
    @PostMapping("/missions/{missionId}/complete")
    public WorkspaceResponse completeMission(
            @PathVariable("missionId") String missionId,
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token) {
        accessService.completeMission(token, missionId);
        return accessService.getWorkspace(token);
    }

    /** Salva a personalização mantendo a credencial exclusivamente no header protegido. */
    @PostMapping("/missions/{missionId}/interactions")
    public WorkspaceResponse saveMissionInteraction(
            @PathVariable("missionId") String missionId,
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token,
            @Valid @RequestBody MissionInteractionRequest request) {
        accessService.saveMissionInteraction(token, missionId, request);
        return accessService.getWorkspace(token);
    }

    /** Baixa o conteúdo personalizado usando bearer fora da URL e marco concluído. */
    @GetMapping("/deliveries/{missionId}/download")
    public ResponseEntity<String> downloadDelivery(
            @PathVariable("missionId") String missionId,
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String accessToken) {
        DeliveryArtifactResponse artifact = accessService.getDeliveryArtifact(accessToken, missionId);
        String filename = missionId.replaceAll("[^a-zA-Z0-9-]", "-") + ".md";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
                .header("Content-Disposition", ContentDisposition.attachment().filename(filename).build().toString())
                .body(artifact.content());
    }

    /** Registra suporte sem inserir o bearer na URL observável pela infraestrutura. */
    @PostMapping("/support-requests")
    public SupportRequestResponse requestSupport(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token,
            @Valid @RequestBody SupportRequest request) {
        return accessService.requestSupport(token, request.message());
    }

    /** Executa direitos da titular com a credencial restrita ao header protegido. */
    @PostMapping("/privacy-requests")
    public PrivacyActionResponse requestPrivacyAction(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token,
            @Valid @RequestBody PrivacyActionRequest request) {
        return accessService.executePrivacyAction(token, request);
    }

    /** Autoriza o proxy a servir um material somente para acesso pago ainda vigente. */
    @GetMapping("/materials/authorize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void authorizeMaterialAccess(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String accessToken) {
        accessService.authorizeMaterialAccess(accessToken);
    }
}
