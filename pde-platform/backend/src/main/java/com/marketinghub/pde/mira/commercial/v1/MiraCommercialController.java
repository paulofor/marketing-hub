package com.marketinghub.pde.mira.commercial.v1;

import com.marketinghub.pde.dto.PrivacyActionRequest;
import com.marketinghub.pde.dto.PrivacyActionResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Responsabilidade: expor a entrega comercial autenticada de Mira v1. */
@RestController
@RequestMapping("/api/pde/mira/commercial/v1")
public class MiraCommercialController {
    private final MiraCommercialService service;

    /** Recebe o serviço único que governa a rotina comercial. */
    public MiraCommercialController(MiraCommercialService service) {
        this.service = service;
    }

    /** Retorna identidade e limites públicos da entrega. */
    @GetMapping("/contract")
    public MiraCommercialService.ContractResponse contract() {
        return service.contract();
    }

    /** Recupera ou inicia uma sessão paga sem bearer na URL. */
    @GetMapping("/session")
    public MiraCommercialService.SessionResponse session(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token) {
        return service.session(token);
    }

    /** Salva os rótulos informados para a próxima organização disponível. */
    @PutMapping("/input")
    public MiraCommercialService.SessionResponse saveInput(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token,
            @Valid @RequestBody MiraCommercialService.InputRequest request) {
        return service.saveInput(token, request);
    }

    /** Organiza a rotina sem usar IA ou criar indicação clínica. */
    @PostMapping("/generate")
    public MiraCommercialService.SessionResponse generate(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token) {
        return service.generate(token);
    }

    /** Registra uso real e conclusão da jornada paga. */
    @PostMapping("/events")
    public MiraCommercialService.SessionResponse event(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token,
            @Valid @RequestBody MiraCommercialService.EventRequest request) {
        return service.recordEvent(token, request);
    }

    /** Executa acesso, correção ou objeção sobre os dados da titular. */
    @PostMapping("/privacy")
    public PrivacyActionResponse privacy(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token,
            @Valid @RequestBody PrivacyActionRequest request) {
        return service.privacy(token, request);
    }

    /** Executa a exclusão total solicitada pela titular. */
    @DeleteMapping("/privacy")
    public PrivacyActionResponse delete(
            @RequestHeader(value = "X-PDE-Access-Token", required = false) String token) {
        return service.privacy(token, new PrivacyActionRequest("DELETION", null));
    }
}
