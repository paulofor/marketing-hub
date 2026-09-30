package com.marketinghub.pde.agentvalidation.v1;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor protótipos visuais estáticos somente à homologação autorizada. */
@RestController
@RequestMapping("/api/pde/agent-validation/v1/products/{productSlug}")
public class PdeStaticVisualAgentValidationController {
    private static final Logger log =
            LoggerFactory.getLogger(PdeStaticVisualAgentValidationController.class);
    private final PdeStaticVisualAgentValidationService service;
    private final InternalApiAuthorizer authorizer;

    /** Configura o motor compartilhado e a proteção das operações internas. */
    public PdeStaticVisualAgentValidationController(
            PdeStaticVisualAgentValidationService service, InternalApiAuthorizer authorizer) {
        this.service = service;
        this.authorizer = authorizer;
    }

    /** Expõe somente identidade, eventos e travas sanitizadas da superfície. */
    @GetMapping("/contract")
    public PdeStaticVisualAgentValidationService.ContractResponse contract(
            @PathVariable("productSlug") String productSlug) {
        return service.contract(productSlug);
    }

    /** Cria uma sessão segregada após validar a credencial interna do harness. */
    @PostMapping("/internal/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public PdeStaticVisualAgentValidationService.SessionResponse start(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody PdeStaticVisualAgentValidationService.AgentSessionRequest request) {
        authorizer.requireAuthorized(internalToken);
        log.info(
                "Payload bruto recebido para sessão visual multiagente; productSlug={} request={}",
                productSlug,
                request);
        return service.startAgentValidation(productSlug, request);
    }

    /** Recupera a evidência sintética sem devolver a credencial da sessão. */
    @GetMapping("/internal/evidence/{evidenceId}")
    public PdeStaticVisualAgentValidationService.AgentValidationEvidence evidence(
            @PathVariable("productSlug") String productSlug,
            @PathVariable("evidenceId") String evidenceId,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken) {
        authorizer.requireAuthorized(internalToken);
        return service.evidence(productSlug, evidenceId);
    }

    /** Expira uma sessão de teste para comprovar o caminho real de continuidade. */
    @PostMapping("/internal/session-expiration")
    public Map<String, Object> expireSession(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader(value = "X-PDE-Internal-Token", required = false) String internalToken,
            @Valid @RequestBody
                    PdeStaticVisualAgentValidationService.InternalSessionExpirationRequest request) {
        authorizer.requireAuthorized(internalToken);
        return service.expireSessionForHarness(productSlug, request);
    }

    /** Recupera o checkpoint autorizado pelo token opaco emitido pelo backend. */
    @GetMapping("/session")
    public PdeStaticVisualAgentValidationService.SessionResponse session(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader("X-PDE-Agent-Session") String sessionToken) {
        return service.session(productSlug, sessionToken);
    }

    /** Persiste a entrada bruta limitada antes de qualquer transformação visual. */
    @PutMapping("/input")
    public PdeStaticVisualAgentValidationService.SessionResponse input(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader("X-PDE-Agent-Session") String sessionToken,
            @Valid @RequestBody PdeStaticVisualAgentValidationService.InputRequest request) {
        log.info("Payload bruto recebido na entrada visual; productSlug={} request={}", productSlug, request);
        return service.saveInput(productSlug, sessionToken, request);
    }

    /** Materializa as três fixtures ou devolve o bloqueio determinístico do cenário. */
    @PostMapping("/generate")
    public PdeStaticVisualAgentValidationService.SessionResponse generate(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader("X-PDE-Agent-Session") String sessionToken) {
        return service.generate(productSlug, sessionToken);
    }

    /** Emite uma credencial rotativa depois do aceite da política interna versionada. */
    @PostMapping("/continuity")
    @ResponseStatus(HttpStatus.CREATED)
    public PdeStaticVisualAgentValidationService.ContinuityCredentialResponse continuity(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader("X-PDE-Agent-Session") String sessionToken,
            @Valid @RequestBody
                    PdeStaticVisualAgentValidationService.ContinuityCredentialRequest request) {
        return service.createContinuity(productSlug, sessionToken, request);
    }

    /** Troca a credencial de continuidade por nova sessão e nova credencial rotacionada. */
    @PostMapping("/continuity/resume")
    public PdeStaticVisualAgentValidationService.ContinuityResumeResponse resumeContinuity(
            @PathVariable("productSlug") String productSlug,
            @Valid @RequestBody PdeStaticVisualAgentValidationService.ContinuityResumeRequest request) {
        return service.resumeContinuity(productSlug, request);
    }

    /** Recupera o pacote apenas quando ele pertence à sessão autenticada. */
    @GetMapping("/packages/{packageId}")
    public PdeStaticVisualAgentValidationService.ResultPackageResponse resultPackage(
            @PathVariable("productSlug") String productSlug,
            @PathVariable("packageId") String packageId,
            @RequestHeader("X-PDE-Agent-Session") String sessionToken) {
        return service.resultPackage(productSlug, sessionToken, packageId);
    }

    /** Registra uma ação explícita do agente sem inferir valor, uso ou preferência. */
    @PostMapping("/events")
    public PdeStaticVisualAgentValidationService.SessionResponse event(
            @PathVariable("productSlug") String productSlug,
            @RequestHeader("X-PDE-Agent-Session") String sessionToken,
            @Valid @RequestBody PdeStaticVisualAgentValidationService.EventRequest request) {
        log.info("Payload bruto recebido no evento visual; productSlug={} request={}", productSlug, request);
        return service.event(productSlug, sessionToken, request);
    }

    /** Comprova que a superfície não possui publicação, cobrança, campanha ou provedor pago. */
    @GetMapping("/safety")
    public Map<String, Object> safety(@PathVariable("productSlug") String productSlug) {
        service.contract(productSlug);
        return Map.of(
                "published", false,
                "paymentEnabled", false,
                "campaignCreated", false,
                "mediaSpendBrl", 0,
                "providerCallsAuthorized", 0,
                "checkoutMode", "SIMULATED_NO_CHARGE");
    }
}
