package com.marketinghub.pde.mira.privatevalidation.v1;

import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a experiência privada e não comercial de Mira. */
@RestController
@RequestMapping("/api/pde/mira/private/v1")
public class MiraPrivatePrototypeController {
    private static final Logger log = LoggerFactory.getLogger(MiraPrivatePrototypeController.class);
    private final MiraPrivatePrototypeService service;

    /** Recebe o serviço que governa sessões, resultado e eventos privados. */
    public MiraPrivatePrototypeController(MiraPrivatePrototypeService service) {
        this.service = service;
    }

    /** Expõe o contrato sanitizado usado pela tela e pela homologação. */
    @GetMapping("/contract")
    public MiraPrivatePrototypeService.ContractResponse contract() {
        return service.contract();
    }

    /** Troca um acesso opaco por uma sessão retomável somente após consentimento. */
    @PostMapping("/access")
    @ResponseStatus(HttpStatus.CREATED)
    public MiraPrivatePrototypeService.SessionResponse access(
            @Valid @RequestBody MiraPrivatePrototypeService.AccessRequest request) {
        log.info("Payload bruto recebido no acesso privado de Mira; consentAccepted={}", request.consentAccepted());
        return service.access(request);
    }

    /** Recupera o checkpoint atual sem regenerar resultado ou evento. */
    @GetMapping("/session")
    public MiraPrivatePrototypeService.SessionResponse session(
            @RequestHeader("X-Mira-Session") String sessionToken) {
        return service.session(sessionToken);
    }

    /** Persiste a entrada mínima documentada da própria sessão. */
    @PutMapping("/input")
    public MiraPrivatePrototypeService.SessionResponse input(
            @RequestHeader("X-Mira-Session") String sessionToken,
            @Valid @RequestBody MiraPrivatePrototypeService.InputRequest request) {
        log.info("Payload bruto recebido na entrada privada de Mira; products={}", request.products().size());
        return service.saveInput(sessionToken, request);
    }

    /** Produz uma rotina segura ou um bloqueio explicável a partir do rótulo informado. */
    @PostMapping("/generate")
    public MiraPrivatePrototypeService.SessionResponse generate(
            @RequestHeader("X-Mira-Session") String sessionToken) {
        return service.generate(sessionToken);
    }

    /** Registra uso, preferência ou checkout simulado sem aceitar eventos arbitrários. */
    @PostMapping("/events")
    public MiraPrivatePrototypeService.SessionResponse event(
            @RequestHeader("X-Mira-Session") String sessionToken,
            @Valid @RequestBody MiraPrivatePrototypeService.EventRequest request) {
        log.info("Payload bruto recebido no evento privado de Mira; eventType={}", request.eventType());
        return service.event(sessionToken, request);
    }

    /** Comprova ausência de publicação, pagamento e mídia na superfície privada. */
    @GetMapping("/safety")
    public Map<String, Object> safety() {
        return Map.of("published", false, "paymentEnabled", false, "mediaSpendBrl", 0, "checkoutMode", "SIMULATED_NO_CHARGE");
    }
}
